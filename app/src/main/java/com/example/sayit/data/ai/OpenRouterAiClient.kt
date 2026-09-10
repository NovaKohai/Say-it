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

data class OpenRouterRequest(
    val apiKey: String,
    val prompt: String,
    val systemContext: String,
    val history: List<ChatTurn> = emptyList(),
    val model: String = OpenRouterAiClient.DEFAULT_MODEL
)

class OpenRouterAiClient {

    companion object {
        const val DEFAULT_MODEL = "inclusionai/ling-3.0-flash-fin:free"
        const val FALLBACK_MODEL_1 = "poolside/laguna-s-2.1:free"
        const val FALLBACK_MODEL_2 = "nvidia/nemotron-3-ultra-550b-a55b:free"

        /**
         * Real-time tested and verified active free models on OpenRouter.
         * Filtered to exclude congested/rate-limited models (like Gemma 429).
         * Prioritized by Arabic conversational fluency, financial reasoning, and response speed.
         */
        val FREE_FALLBACK_MODELS = listOf(
            "inclusionai/ling-3.0-flash-fin:free",
            "poolside/laguna-s-2.1:free",
            "nvidia/nemotron-3-ultra-550b-a55b:free",
            "poolside/laguna-xs-2.1:free",
            "nvidia/nemotron-3.5-lightning:free",
            "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free",
            "nvidia/nemotron-3-super-120b-a12b:free",
            "nex-agi/nex-n2.5-pro:free",
            "nex-agi/nex-n2.5-mini:free",
            "liquid/lfm-2.5-2.6b:free",
            "google/gemma-4-26b-a4b-it:free"
        )

        private const val OPENROUTER_COMPLETIONS_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val OPENROUTER_AUTH_URL = "https://openrouter.ai/api/v1/auth/key"
        private const val CONNECT_TIMEOUT_MS = 6000
        private const val READ_TIMEOUT_MS = 14000

        fun cleanModelResponse(raw: String): String {
            var text = raw.trim()
            if (text.isBlank() || text.equals("null", ignoreCase = true)) return ""

            // 1. Strip think/thought tags
            text = text
                .replace(Regex("(?s)<think>.*?</think>"), "")
                .replace(Regex("(?s)<thought>.*?</thought>"), "")
                .replace(Regex("(?s)Here's a thinking process:.*?(\n\n|$)"), "")
                .trim()

            // 2. If response contains Arabic, strip any leading English reasoning blocks
            val firstArabicIdx = text.indexOfFirst { it in '\u0600'..'\u06FF' }
            if (firstArabicIdx > 0) {
                val prefix = text.substring(0, firstArabicIdx).trim()
                val isOnlyLatinAndPunctuation = prefix.all { c ->
                    c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c.isWhitespace() ||
                    c in ".,!?:;'\"()-_/\\[]{}<>*#~`"
                }
                if (isOnlyLatinAndPunctuation) {
                    text = text.substring(firstArabicIdx).trim()
                    // Strip trailing quotes or trailing English CoT fragments if any
                    text = text.replace(Regex("""["']+\s*[A-Za-z\s.,!?]*$"""), "").trim()
                }
            }

            return if (text.isNotBlank() && !text.equals("null", ignoreCase = true)) text else ""
        }

        fun optCleanString(obj: JSONObject?, key: String): String {
            if (obj == null || obj.isNull(key)) return ""
            val str = obj.optString(key, "").trim()
            return if (str.equals("null", ignoreCase = true)) "" else str
        }
    }

    suspend fun generateResponse(request: OpenRouterRequest): String = withContext(Dispatchers.IO) {
        val trimmedKey = request.apiKey.trim()
        if (trimmedKey.isBlank()) {
            throw AppError.AiAuthError(
                message = "يرجى إدخال مفتاح API صالح لتفعيل الشات بوت.",
                messageEn = "Please configure a valid API key to activate the chatbot."
            )
        }

        // Build priority cascade: primary requested model first, then all working free models
        val modelsToTry = buildList {
            add(request.model)
            addAll(FREE_FALLBACK_MODELS.filter { it != request.model })
        }

        var lastException: Exception? = null

        for ((index, modelCandidate) in modelsToTry.withIndex()) {
            try {
                android.util.Log.d("SayItAI", "Attempting model (${index + 1}/${modelsToTry.size}): $modelCandidate")
                val response = executeChatCompletion(request.copy(apiKey = trimmedKey, model = modelCandidate))
                if (response.isNotBlank()) {
                    if (index > 0) {
                        android.util.Log.i("SayItAI", "Fallback succeeded using model: $modelCandidate")
                    }
                    return@withContext response
                }
            } catch (e: AppError.AiAuthError) {
                // Auth error won't be fixed by changing models
                throw e
            } catch (e: AppError.NetworkUnavailable) {
                // Network unavailable won't be fixed by changing models
                throw e
            } catch (e: AppError.AiRateLimit) {
                if (e.cause?.message?.startsWith("ACCOUNT_DAILY_LIMIT") == true) {
                    throw e
                }
                lastException = e
                android.util.Log.w("SayItAI", "Model $modelCandidate rate limited, trying next fallback...")
            } catch (e: Exception) {
                lastException = e
                android.util.Log.w("SayItAI", "Model $modelCandidate failed (${e.message}), trying next free fallback...")
            }
        }

        throw (lastException ?: AppError.Unknown(
            message = "جميع نماذج الذكاء الاصطناعي المجانية مشغولة حالياً، يرجى المحاولة بعد قليل.",
            cause = Exception("All free fallback models exhausted")
        ))
    }

