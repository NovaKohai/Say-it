package com.example.sayit.ai

import com.example.sayit.domain.model.AiAction
import com.example.sayit.domain.model.AiEngineType
import com.example.sayit.domain.model.AiMessage
import com.example.sayit.domain.model.AppError
import com.example.sayit.domain.model.MessageSender
import com.example.sayit.domain.model.NavigationTarget
import com.example.sayit.domain.repository.AiCopilotRepository
import com.example.sayit.domain.usecase.AskAiCopilotUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCopilotTest {

    private class FakeAiCopilotRepository : AiCopilotRepository {
        var lastPrompt: String? = null

        override suspend fun getInitialGreeting(isArabic: Boolean): AiMessage {
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = if (isArabic) "أهلاً بك يا بطل!" else "Welcome!",
                actions = listOf(
                    AiAction.ShowBudgetSnapshot(
                        monthlyBudget = 15000.0,
                        totalSpent = 3500.0,
                        remaining = 11500.0,
                        burnRate = 350.0,
                        depletionDate = "End of Month"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST
            )
        }

        private var apiKey: String = ""

        override fun getGeminiApiKey(): String = apiKey
        override fun saveGeminiApiKey(key: String) { apiKey = key }
        override suspend fun testGeminiApiKey(key: String): Boolean = key.isNotBlank()

        override suspend fun queryCopilot(
            userPrompt: String,
            isArabic: Boolean,
            conversationHistory: List<AiMessage>
        ): AiMessage {
            lastPrompt = userPrompt
            val lower = userPrompt.lowercase()
            return when {
                lower.contains("sms") || lower.contains("استيراد") -> {
                    AiMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "مزامنة الرسائل",
                        actions = listOf(
                            AiAction.Navigate(
                                target = NavigationTarget.SMS_IMPORT,
                                labelAr = "مزامنة SMS",
                                labelEn = "Sync SMS"
                            )
                        ),
                        sourceEngine = AiEngineType.LOCAL_FAST
                    )
                }
                lower.contains("أمان") || lower.contains("امان") || lower.contains("safe") -> {
                    AiMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "نعم يا طارق، ميزانيتك في أمان تام ومستقرة جداً حتى الآن.\nأنفقت 850 ج.م فقط من أصل 15,000 ج.م.",
                        actions = listOf(
                            AiAction.ShowBudgetSnapshot(
                                monthlyBudget = 15000.0,
                                totalSpent = 850.0,
                                remaining = 14150.0,
                                burnRate = 94.0,
                                depletionDate = "نهاية الشهر"
                            )
                        ),
                        sourceEngine = AiEngineType.LOCAL_FAST
                    )
                }
                lower.contains("قسط") || lower.contains("أقساط") || lower.contains("installment") -> {
                    AiMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "معندكش أي أقساط أو التزامات مالية مسجلة للشهر ده يا طارق، كله في السليم وتمام التمام! 👏",
                        actions = listOf(
                            AiAction.Navigate(
                                target = NavigationTarget.INSTALLMENTS,
                                labelAr = "عرض جدول الأقساط",
                                labelEn = "View Installments"
                            )
                        ),
                        sourceEngine = AiEngineType.LOCAL_FAST
                    )
                }
                lower.contains("ميزانية") || lower.contains("budget") -> {
                    AiMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "الميزانية متبقي فيها 11,500 ج.م",
                        actions = listOf(
                            AiAction.ShowBudgetSnapshot(
                                monthlyBudget = 15000.0,
                                totalSpent = 3500.0,
                                remaining = 11500.0,
                                burnRate = 350.0,
                                depletionDate = "End of Month"
                            )
                        ),
                        sourceEngine = AiEngineType.LOCAL_FAST
                    )
                }
                lower.contains("لو اشتريت") || lower.contains("what if") -> {
                    AiMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "محاكاة: المتبقي بعد الشراء سيكون 6,500 ج.م والميزانية آمنة.",
                        sourceEngine = AiEngineType.LOCAL_FAST
                    )
                }
                else -> {
                    AiMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "رد سحابي من Gemini 1.5 Flash",
                        sourceEngine = AiEngineType.GEMINI_CLOUD
                    )
                }
            }
        }
    }

    @Test
    fun testInitialGreetingFlow() = runBlocking {
        val repo = FakeAiCopilotRepository()
        val useCase = AskAiCopilotUseCase(repo)

        val greeting = useCase.getInitialGreeting(isArabic = true)
        assertNotNull(greeting)
        assertTrue(greeting.text.contains("أهلاً بك"))
        assertEquals(AiEngineType.LOCAL_FAST, greeting.sourceEngine)
        assertTrue(greeting.actions.any { it is AiAction.ShowBudgetSnapshot })
    }

    @Test
    fun testEmptyPromptReturnsGreeting() = runBlocking {
        val repo = FakeAiCopilotRepository()
        val useCase = AskAiCopilotUseCase(repo)

        val result = useCase("   ", isArabic = true)
        assertNotNull(result)
        assertTrue(result.text.contains("أهلاً بك"))
    }

    @Test
    fun testSmsNavigationIntentRouting() = runBlocking {
        val repo = FakeAiCopilotRepository()
        val useCase = AskAiCopilotUseCase(repo)

        val response = useCase("ازاي استورد رسايل ال sms؟", isArabic = true)
        assertNotNull(response)
        assertEquals("ازاي استورد رسايل ال sms؟", repo.lastPrompt)
        val nav = response.actions.firstOrNull { it is AiAction.Navigate } as? AiAction.Navigate
        assertNotNull(nav)
        assertEquals(NavigationTarget.SMS_IMPORT, nav!!.target)
    }

    @Test
    fun testBudgetSnapshotIntentRouting() = runBlocking {
        val repo = FakeAiCopilotRepository()
        val useCase = AskAiCopilotUseCase(repo)

        val response = useCase("متبقي كام من الميزانية؟", isArabic = true)
        assertNotNull(response)
        assertTrue(response.actions.any { it is AiAction.ShowBudgetSnapshot })
    }

    @Test
    fun testWhatIfSimulationIntent() = runBlocking {
        val repo = FakeAiCopilotRepository()
        val useCase = AskAiCopilotUseCase(repo)

        val response = useCase("لو اشتريت موبايل بـ 5000", isArabic = true)
        assertNotNull(response)
        assertTrue(response.text.contains("محاكاة"))
        assertEquals(AiEngineType.LOCAL_FAST, response.sourceEngine)
    }

    @Test
    fun testCloudEngineRoutingForComplexPrompt() = runBlocking {
        val repo = FakeAiCopilotRepository()
        val useCase = AskAiCopilotUseCase(repo)

        val response = useCase("عايز خطة مالية استثمارية لشراء شقة في 3 سنين", isArabic = true)
        assertNotNull(response)
        assertEquals(AiEngineType.GEMINI_CLOUD, response.sourceEngine)
    }

    @Test
    fun testAppErrorHierarchy() {
        val networkErr = AppError.NetworkUnavailable()
        val authErr = AppError.AiAuthError()
        val timeoutErr = AppError.AiTimeout()
        val rateLimitErr = AppError.AiRateLimit()
        val dbErr = AppError.DatabaseError()

        assertTrue(networkErr is Exception)
        assertTrue(authErr is Exception)
        assertTrue(timeoutErr is Exception)
        assertTrue(rateLimitErr is Exception)
        assertTrue(dbErr is Exception)

        assertNotNull(networkErr.message)
        assertNotNull(networkErr.messageEn)
    }

    @Test
    fun testPersonaSelectionAction() {
        val actionTarek = AiAction.SelectPersona(name = "طارق", label = "طارق")
        val actionAhmed = AiAction.SelectPersona(name = "أحمد", label = "أحمد")

        assertEquals("طارق", actionTarek.name)
        assertEquals("طارق", actionTarek.label)
        assertEquals("أحمد", actionAhmed.name)
        assertEquals("أحمد", actionAhmed.label)
    }

    @Test
    fun testErrorHandlingContainsNoDeveloperTerms() {
        val errors = listOf(
            AppError.NetworkUnavailable(),
            AppError.AiRateLimit(),
            AppError.AiAuthError(),
            AppError.AiTimeout(),
            AppError.DatabaseError(),
            AppError.Unknown()
        )

        val forbiddenTechnicalTerms = listOf("API", "KEY", "HTTP", "JSON", "TOKEN", "EXCEPTION", "NULLPOINTER")

        for (err in errors) {
            val arUpper = err.message.uppercase()
            val enUpper = (when (err) {
                is AppError.NetworkUnavailable -> err.messageEn
                is AppError.AiRateLimit -> err.messageEn
                is AppError.AiAuthError -> err.messageEn
                is AppError.AiTimeout -> err.messageEn
                is AppError.DatabaseError -> err.messageEn
                is AppError.Unknown -> err.messageEn
                else -> ""
            }).uppercase()

            for (term in forbiddenTechnicalTerms) {
                assertTrue("Arabic error message '${err.message}' should not leak '$term'", !arUpper.contains(term))
                assertTrue("English error message '$enUpper' should not leak '$term'", !enUpper.contains(term))
            }
        }
    }

    @Test
    fun testBudgetSafetyQueryReturnsRealAnalysis() = runBlocking {
        val repo = FakeAiCopilotRepository()
        val useCase = AskAiCopilotUseCase(repo)

        val response = useCase("ميزانيتي في أمان؟", isArabic = true)
        assertNotNull(response)
        assertTrue("Response should directly confirm safety status", response.text.contains("أمان"))
        assertTrue("Response should mention numbers and context", response.text.contains("15,000") || response.text.contains("850"))
        assertNotNull(response.actions)
        assertTrue(response.actions.any { it is AiAction.ShowBudgetSnapshot })
    }

    @Test
    fun testApiKeySaveAndRetrieval() = runBlocking {
        val repo = FakeAiCopilotRepository()
        val useCase = AskAiCopilotUseCase(repo)

        assertEquals("", useCase.getGeminiApiKey())
        useCase.saveGeminiApiKey("AIzaSyFakeKey123")
        assertEquals("AIzaSyFakeKey123", useCase.getGeminiApiKey())
        assertTrue(useCase.testGeminiApiKey("AIzaSyFakeKey123"))
    }

    @Test
    fun testConversationHistoryPassing() = runBlocking {
        val repo = FakeAiCopilotRepository()
        val useCase = AskAiCopilotUseCase(repo)

        val history = listOf(
            AiMessage(sender = MessageSender.USER, text = "صرفت 500 ج.م"),
            AiMessage(sender = MessageSender.ASSISTANT, text = "تم تسجيل 500 ج.م")
        )

        val response = useCase("طب ولو صرفت 200 كمان؟", isArabic = true, conversationHistory = history)
        assertNotNull(response)
        assertEquals("طب ولو صرفت 200 كمان؟", repo.lastPrompt)
    }

    @Test
    fun testOpenApiKeySetupAction() {
        val action = AiAction.OpenApiKeySetup()
        assertEquals("تفعيل مفتاح الذكاء الاصطناعي (Gemini)", action.labelAr)
        assertEquals("Configure Gemini API Key", action.labelEn)
    }

    @Test
    fun testSanitizedGeminiContentsPayload() {
        val rawHistory = listOf(
            com.example.sayit.data.ai.ChatTurn("model", "Welcome greeting"),
            com.example.sayit.data.ai.ChatTurn("user", "Hello"),
            com.example.sayit.data.ai.ChatTurn("user", "Another user message"),
            com.example.sayit.data.ai.ChatTurn("model", "Hello there!"),
            com.example.sayit.data.ai.ChatTurn("user", "Pending question")
        )

        val turns = com.example.sayit.data.ai.GeminiAiClient.buildSanitizedTurns(rawHistory, "What is my budget?")
        assertNotNull(turns)

        var expected = "user"
        for (i in turns.indices) {
            val role = turns[i].role
            assertEquals("Index $i should have role $expected", expected, role)
            expected = if (expected == "user") "model" else "user"
        }

        val lastTurn = turns.last()
        assertEquals("user", lastTurn.role)
        assertEquals("What is my budget?", lastTurn.text)
    }

    @Test
    fun testOpenRouterCleanModelResponseStripsThinkingTags() {
        val rawWithThink = "<think>\nThinking about finances and calculating numbers...\n</think>\nميزانيتك تمام ومتبقي 8000 ج.م."
        val cleaned = com.example.sayit.data.ai.OpenRouterAiClient.cleanModelResponse(rawWithThink)
        assertEquals("ميزانيتك تمام ومتبقي 8000 ج.م.", cleaned)

        val rawWithThought = "<thought>Some deep reasoning</thought>أهلاً بك يا بطل!"
        val cleanedThought = com.example.sayit.data.ai.OpenRouterAiClient.cleanModelResponse(rawWithThought)
        assertEquals("أهلاً بك يا بطل!", cleanedThought)
    }

    @Test
    fun testDefaultOpenRouterKeyAndModel() {
        assertEquals(
            com.example.sayit.BuildConfig.GEMINI_API_KEY,
            com.example.sayit.data.local.SayItPreferences.DEFAULT_API_KEY
        )
        assertEquals(
            "inclusionai/ling-3.0-flash-fin:free",
            com.example.sayit.data.local.SayItPreferences.DEFAULT_AI_MODEL
        )
        assertTrue(com.example.sayit.data.ai.OpenRouterAiClient.FREE_FALLBACK_MODELS.isNotEmpty())
        assertEquals("inclusionai/ling-3.0-flash-fin:free", com.example.sayit.data.ai.OpenRouterAiClient.DEFAULT_MODEL)
        assertTrue(com.example.sayit.data.ai.OpenRouterAiClient.FREE_FALLBACK_MODELS.contains("inclusionai/ling-3.0-flash-fin:free"))
        assertTrue(com.example.sayit.data.ai.OpenRouterAiClient.FREE_FALLBACK_MODELS.contains("poolside/laguna-s-2.1:free"))
        assertTrue(com.example.sayit.data.ai.OpenRouterAiClient.FREE_FALLBACK_MODELS.contains("nvidia/nemotron-3-ultra-550b-a55b:free"))
        assertTrue(com.example.sayit.data.ai.OpenRouterAiClient.FREE_FALLBACK_MODELS.contains("nex-agi/nex-n2.5-pro:free"))
    }

    @Test
    fun testNullResponseCleaned() {
        val cleanedNull = com.example.sayit.data.ai.OpenRouterAiClient.cleanModelResponse("null")
        assertEquals("", cleanedNull)

        val cleanedNullCaps = com.example.sayit.data.ai.OpenRouterAiClient.cleanModelResponse("NULL")
        assertEquals("", cleanedNullCaps)

        val cleanedNullSpace = com.example.sayit.data.ai.OpenRouterAiClient.cleanModelResponse("   null   ")
        assertEquals("", cleanedNullSpace)
    }

    @Test
    fun testEnglishPreambleStrippedFromArabicResponse() {
        val raw = "We need answer in Egyptian colloquial only, no MSA words. Need assess budget: spent 1000.\n\"إيه يا بطل، الحسبة تمام: صرفت 1000 من 15000، فاضلك 14000.\""
        val cleaned = com.example.sayit.data.ai.OpenRouterAiClient.cleanModelResponse(raw)
        assertEquals("إيه يا بطل، الحسبة تمام: صرفت 1000 من 15000، فاضلك 14000.", cleaned)
    }

    @Test
    fun testInstallmentsQuery() = runBlocking {
        val repo = FakeAiCopilotRepository()
        val useCase = AskAiCopilotUseCase(repo)
        val response = useCase("عليا أقساط إيه الشهر ده؟", isArabic = true)

        assertNotNull(response)
        assertTrue(response.text.contains("أقساط"))
        assertEquals(AiEngineType.LOCAL_FAST, response.sourceEngine)
        val navAction = response.actions.filterIsInstance<AiAction.Navigate>().firstOrNull()
        assertNotNull(navAction)
        assertEquals(NavigationTarget.INSTALLMENTS, navAction?.target)
    }
}
