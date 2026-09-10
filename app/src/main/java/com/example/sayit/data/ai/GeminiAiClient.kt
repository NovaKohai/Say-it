package com.example.sayit.data.ai

import com.example.sayit.domain.model.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

data class ChatTurn(
    val role: String, // "user" or "model"
    val text: String
)

class GeminiAiClient {

    companion object {
        private const val MODEL_PRIMARY = "gemini-2.0-flash"
        private const val MODEL_FALLBACK = "gemini-1.5-flash"
        private const val BASE_URL_PREFIX = "https://generativelanguage.googleapis.com/v1beta/models/"
        private const val CONNECT_TIMEOUT_MS = 8000
        private const val READ_TIMEOUT_MS = 15000

        fun buildSanitizedTurns(history: List<ChatTurn>, prompt: String): List<ChatTurn> {
            val alternatingTurns = mutableListOf<ChatTurn>()
            var expectedRole = "user"

            for (turn in history.takeLast(10)) {
                if (turn.text.isNotBlank() && turn.role == expectedRole) {
                    alternatingTurns.add(turn)
                    expectedRole = if (expectedRole == "user") "model" else "user"
                }
            }

            if (alternatingTurns.isNotEmpty() && alternatingTurns.last().role == "user") {
                alternatingTurns.removeAt(alternatingTurns.size - 1)
            }

            alternatingTurns.add(ChatTurn("user", prompt))
            return alternatingTurns
        }

        fun buildSanitizedContents(history: List<ChatTurn>, prompt: String): JSONArray {
            val contentsArray = JSONArray()
            val turns = buildSanitizedTurns(history, prompt)

            for (turn in turns) {
                val turnObj = JSONObject().apply {
                    put("role", turn.role)
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", turn.text) })
                    }
                    put("parts", parts)
                }
                contentsArray.put(turnObj)
            }

            return contentsArray
        }
    }

    suspend fun generateResponse(
        apiKey: String,
        prompt: String,
        systemContext: String,
        history: List<ChatTurn> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw AppError.AiAuthError(
                message = "يرجى إدخال مفتاح Gemini API لتفعيل المحادثة الذكية.",
                messageEn = "Please configure your Gemini API key to activate smart conversation."
            )
        }

        // Try primary 2.0-flash first
        try {
            return@withContext executeModelCall(MODEL_PRIMARY, apiKey, prompt, systemContext, history)
        } catch (e: AppError.AiAuthError) {
            throw e
        } catch (e: AppError.AiRateLimit) {
            throw e
        } catch (e: AppError.NetworkUnavailable) {
            throw e
        } catch (_: Exception) {
            // Fallback to gemini-1.5-flash
            return@withContext executeModelCall(MODEL_FALLBACK, apiKey, prompt, systemContext, history)
        }
    }

    suspend fun testApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) return@withContext false

        var connection: HttpURLConnection? = null
        try {
            val urlString = "https://generativelanguage.googleapis.com/v1beta/models?key=$trimmed"
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            val code = connection.responseCode
            return@withContext (code == 200)
        } catch (_: Exception) {
            return@withContext false
        } finally {
            connection?.disconnect()
        }
    }

    private fun executeModelCall(
        modelName: String,
        apiKey: String,
        prompt: String,
        systemContext: String,
        history: List<ChatTurn>
    ): String {
        val urlString = "$BASE_URL_PREFIX$modelName:generateContent?key=$apiKey"
        var connection: HttpURLConnection? = null

        try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val jsonBody = JSONObject().apply {
                // 1. System Instruction
                if (systemContext.isNotBlank()) {
                    val sysObj = JSONObject().apply {
                        val partsArr = JSONArray().apply {
                            put(JSONObject().apply { put("text", systemContext) })
                        }
                        put("parts", partsArr)
                    }
                    put("system_instruction", sysObj)
                }

                // 2. Multi-turn Contents with Strict Gemini Role Alternation
                val contentsArray = buildSanitizedContents(history, prompt)
                put("contents", contentsArray)

                // 3. Generation Config
                val genConfig = JSONObject().apply {
                    put("temperature", 0.65)
                    put("maxOutputTokens", 1200)
                }
                put("generationConfig", genConfig)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            if (statusCode == 200) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { reader ->
                    reader.readText()
                }

                val jsonResponse = JSONObject(responseText)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "").trim()
                    }
                }
                throw AppError.Unknown("Empty candidates from Gemini")
            } else {
                when (statusCode) {
                    400 -> {
                        // Fallback without system_instruction if format issue on older model
                        val errStream = connection.errorStream
                        val err = if (errStream != null) BufferedReader(InputStreamReader(errStream, "UTF-8")).use { it.readText() } else ""
                        if (err.contains("system_instruction") || err.contains("INVALID_ARGUMENT")) {
                            return executeFallbackSingleTurn(modelName, apiKey, prompt, systemContext)
                        }
                        throw AppError.Unknown(message = "تعذر إكمال الطلب، يرجى إعادة المحاولة.", cause = Exception(err))
                    }
                    401, 403 -> throw AppError.AiAuthError()
                    429 -> throw AppError.AiRateLimit()
                    else -> {
                        val errorStream = connection.errorStream
                        val errBody = if (errorStream != null) {
                            BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                        } else ""
                        throw AppError.Unknown(
                            message = "تعذر استلام الرد حالياً، يرجى المحاولة لاحقاً.",
                            cause = Exception("HTTP $statusCode: $errBody")
                        )
                    }
                }
            }
        } catch (e: UnknownHostException) {
            throw AppError.NetworkUnavailable(cause = e)
        } catch (e: SocketTimeoutException) {
            throw AppError.AiTimeout(cause = e)
        } catch (e: AppError) {
            throw e
        } catch (e: Exception) {
            throw AppError.Unknown(message = "تعذر إكمال الاتصال حالياً، يرجى المحاولة لاحقاً.", cause = e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun executeFallbackSingleTurn(
        modelName: String,
        apiKey: String,
        prompt: String,
        systemContext: String
    ): String {
        val urlString = "$BASE_URL_PREFIX$modelName:generateContent?key=$apiKey"
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }

        try {
            val fullText = "$systemContext\n\nUser Question:\n$prompt"
            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", fullText) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            if (connection.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
                val jsonResponse = JSONObject(responseText)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "").trim()
                    }
                }
            }
            throw AppError.Unknown("Empty candidates from Gemini fallback")
        } finally {
            connection.disconnect()
        }
    }
}