    suspend fun generateResponse(
        apiKey: String,
        prompt: String,
        systemContext: String,
        history: List<ChatTurn> = emptyList(),
        model: String = DEFAULT_MODEL
    ): String = generateResponse(OpenRouterRequest(apiKey, prompt, systemContext, history, model))

    suspend fun testApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) return@withContext false

        var connection: HttpURLConnection? = null
        try {
            val url = URL(OPENROUTER_AUTH_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Authorization", "Bearer $trimmed")
                setRequestProperty("HTTP-Referer", "https://sayit.app")
                setRequestProperty("X-Title", "Say It Financial Copilot")
            }
            val code = connection.responseCode
            return@withContext (code == 200)
        } catch (e: java.io.IOException) {
            android.util.Log.w("SayItAI", "testApiKey network failed: ${e.message}")
            return@withContext false
        } finally {
            connection?.disconnect()
        }
    }

    private fun executeChatCompletion(request: OpenRouterRequest): String {
        val (apiKey, prompt, systemContext, history, modelName) = request
        var connection: HttpURLConnection? = null
        try {
            val url = URL(OPENROUTER_COMPLETIONS_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("HTTP-Referer", "https://sayit.app")
                setRequestProperty("X-Title", "Say It Financial Copilot")
                setRequestProperty("User-Agent", "SayIt-Android-Fintech/1.0")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val messagesArray = JSONArray()

            // 1. System Prompt
            if (systemContext.isNotBlank()) {
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemContext)
                })
            }

            // 2. History turns
            for (turn in history.takeLast(10)) {
                if (turn.text.isNotBlank()) {
                    val openAiRole = when (turn.role.lowercase()) {
                        "user" -> "user"
                        "assistant", "model" -> "assistant"
                        "system" -> "system"
                        else -> "user"
                    }
                    messagesArray.put(JSONObject().apply {
                        put("role", openAiRole)
                        put("content", turn.text)
                    })
                }
            }

            // 3. Current User Prompt
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })

            val jsonBody = JSONObject().apply {
                put("model", modelName)
                put("messages", messagesArray)
                put("temperature", 0.6)
                put("max_tokens", 800)
            }

            android.util.Log.d("SayItAI", "Calling OpenRouter model $modelName with ${messagesArray.length()} messages...")
            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            android.util.Log.d("SayItAI", "OpenRouter model $modelName returned status: $statusCode")
            if (statusCode == 200) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { reader ->
                    reader.readText()
                }
                android.util.Log.d("SayItAI", "Response received: ${responseText.take(150)}")
                val jsonResponse = JSONObject(responseText.trim())
                if (jsonResponse.has("error")) {
                    val errMsg = jsonResponse.optJSONObject("error")?.optString("message", "Upstream error") ?: "Upstream error"
                    throw AppError.Unknown(
                        message = "Upstream model error: $errMsg",
                        cause = Exception(errMsg)
                    )
                }

                val choices = jsonResponse.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.optJSONObject("message")
                    var textContent = optCleanString(message, "content")
                    if (textContent.isBlank()) {
                        textContent = optCleanString(message, "reasoning")
                    }
                    if (textContent.isBlank()) {
                        textContent = optCleanString(firstChoice, "text")
                    }
                    val cleaned = cleanModelResponse(textContent)
                    if (cleaned.isNotBlank()) {
                        return cleaned
                    }
                }
                throw AppError.Unknown("Empty or null choices from model $modelName")
            } else {
                when (statusCode) {
                    401, 403 -> throw AppError.AiAuthError()
                    429 -> {
                        val errorStream = connection.errorStream
                        val errBody = if (errorStream != null) {
                            BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                        } else ""
                        val isAccountLimit = errBody.contains("free-models-per-day") || errBody.contains("openrouter_free_tier_daily")
                        throw AppError.AiRateLimit(
                            message = if (isAccountLimit) "الحصة اليومية للخدمة السحابية اكتملت مؤقتاً." else "تعذر الاتصال حالياً.",
                            cause = Exception(if (isAccountLimit) "ACCOUNT_DAILY_LIMIT: $errBody" else "HTTP 429: $errBody")
                        )
                    }
                    else -> {
                        val errorStream = connection.errorStream
                        val errBody = if (errorStream != null) {
                            BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                        } else ""
                        throw AppError.Unknown(
                            message = "تعذر استلام الرد حالياً (رمز: $statusCode)، يرجى المحاولة لاحقاً.",
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
}
