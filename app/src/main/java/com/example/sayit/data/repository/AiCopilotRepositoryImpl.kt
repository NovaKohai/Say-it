package com.example.sayit.data.repository

import com.example.sayit.data.ai.ChatTurn
import com.example.sayit.data.ai.GeminiAiClient
import com.example.sayit.data.ai.OpenRouterAiClient
import com.example.sayit.data.ai.LocalFinancialSummary
import com.example.sayit.data.ai.LocalToolsExecutor
import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.data.local.SayItPreferences
import com.example.sayit.domain.model.ActionStatus
import com.example.sayit.domain.model.AiAction
import com.example.sayit.domain.model.AiEngineType
import com.example.sayit.domain.model.AiMessage
import com.example.sayit.domain.model.AppError
import com.example.sayit.domain.model.Category
import com.example.sayit.domain.model.MessageSender
import com.example.sayit.domain.model.NavigationTarget
import com.example.sayit.domain.model.TransactionType
import com.example.sayit.domain.repository.AiCopilotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

class AiCopilotRepositoryImpl(
    private val localTools: LocalToolsExecutor,
    private val geminiClient: GeminiAiClient,
    private val preferences: SayItPreferences,
    private val database: SayItDatabase? = null,
    private val openRouterClient: OpenRouterAiClient = OpenRouterAiClient()
) : AiCopilotRepository {

    override fun getGeminiApiKey(): String = preferences.geminiApiKey

    override fun saveGeminiApiKey(key: String) {
        preferences.geminiApiKey = key.trim()
    }

    override suspend fun testGeminiApiKey(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.startsWith("sk-") || trimmed.contains("-or-")) {
            return openRouterClient.testApiKey(trimmed)
        }
        val orResult = openRouterClient.testApiKey(trimmed)
        if (orResult) return true
        return geminiClient.testApiKey(trimmed)
    }

    override suspend fun getInitialGreeting(isArabic: Boolean): AiMessage = withContext(Dispatchers.IO) {
        val userName = preferences.userPreferredName
        val summary = localTools.getFinancialSummary()
        val hasKey = preferences.geminiApiKey.isNotBlank()

        if (userName.isBlank()) {
            val text = if (isArabic) {
                "يا هلا بيك في Say It! منور يا غالي 👋\nتحب أكلمك باسم طارق ولا أحمد؟"
            } else {
                "Welcome to Say It! 👋\nWould you like me to address you as Tarek or Ahmed?"
            }
            AiMessage(
                sender = MessageSender.ASSISTANT,
                text = text,
                actions = listOf(
                    AiAction.SelectPersona(name = "طارق", label = "طارق"),
                    AiAction.SelectPersona(name = "أحمد", label = "أحمد")
                ),
                sourceEngine = AiEngineType.LOCAL_FAST
            )
        } else {
            val text = if (isArabic) {
                if (hasKey) {
                    "منور يا $userName يا بطل! 👋\nأنا صاحبك ومستشارك المالي هنا في Say It. دردش معايا براحتك عن مصاريفك، ميزانيتك، أو أي حسبة بتفكر فيها وهحسبهالك بالمصري الصرف وعلى مية بيضا.\n\nتحب نبدأ بإيه النهاردة؟"
                } else {
                    "منور يا $userName! 👋\nأنا جاهز أساعدك تظبط مصاريفك وميزانيتك وتعرف فلوسك رايحة فين."
                }
            } else {
                if (hasKey) {
                    "Welcome $userName to Say It! 👋\nI am your smart financial companion. I can chat freely, analyze your spending, and provide personalized budgeting advice.\n\nWhat would you like to explore today?"
                } else {
                    "Welcome $userName to Say It! 👋\nI am here to help manage your budget and guide your finances."
                }
            }

            val actions = mutableListOf<AiAction>()
            actions.add(
                AiAction.ShowBudgetSnapshot(
                    monthlyBudget = summary.monthlyBudget,
                    totalSpent = summary.totalExpenseMonth,
                    remaining = summary.remainingBudget,
                    burnRate = summary.dailyBurn,
                    depletionDate = if (isArabic) "نهاية الشهر" else "End of Month"
                )
            )

            if (!hasKey) {
                actions.add(
                    AiAction.OpenApiKeySetup(
                        labelAr = "تفعيل مفتاح الذكاء الاصطناعي (Gemini)",
                        labelEn = "Configure Gemini API Key"
                    )
                )
            }

            AiMessage(
                sender = MessageSender.ASSISTANT,
                text = text,
                actions = actions,
                sourceEngine = if (hasKey) AiEngineType.GEMINI_CLOUD else AiEngineType.LOCAL_FAST
            )
        }
    }

    override suspend fun queryCopilot(
        userPrompt: String,
        isArabic: Boolean,
        conversationHistory: List<AiMessage>
    ): AiMessage = withContext(Dispatchers.IO) {
        val userName = preferences.userPreferredName.ifBlank { if (isArabic) "يا بطل" else "my friend" }
        val apiKey = preferences.geminiApiKey.trim()

        // 1. If API key is missing, prompt user to configure real AI key
        if (apiKey.isBlank()) {
            val noticeText = if (isArabic) {
                "منور يا $userName! 👋\n\nأنا شات بوت ذكاء اصطناعي حقيقي. عشان أقدر أدردش معاك وأحسبلك كل مليم، محتاجين نفعل مفتاح الـ API."
            } else {
                "Hello $userName! 👋\n\nI am your AI financial companion. To chat freely and compute your numbers, please activate your API key."
            }

            return@withContext AiMessage(
                sender = MessageSender.ASSISTANT,
                text = noticeText,
                actions = listOf(
                    AiAction.OpenApiKeySetup(
                        labelAr = "تفعيل مفتاح الذكاء الاصطناعي",
                        labelEn = "Activate AI Key"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST
            )
        }

        // 2. Build Rich Real-Time Financial Context
        val summary = localTools.getFinancialSummary()
        val topMerchantsStr = summary.topMerchants.take(5).joinToString(", ") { "${it.merchantName}: ${it.totalAmount.toInt()} EGP" }

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = cal.timeInMillis

        val categoryList = database?.let { getCategoryBreakdown(it, startOfMonth) } ?: emptyList()
        val installmentList = database?.let { getActiveInstallments(it) } ?: emptyList()
        val recentTxList = database?.let { getRecentTransactions(it) } ?: emptyList()

        val isEnglishInput = !isArabic || userPrompt.matches(Regex("^[a-zA-Z0-9\\s?,.!'-]+$"))

        val systemContext = com.example.sayit.data.ai.AiSystemPrompt.buildSystemPrompt(
            userName = userName,
            isEnglishInput = isEnglishInput,
            summary = summary,
            categoryList = categoryList,
            topMerchantsStr = topMerchantsStr,
            installmentList = installmentList,
            recentTxList = recentTxList
        )

        // 3. Format Multi-Turn Chat History
        val historyTurns = conversationHistory
            .filter { it.text.isNotBlank() && !it.actions.any { a -> a is AiAction.SelectPersona } }
            .takeLast(10)
            .map { msg ->
                ChatTurn(
                    role = if (msg.sender == MessageSender.USER) "user" else "model",
                    text = msg.text
                )
            }

        // 4. Connect to Cloud AI (OpenRouter with Nemotron / Gemini fallback)
        return@withContext try {
            val rawResponse = if (apiKey.startsWith("sk-") || apiKey.contains("-or-")) {
                openRouterClient.generateResponse(
                    apiKey = apiKey,
                    prompt = userPrompt,
                    systemContext = systemContext,
                    history = historyTurns,
                    model = preferences.aiModel
                )
            } else {
                geminiClient.generateResponse(
                    apiKey = apiKey,
                    prompt = userPrompt,
                    systemContext = systemContext,
                    history = historyTurns
                )
            }

            // Parse any action tags in response
            val (cleanedText, parsedActions) = extractActionsFromResponse(rawResponse, isArabic)
            val finalText = cleanedText.trim()
            if (finalText.isBlank() || finalText.equals("null", ignoreCase = true)) {
                throw AppError.Unknown("Empty or null response received from model")
            }

            AiMessage(
                sender = MessageSender.ASSISTANT,
                text = finalText,
                actions = parsedActions,
                sourceEngine = AiEngineType.GEMINI_CLOUD
            )
        } catch (err: AppError) {
            generateIntelligentLocalResponse(
                userPrompt = userPrompt,
                isArabic = isArabic,
                summary = summary,
                categoryList = categoryList,
                installmentList = installmentList,
                recentTxList = recentTxList,
                startOfMonth = startOfMonth,
                err = err
            )
        } catch (e: Exception) {
            generateIntelligentLocalResponse(
                userPrompt = userPrompt,
                isArabic = isArabic,
                summary = summary,
                categoryList = categoryList,
                installmentList = installmentList,
                recentTxList = recentTxList,
                startOfMonth = startOfMonth,
                err = AppError.Unknown(message = e.message ?: "Unknown", cause = e)
            )
        }
    }

    private fun generateIntelligentLocalResponse(
        userPrompt: String,
        isArabic: Boolean,
        summary: LocalFinancialSummary,
        categoryList: List<String>,
        installmentList: List<String>,
        recentTxList: List<String>,
        startOfMonth: Long,
        err: AppError?
    ): AiMessage {
        val userName = preferences.userPreferredName.trim()
        val greeting = if (userName.isNotBlank()) "يا $userName" else "يا بطل"
        val lower = userPrompt.lowercase(Locale.ROOT).trim()

        val budgetStr = String.format(Locale.US, "%,d", summary.monthlyBudget.toInt())
        val spentStr = String.format(Locale.US, "%,d", summary.totalExpenseMonth.toInt())
        val remStr = String.format(Locale.US, "%,d", summary.remainingBudget.toInt())
        val burnStr = String.format(Locale.US, "%,d", summary.dailyBurn.toInt())

        // 1. Installments & Debts Intent (أقساط، قسط، ديون، دين، التزامات)
        if (lower.contains("قسط") || lower.contains("أقساط") || lower.contains("اقساط") ||
            lower.contains("ديون") || lower.contains("دين") || lower.contains("التزامات") ||
            lower.contains("installment") || lower.contains("debt")) {
            val detailedInst = database?.let { getDetailedActiveInstallments(it) } ?: emptyList()
            val replyText = if (isArabic) {
                if (detailedInst.isEmpty()) {
                    "معندكش أي أقساط أو ديون مسجلة للشهر ده (0 ج.م مستحقة)."
                } else {
                    val count = detailedInst.size
                    val countStr = if (count == 1) "قسط واحد" else if (count == 2) "قسطين" else "$count أقساط"
                    val totalMonthly = detailedInst.sumOf { it.monthlyAmount }
                    val totalMonthlyStr = String.format(Locale.US, "%,d", totalMonthly.toInt())

                    "عليك $countStr مستحقة الشهر ده بإجمالي $totalMonthlyStr ج.م:\n\n" +
                    detailedInst.joinToString("\n\n") { inst ->
                        "• ${inst.name} (${inst.provider}):\n  - القسط الشهري: ${String.format(Locale.US, "%,d", inst.monthlyAmount.toInt())} ج.م (مستحق يوم ${inst.dueDay} في الشهر)\n  - إجمالي المديونية: ${String.format(Locale.US, "%,d", inst.totalAmount.toInt())} ج.م"
                    } +
                    "\n\nتقدر تتابع مواعيد سدادها أول بأول من شاشة الأقساط."
                }
            } else {
                if (detailedInst.isEmpty()) {
                    "You have no active installments or debts due this month (0 EGP)."
                } else {
                    val totalMonthly = detailedInst.sumOf { it.monthlyAmount }
                    val totalMonthlyStr = String.format(Locale.US, "%,d", totalMonthly.toInt())
                    "You have ${detailedInst.size} active installment(s) this month totaling $totalMonthlyStr EGP:\n\n" +
                    detailedInst.joinToString("\n\n") { inst ->
                        "• ${inst.name} (${inst.provider}):\n  - Monthly payment: ${String.format(Locale.US, "%,d", inst.monthlyAmount.toInt())} EGP\n  - Due date: Day ${inst.dueDay} of month\n  - Total remaining: ${String.format(Locale.US, "%,d", inst.totalAmount.toInt())} EGP"
                    }
                }
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.INSTALLMENTS,
                        labelAr = "عرض جدول الأقساط",
                        labelEn = "View Installments"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 2. Change Name Intent (غير اسمي، خلّي اسمي، سميني، change my name)
        val nameMatch = Regex("""(?:خلي|خلّي|سميني|غير اسمي لـ|غير اسمي الى|change my name to|call me)\s+([^\s]+)""").find(userPrompt)
        if (nameMatch != null) {
            val newName = nameMatch.groupValues[1].trim()
            if (newName.isNotBlank() && !newName.equals("اسمي", ignoreCase = true)) {
                preferences.userPreferredName = newName
                val replyText = if (isArabic) {
                    "منور الدنيا يا $newName! غيرت اسمك خلاص، تحب نراجع ميزانيتك دلوقتي؟"
                } else {
                    "Nice to meet you, $newName! I've updated your name. How can I help with your budget today?"
                }
                return AiMessage(
                    sender = MessageSender.ASSISTANT,
                    text = replyText,
                    sourceEngine = AiEngineType.LOCAL_FAST,
                    error = err
                )
            }
        }

        // 3. Change Budget Intent (خلّي ميزانيتي، غير الميزانية لـ، change budget to)
        val budgetMatch = Regex("""(?:خلي|خلّي|غير الميزانية لـ|عدل الميزانية لـ|set budget to|change budget to)\s*([\d,.]+)""").find(userPrompt)
        if (budgetMatch != null) {
            val rawBudget = budgetMatch.groupValues[1].replace(",", "").trim()
            val newBudget = rawBudget.toDoubleOrNull()
            if (newBudget != null && newBudget > 0) {
                preferences.monthlyBudget = newBudget
                database?.notifyTxChanged()
                val newBudgetStr = String.format(Locale.US, "%,d", newBudget.toInt())
                val replyText = if (isArabic) {
                    "تمام $greeting! عدلت ميزانيتك الشهرية لـ $newBudgetStr ج.م. الحسابات اتحدثت تلقائياً."
                } else {
                    "All set $userName! Updated your monthly budget to $newBudgetStr EGP."
                }
                return AiMessage(
                    sender = MessageSender.ASSISTANT,
                    text = replyText,
                    actions = listOf(
                        AiAction.Navigate(
                            target = NavigationTarget.BUDGET_EDIT,
                            labelAr = "عرض الميزانية",
                            labelEn = "View Budget"
                        )
                    ),
                    sourceEngine = AiEngineType.LOCAL_FAST,
                    error = err
                )
            }
        }

        // 4. Add / Record Expense Intent (سجل، صرفت، دفعت، اشتريت)
        val (_, addProposalFromTag) = extractAddTransactionProposal(userPrompt, isArabic)
        val naturalAddProposal = addProposalFromTag ?: parseNaturalLanguageExpense(userPrompt, isArabic)
        if (naturalAddProposal != null) {
            val replyText = if (isArabic) {
                "أكيد $greeting، جهزتلك كارت تسجيل المصروف ده:\n• المبلغ: ${String.format(Locale.US, "%,d", naturalAddProposal.amount.toInt())} ج.م\n• البند: ${naturalAddProposal.merchant.ifBlank { naturalAddProposal.categoryName }}\n\nاضغط على الزرار لتأكيد الحفظ في ميزانيتك."
            } else {
                "Sure $userName, I've prepared this transaction:\n• Amount: ${String.format(Locale.US, "%,d", naturalAddProposal.amount.toInt())} EGP\n• Item: ${naturalAddProposal.merchant}\n\nTap confirm to save it to your budget."
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(naturalAddProposal),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 5. Delete Transaction Intent (احذف، امسح)
        val (_, delProposal) = extractDeleteTransactionProposal(userPrompt)
        if (delProposal != null) {
            val replyText = if (isArabic) {
                "أكيد $greeting، اضغط على زرار التأكيد لحذف المعاملة."
            } else {
                "Sure $userName, tap confirm to delete this transaction."
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(delProposal),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 6. What-if Simulation Intent (لو صرفت 5,000 ج.م إيه الحسبة؟ / what if I spend)
        val simMatch = Regex("""(?:لو صرفت|لو اشتريت|لو دفعت|what if i spend)\s*([\d,.]+)""", RegexOption.IGNORE_CASE).find(lower)
        if (simMatch != null) {
            val rawAmountStr = simMatch.groupValues[1].replace(",", "").trim()
            val simAmount = rawAmountStr.toDoubleOrNull() ?: 5000.0
            val newRemaining = summary.remainingBudget - simAmount
            val newSpent = summary.totalExpenseMonth + simAmount
            val spentPct = if (summary.monthlyBudget > 0) ((newSpent / summary.monthlyBudget) * 100).toInt() else 0
            val simAmountStr = String.format(Locale.US, "%,d", simAmount.toInt())
            val newRemainingStr = String.format(Locale.US, "%,d", newRemaining.toInt())

            val replyText = if (isArabic) {
                if (newRemaining >= 0) {
                    "بص يا $greeting، لو صرفت $simAmountStr ج.م دلوقتي الحسبة هتبقى كالآتي:\n• اللي هيفضل في جيبك: $newRemainingStr ج.م (بدل $remStr ج.م)\n• نسبة استهلاكك للميزانية هتزيد لـ $spentPct%\n• كده إنت لسه في الأمان وتحت سقف الميزانية، بس حاول توازن مصاريفك لبقية الشهر! 👍"
                } else {
                    val deficitStr = String.format(Locale.US, "%,d", (-newRemaining).toInt())
                    "خد بالك $greeting! ⚠️ لو صرفت $simAmountStr ج.م دلوقتي هتكسر حاجز الميزانية الشهرية ويحصل معاك عجز $deficitStr ج.م!\n• إجمالي صرفك هيوصل لـ ${String.format(Locale.US, "%,d", newSpent.toInt())} ج.م (أكبر من ميزانيتك $budgetStr ج.م)\nأنصحك تأجل المصروف ده شوية أو تدور على بديل أوفر."
                }
            } else {
                if (newRemaining >= 0) {
                    "If you spend $simAmountStr EGP now, $userName:\n• Remaining budget drops to $newRemainingStr EGP (down from $remStr EGP)\n• You will have used $spentPct% of your monthly budget.\n• You're still within your safety margin! 👍"
                } else {
                    val deficitStr = String.format(Locale.US, "%,d", (-newRemaining).toInt())
                    "Caution $userName! ⚠️ Spending $simAmountStr EGP will exceed your monthly budget by $deficitStr EGP (total spending: ${String.format(Locale.US, "%,d", newSpent.toInt())} EGP vs $budgetStr EGP budget)."
                }
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.ShowBudgetSnapshot(
                        monthlyBudget = summary.monthlyBudget,
                        totalSpent = newSpent,
                        remaining = newRemaining,
                        burnRate = summary.dailyBurn,
                        depletionDate = if (isArabic) "بعد الصرف المقترح" else "After proposed spend"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 7. Budget Optimization & Advice Intent (إزاي أظبط ميزانيتي؟ / نصايح / توفير / optimize)
        if (lower.contains("أظبط") || lower.contains("اظبط") || lower.contains("أوفر") || lower.contains("اوفر") ||
            lower.contains("توفير") || lower.contains("نصيحة") || lower.contains("نصايح") || lower.contains("optimize")) {
            val replyText = if (isArabic) {
                "عشان تظبط ميزانيتك وتحوش أكتر $greeting، إليك أهم 3 خطوات عملية مبنية على أرقامك الحالية:\n\n" +
                "1. قاعدة 50/30/20:\nخصص 50% للالتزامات الأساسية (أكل وفواتير)، 30% للمتطلبات الشخصية، و 20% حولهم أول الشهر لحساب التوفير مباشرة.\n\n" +
                "2. سقف الصرف اليومي:\nمعدل صرفك الحالي حوالي $burnStr ج.م/يوم. لو التزمت بسقف ثابت يومياً مش هتتفاجئ بانتهاء الميزانية قبل آخر الشهر.\n\n" +
                "3. راقب بنود الصرف الخفية:\nأكتر بنود بتسحب فلوس بدون ما نشعر هي الكافيهات والأكل الدليفري والشراء الاندفاعي أونلاين. تقدر تتابع مصاريفك أول بأول من التحليلات."
            } else {
                "Here are 3 practical steps to optimize your budget, $userName:\n\n" +
                "1. The 50/30/20 Rule: 50% essentials, 30% lifestyle, and 20% to savings.\n" +
                "2. Daily Spending Limit: Your current burn rate is $burnStr EGP/day. Staying under this cap keeps your budget balanced.\n" +
                "3. Watch Hidden Leaks: Cafe visits, food deliveries, and online impulse buys add up quickly."
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.ANALYTICS,
                        labelAr = "عرض تحليلات المصاريف",
                        labelEn = "View Expense Analytics"
                    ),
                    AiAction.Navigate(
                        target = NavigationTarget.BUDGET_EDIT,
                        labelAr = "تعديل الميزانية الشهرية",
                        labelEn = "Adjust Monthly Budget"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 8. Bank SMS Import Guide Intent (إزاي أسحب رسائل البنك؟ / رسائل البنك / sms)
        if (lower.contains("رسائل") || lower.contains("رساله") || lower.contains("رسالة") || lower.contains("sms") ||
            lower.contains("انستاباي") || lower.contains("إنستاباي") || lower.contains("فودافون كاش") || lower.contains("بنك")) {
            val replyText = if (isArabic) {
                "سحب رسائل البنك والمحافظ الذكية سهل جداً وبضغطة واحدة $greeting! 📱💳\n\n" +
                "• التطبيق بيقرأ رسائل الخصم والشراء والإيداع من بنوكك ومحافظك تلقائياً.\n" +
                "• بيسجل المعاملة بالمبلغ واسم التاجر والتاريخ بدون أي إدخال يدوي.\n\n" +
                "اضغط على الزرار تحت للذهاب لشاشة سحب الرسائل واستيراد حركاتك فوراً."
            } else {
                "Importing bank and wallet SMS messages is fast and automatic, $userName! 📱💳\n\n" +
                "• The app scans transaction alerts from banks and wallets automatically.\n" +
                "• Amounts, merchants, and categories are parsed with zero manual effort.\n\n" +
                "Tap the button below to import your bank SMS messages now."
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.SMS_IMPORT,
                        labelAr = "سحب رسائل البنك الآن",
                        labelEn = "Import Bank SMS Now"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 9. Specific Category Spending Intent (كام صرفت على الأكل؟ / مواصلات / كافيهات / فواتير / بقالة)
        val isFoodQuery = lower.contains("أكل") || lower.contains("اكل") || lower.contains("طعام") || lower.contains("مطعم") || lower.contains("غدا") || lower.contains("عشا") || lower.contains("فطار") || lower.contains("food")
        val isTransportQuery = lower.contains("مواصلات") || lower.contains("بنزين") || lower.contains("أوبر") || lower.contains("اوبر") || lower.contains("تاكسي") || lower.contains("transport")
        val isCafeQuery = lower.contains("كافيه") || lower.contains("قهوة") || lower.contains("شاي") || lower.contains("مشروب") || lower.contains("coffee") || lower.contains("cafe")
        val isGroceryQuery = lower.contains("سوبر") || lower.contains("ماركت") || lower.contains("بقالة") || lower.contains("طلبات") || lower.contains("grocery")
        val isBillsQuery = lower.contains("فواتير") || lower.contains("فاتورة") || lower.contains("كهربا") || lower.contains("نت") || lower.contains("غاز") || lower.contains("شحن") || lower.contains("bills")
        val isHealthQuery = lower.contains("صحة") || lower.contains("علاج") || lower.contains("دكتور") || lower.contains("صيدلية") || lower.contains("أدوية") || lower.contains("health")
        val isShoppingQuery = lower.contains("تسوق") || lower.contains("شوبنج") || lower.contains("لبس") || lower.contains("هدوم") || lower.contains("shopping")

        if (isFoodQuery || isTransportQuery || isCafeQuery || isGroceryQuery || isBillsQuery || isHealthQuery || isShoppingQuery) {
            val targetCategoryIds = when {
                isFoodQuery || isCafeQuery -> listOf("cat_food")
                isTransportQuery -> listOf("cat_transport")
                isGroceryQuery -> listOf("cat_groceries")
                isBillsQuery -> listOf("cat_bills")
                isShoppingQuery -> listOf("cat_shopping")
                isHealthQuery -> listOf("cat_health")
                else -> listOf("cat_other")
            }

            val defaultNameAr = when {
                isFoodQuery -> "الأكل والمطاعم"
                isCafeQuery -> "الكافيهات والمشروبات"
                isTransportQuery -> "المواصلات والبنزين"
                isGroceryQuery -> "السوبرماركت والبقالة"
                isBillsQuery -> "الفواتير والاشتراكات"
                isShoppingQuery -> "التسوق والملابس"
                isHealthQuery -> "الصحة والأدوية"
                else -> "مصاريف أخرى"
            }
            val defaultNameEn = when {
                isFoodQuery -> "Food & Dining"
                isCafeQuery -> "Cafes & Drinks"
                isTransportQuery -> "Transportation"
                isGroceryQuery -> "Groceries"
                isBillsQuery -> "Bills & Utilities"
                isShoppingQuery -> "Shopping"
                isHealthQuery -> "Healthcare"
                else -> "Other Expenses"
            }

            val detail = database?.let { getSpecificCategoryDetail(it, targetCategoryIds, startOfMonth) }

            val replyText = if (isArabic) {
                if (detail != null && detail.totalAmount > 0) {
                    val totalStr = String.format(Locale.US, "%,d", detail.totalAmount.toInt())
                    val pct = if (summary.totalExpenseMonth > 0) ((detail.totalAmount / summary.totalExpenseMonth) * 100).toInt() else 0
                    val count = detail.transactionCount
                    val countStr = if (count == 1) "عملية واحدة" else if (count == 2) "عمليتين" else "$count عمليات"
                    val itemsList = detail.topTransactions.take(6).joinToString("\n") { "• $it" }

                    "صرفت $totalStr ج.م على $defaultNameAr الشهر ده، متوزعة على $countStr:\n\n" +
                    itemsList +
                    "\n\n(بتمثل $pct% من إجمالي مصاريفك للشهر)."
                } else {
                    "صرفت 0 ج.م على $defaultNameAr الشهر ده (معندكش أي عمليات مسجلة في البند ده لحد دلوقتي)."
                }
            } else {
                if (detail != null && detail.totalAmount > 0) {
                    val totalStr = String.format(Locale.US, "%,d", detail.totalAmount.toInt())
                    val pct = if (summary.totalExpenseMonth > 0) ((detail.totalAmount / summary.totalExpenseMonth) * 100).toInt() else 0
                    val count = detail.transactionCount
                    val countStr = if (count == 1) "1 transaction" else "$count transactions"
                    val itemsList = detail.topTransactions.take(6).joinToString("\n") { "• $it" }

                    "You spent $totalStr EGP on $defaultNameEn this month across $countStr:\n\n" +
                    itemsList +
                    "\n\n(Represents $pct% of your total spending this month)."
                } else {
                    "You spent 0 EGP on $defaultNameEn this month (0 recorded transactions)."
                }
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.ANALYTICS,
                        labelAr = "عرض التحليلات بالتفصيل",
                        labelEn = "View Detailed Analytics"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // Specific Merchant Query (e.g. كام صرفت في كارفور / ستاربكس)
        val cleanMerchantQuery = lower
            .replace("كام", "")
            .replace("صرفت", "")
            .replace("دفعت", "")
            .replace("في", "")
            .replace("عند", "")
            .replace("على", "")
            .replace("how much", "")
            .replace("did i spend", "")
            .replace("at", "")
            .replace("on", "")
            .replace("?", "")
            .replace("؟", "")
            .trim()
        val merchantDetail = if (cleanMerchantQuery.length >= 3 && !cleanMerchantQuery.contains("شهر") && !cleanMerchantQuery.contains("ميزاني") && !cleanMerchantQuery.contains("قسط")) {
            database?.let { getMerchantDetail(it, cleanMerchantQuery, startOfMonth) }
        } else null

        if (merchantDetail != null && merchantDetail.totalAmount > 0) {
            val totalStr = String.format(Locale.US, "%,d", merchantDetail.totalAmount.toInt())
            val count = merchantDetail.count
            val countStr = if (count == 1) "عملية واحدة" else if (count == 2) "عمليتين" else "$count عمليات"
            val itemsList = merchantDetail.transactions.take(5).joinToString("\n") { "• $it" }
            val replyText = if (isArabic) {
                "صرفت في ${merchantDetail.merchantName} الشهر ده $totalStr ج.م (متوزعة على $countStr):\n\n" +
                itemsList
            } else {
                "You spent $totalStr EGP at ${merchantDetail.merchantName} this month across $count transaction(s):\n\n" +
                itemsList
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.ANALYTICS,
                        labelAr = "عرض تفاصيل المعاملات",
                        labelEn = "View Transactions"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 10. General Categories Breakdown (فئات، تصنيف، أكتر فئة، categories)
        if (lower.contains("فئات") || lower.contains("تصنيف") || lower.contains("اكتر فئة") || lower.contains("أكتر حاجة") || lower.contains("categories")) {
            val replyText = if (isArabic) {
                if (categoryList.isEmpty()) {
                    "لسه مفيش تقسيم فئات مسجل للشهر ده $greeting."
                } else {
                    "توزيع مصاريفك حسب الفئات للشهر ده $greeting:\n\n" +
                    categoryList.joinToString("\n") { "• $it" }
                }
            } else {
                if (categoryList.isEmpty()) {
                    "No category spending recorded yet for this month, $userName."
                } else {
                    "Your category spending breakdown for this month, $userName:\n\n" +
                    categoryList.joinToString("\n") { "• $it" }
                }
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.ANALYTICS,
                        labelAr = "فتح شاشة التحليلات",
                        labelEn = "Open Analytics"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 11. Recent Transactions Intent (آخر مصاريف، آخر العمليات، آخر عملية، سجلت إيه)
        if (lower.contains("آخر") || lower.contains("اخر") || lower.contains("عمليات") || lower.contains("معاملات") || lower.contains("recent")) {
            val replyText = if (isArabic) {
                if (recentTxList.isEmpty()) {
                    "لسه مفيش أي معاملات مسجلة في حسابك $greeting."
                } else {
                    "آخر العمليات اللي سجلتها $greeting:\n\n" +
                    recentTxList.take(4).joinToString("\n") { "• $it" }
                }
            } else {
                if (recentTxList.isEmpty()) {
                    "No recent transactions recorded yet, $userName."
                } else {
                    "Here are your most recent transactions, $userName:\n\n" +
                    recentTxList.take(4).joinToString("\n") { "• $it" }
                }
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 12. Income / Salary Intent (الدخل، المرتب، قبضت، راتب)
        if (lower.contains("دخل") || lower.contains("مرتب") || lower.contains("قبض") || lower.contains("راتب") || lower.contains("income") || lower.contains("salary")) {
            val incomeStr = String.format(Locale.US, "%,d", summary.totalIncomeMonth.toInt())
            val replyText = if (isArabic) {
                "إجمالي الدخل والأرباح المسجلة في حسابك للشهر ده $greeting هو $incomeStr ج.م، وصرفت منه $spentStr ج.م حتى الآن."
            } else {
                "Your total logged income for this month is $incomeStr EGP, $userName (total expenses: $spentStr EGP)."
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.ANALYTICS,
                        labelAr = "عرض تفاصيل الدخل والمصاريف",
                        labelEn = "View Income & Expenses"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 13. Top Merchants Intent (محلات، متاجر، أكتر مكان صرفت فيه)
        if (lower.contains("محلات") || lower.contains("محل") || lower.contains("تاجر") || lower.contains("اكتر مكان") || lower.contains("أكتر مكان") || lower.contains("merchants")) {
            val replyText = if (isArabic) {
                if (summary.topMerchants.isEmpty()) {
                    "لسه مفيش محلات أو تجار مسجلين في مصاريف الشهر ده $greeting."
                } else {
                    "أكتر محلات صرفت فيها فلوس الشهر ده $greeting:\n\n" +
                    summary.topMerchants.take(5).joinToString("\n") { "• ${it.merchantName}: ${String.format(Locale.US, "%,d", it.totalAmount.toInt())} ج.م" }
                }
            } else {
                if (summary.topMerchants.isEmpty()) {
                    "No merchant spending recorded yet for this month, $userName."
                } else {
                    "Your top spending merchants this month, $userName:\n\n" +
                    summary.topMerchants.take(5).joinToString("\n") { "• ${it.merchantName}: ${String.format(Locale.US, "%,d", it.totalAmount.toInt())} EGP" }
                }
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.ANALYTICS,
                        labelAr = "عرض التحليلات",
                        labelEn = "View Analytics"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 14. Friendly Greetings & Chit-Chat (أهلاً، صباح الخير، مساء الخير، شكراً، تسلم، تمام، حبيبي)
        if (lower.contains("صباح الخير") || lower.contains("مساء الخير") || lower.contains("سلام عليكم") ||
            lower.contains("السلام عليكم") || lower.contains("أهلاً") || lower.contains("اهلا") ||
            lower.contains("ازيك") || lower.contains("إزيك") || lower.contains("عامل ايه") ||
            lower.contains("شكرا") || lower.contains("شكراً") || lower.contains("تسلم") ||
            lower.contains("حبيبي") || lower.contains("hello") || lower.contains("hi") || lower.contains("thanks")) {
            val replyText = if (isArabic) {
                "أهلاً بيك $greeting! منور دايماً، أنا معاك ومتابع مصاريفك وأقساطك خطوة بخطوة، تحب نحسب إيه دلوقتي؟"
            } else {
                "Hello $userName! Always happy to assist. How can I help with your budget, expenses, or installments today?"
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 15. Remaining Budget & Burn Rate Intent (كام باقي، فاضل كام، المتبقي، رصيد)
        if (lower.contains("باقي") || lower.contains("فاضل") || lower.contains("متبقي") || lower.contains("رصيد") || lower.contains("remaining")) {
            val replyText = if (isArabic) {
                "فاضل في جيبك وميزانيتك للشهر ده: $remStr ج.م (من أصل $budgetStr ج.م)، ومعدل صرفك اليومي حوالي $burnStr ج.م/يوم."
            } else {
                "Your remaining budget for this month is $remStr EGP out of $budgetStr EGP ($burnStr EGP/day burn rate)."
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.ShowBudgetSnapshot(
                        monthlyBudget = summary.monthlyBudget,
                        totalSpent = summary.totalExpenseMonth,
                        remaining = summary.remainingBudget,
                        burnRate = summary.dailyBurn,
                        depletionDate = if (isArabic) "نهاية الشهر" else "End of Month"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 16. Safety / Status Query (ميزانيتي في أمان؟، ميزانيتي تمام؟، وضعي المالي، في خطر؟)
        if (lower.contains("أمان") || lower.contains("امان") || lower.contains("سليم") || lower.contains("سليمة") ||
            lower.contains("خطر") || lower.contains("وضعي") || lower.contains("وضع") || lower.contains("safe")) {
            val replyText = if (isArabic) {
                if (summary.monthlyBudget > 0 && summary.remainingBudget > 0) {
                    "أيوة $greeting، ميزانيتك في أمان ومعدل صرفك مستقر ($burnStr ج.م/يوم)، وفاضل معاك $remStr ج.م يكفيك لبقية الشهر بانتظام."
                } else if (summary.monthlyBudget > 0 && summary.remainingBudget <= 0) {
                    "خد بالك $greeting، ميزانيتك خلصت أو عديت السقف المحدد! صرفت $spentStr ج.م من أصل $budgetStr ج.م. امسك إيدك شوية عشان تظبط أمورك."
                } else {
                    "وضعك المالي ماشي بانتظام $greeting: صرفت لحد دلوقتي $spentStr ج.م ومعدل حرقك اليومي $burnStr ج.م/يوم."
                }
            } else {
                "Your budget status is healthy, $userName: spent $spentStr EGP out of $budgetStr EGP with $remStr EGP remaining."
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.ShowBudgetSnapshot(
                        monthlyBudget = summary.monthlyBudget,
                        totalSpent = summary.totalExpenseMonth,
                        remaining = summary.remainingBudget,
                        burnRate = summary.dailyBurn,
                        depletionDate = if (isArabic) "نهاية الشهر" else "End of Month"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 17. Overall Monthly Summary Intent (لخصلي مصاريف الشهر، إجمالي المصاريف، ملخص، تقرير)
        if (lower.contains("لخص") || lower.contains("ملخص") || lower.contains("تقرير") || lower.contains("إجمالي") || lower.contains("اجمالي") || lower.contains("summary") || lower.contains("summarize")) {
            val replyText = if (isArabic) {
                "ملخصك المالي للشهر ده $greeting:\n• اللي صرفته: $spentStr ج.م\n• اللي فاضل معاك: $remStr ج.م من أصل $budgetStr ج.م\n• معدل الصرف اليومي: $burnStr ج.م/يوم"
            } else {
                "Monthly financial summary for $userName:\n• Total Spent: $spentStr EGP\n• Remaining: $remStr EGP out of $budgetStr EGP\n• Daily Burn: $burnStr EGP/day"
            }
            return AiMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                actions = listOf(
                    AiAction.ShowBudgetSnapshot(
                        monthlyBudget = summary.monthlyBudget,
                        totalSpent = summary.totalExpenseMonth,
                        remaining = summary.remainingBudget,
                        burnRate = summary.dailyBurn,
                        depletionDate = if (isArabic) "نهاية الشهر" else "End of Month"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST,
                error = err
            )
        }

        // 18. Default Contextual Fallback (لا يكرر الكلام إطلاقاً)
        val defaultText = if (isArabic) {
            "أنا معاك يا $greeting خطوة بخطوة عشان ندير فلوسك بذكاء. حالياً ميزانيتك الشهرية $budgetStr ج.م، صرفت منها $spentStr ج.م وفاضل في جيبك $remStr ج.م.\n\nتقدر تسألني عن:\n• أقساطك أو التزاماتك للشهر ده\n• كام صرفت على الأكل أو المواصلات\n• تسجيل عملية جديدة أو استيراد رسائل البنك\n• حساب أي سيناريو صرف تفكر فيه!"
        } else {
            "I'm here with you, $userName! Your monthly budget is $budgetStr EGP, you've spent $spentStr EGP with $remStr EGP remaining.\n\nFeel free to ask about your installments, category breakdowns, simulate expenses, or import bank SMS!"
        }

        return AiMessage(
            sender = MessageSender.ASSISTANT,
            text = defaultText,
            sourceEngine = AiEngineType.LOCAL_FAST,
            error = err
        )
    }

    private fun parseNaturalLanguageExpense(prompt: String, isArabic: Boolean): AiAction.ConfirmTransaction? {
        val lower = prompt.lowercase(Locale.ROOT)
        val hasIntent = lower.contains("سجل") || lower.contains("صرفت") || lower.contains("دفعت") ||
                        lower.contains("اشتريت") || lower.contains("spent") || lower.contains("record") ||
                        lower.contains("bought")
        if (!hasIntent) return null

        val amountMatch = Regex("""\b(\d+(?:\.\d+)?)\b""").find(prompt) ?: return null
        val amount = amountMatch.groupValues[1].toDoubleOrNull() ?: return null
        if (amount <= 0) return null

        val (catId, merchantDefault) = when {
            lower.contains("بنزين") || lower.contains("مواصلات") || lower.contains("أوبر") || lower.contains("اوبر") || lower.contains("تاكسي") ->
                Pair("cat_transport", if (isArabic) "مواصلات وبنزين" else "Transportation")
            lower.contains("قهوة") || lower.contains("كافيه") || lower.contains("ستاربكس") || lower.contains("شاي") ->
                Pair("cat_food", if (isArabic) "كافيه ومشروبات" else "Cafe & Drinks")
            lower.contains("أكل") || lower.contains("اكل") || lower.contains("مطعم") || lower.contains("غدا") || lower.contains("عشا") || lower.contains("فطار") ->
                Pair("cat_food", if (isArabic) "طعام ومطاعم" else "Dining")
            lower.contains("سوبر") || lower.contains("ماركت") || lower.contains("بقالة") || lower.contains("طلبات") ->
                Pair("cat_groceries", if (isArabic) "سوبرماركت" else "Groceries")
            lower.contains("صيدلية") || lower.contains("علاج") || lower.contains("دوا") ->
                Pair("cat_health", if (isArabic) "صيدلية وأدوية" else "Pharmacy")
            lower.contains("فاتورة") || lower.contains("نت") || lower.contains("كهربا") || lower.contains("شحن") ->
                Pair("cat_bills", if (isArabic) "فواتير وشحن" else "Bills")
            else -> Pair("cat_other", if (isArabic) "مصروف جديد" else "New Expense")
        }

        val paymentSource = when {
            lower.contains("فودافون") -> "VODAFONE_CASH"
            lower.contains("انستاباي") || lower.contains("إنستاباي") -> "INSTAPAY"
            lower.contains("فيزا") || lower.contains("كارت") -> "CARD"
            else -> "CASH"
        }

        val category = Category.findDefault(catId)
        return AiAction.ConfirmTransaction(
            amount = amount,
            type = TransactionType.EXPENSE,
            merchant = merchantDefault,
            categoryId = catId,
            categoryName = if (isArabic) category.nameAr else category.nameEn,
            paymentSource = paymentSource,
            notes = if (isArabic) "تسجيل عبر المساعد الذكي" else "Logged via AI Copilot",
            status = ActionStatus.PENDING
        )
    }

    private fun extractActionsFromResponse(raw: String, isArabic: Boolean): Pair<String, List<AiAction>> {
        val actions = mutableListOf<AiAction>()
        var text = extractNameUpdate(raw)
        text = extractBudgetUpdate(text)

        val (textAfterAdd, addProposal) = extractAddTransactionProposal(text, isArabic)
        text = textAfterAdd
        addProposal?.let { actions.add(it) }

        val (textAfterDel, delProposal) = extractDeleteTransactionProposal(text)
        text = textAfterDel
        delProposal?.let { actions.add(it) }

        val (textAfterNav, navActions) = extractNavigationActions(text)
        text = textAfterNav
        actions.addAll(navActions)

        return Pair(text, actions)
    }

    private fun extractNameUpdate(input: String): String {
        val match = Regex("""\[EXEC:SET_NAME:\s*([^\]]+)\]""").find(input) ?: return input
        val newName = match.groupValues[1].trim()
        if (newName.isNotBlank()) {
            preferences.userPreferredName = newName
        }
        return input.replace(match.value, "").trim()
    }

    private fun extractBudgetUpdate(input: String): String {
        val match = Regex("""\[EXEC:SET_BUDGET:\s*([0-9.,]+)\]""").find(input) ?: return input
        val rawBudget = match.groupValues[1].replace(",", "").trim()
        val newBudget = rawBudget.toDoubleOrNull()
        if (newBudget != null && newBudget > 0) {
            preferences.monthlyBudget = newBudget
            database?.notifyTxChanged()
        }
        return input.replace(match.value, "").trim()
    }

    private fun extractAddTransactionProposal(
        input: String,
        isArabic: Boolean
    ): Pair<String, AiAction.ConfirmTransaction?> {
        val match = Regex("""\[PROPOSE:ADD_TX:\s*(\{.*?\})\]""").find(input) ?: return Pair(input, null)
        val jsonStr = match.groupValues[1]
        var action: AiAction.ConfirmTransaction? = null
        try {
            val json = JSONObject(jsonStr)
            val amount = json.optDouble("amount", 0.0)
            if (amount > 0) {
                val merchant = json.optString("merchant", if (isArabic) "معاملة جديدة" else "New Transaction")
                val categoryId = json.optString("category", "cat_other")
                val isIncome = json.optString("type", "EXPENSE").equals("INCOME", ignoreCase = true)
                val category = Category.findDefault(categoryId)
                val defaultNote = if (isArabic) "تسجيل بواسطة المساعد الذكي" else "Logged via AI Copilot"

                action = AiAction.ConfirmTransaction(
                    amount = amount,
                    type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                    merchant = merchant,
                    categoryId = categoryId,
                    categoryName = if (isArabic) category.nameAr else category.nameEn,
                    paymentSource = json.optString("source", "CASH"),
                    notes = json.optString("notes", defaultNote),
                    status = ActionStatus.PENDING
                )
            }
        } catch (e: org.json.JSONException) {
            android.util.Log.w("AiCopilotRepo", "Failed to parse add transaction proposal JSON: ${e.message}")
        }
        return Pair(input.replace(match.value, "").trim(), action)
    }

    private fun extractDeleteTransactionProposal(input: String): Pair<String, AiAction.ConfirmDeleteTransaction?> {
        val match = Regex("""\[PROPOSE:DELETE_TX:\s*(\{.*?\})\]""").find(input) ?: return Pair(input, null)
        val jsonStr = match.groupValues[1]
        var action: AiAction.ConfirmDeleteTransaction? = null
        try {
            val json = JSONObject(jsonStr)
            val txId = json.optString("id", "")
            if (txId.isNotBlank()) {
                action = AiAction.ConfirmDeleteTransaction(
                    transactionId = txId,
                    merchant = json.optString("merchant", ""),
                    amount = json.optDouble("amount", 0.0),
                    status = ActionStatus.PENDING
                )
            }
        } catch (e: org.json.JSONException) {
            android.util.Log.w("AiCopilotRepo", "Failed to parse delete transaction proposal JSON: ${e.message}")
        }
        return Pair(input.replace(match.value, "").trim(), action)
    }

    private fun extractNavigationActions(input: String): Pair<String, List<AiAction.Navigate>> {
        var currentText = input
        val navActions = mutableListOf<AiAction.Navigate>()
        val targets = listOf(
            Triple("[ACTION:ANALYTICS]", NavigationTarget.ANALYTICS, Pair("فتح شاشة التحليلات", "Open Analytics")),
            Triple("[ACTION:SMS_IMPORT]", NavigationTarget.SMS_IMPORT, Pair("مزامنة رسائل البنك", "Sync Bank SMS")),
            Triple("[ACTION:BUDGET_EDIT]", NavigationTarget.BUDGET_EDIT, Pair("تعديل الميزانية الشهرية", "Edit Monthly Budget")),
            Triple("[ACTION:INSTALLMENTS]", NavigationTarget.INSTALLMENTS, Pair("عرض وإدارة الأقساط", "Manage Installments")),
            Triple("[ACTION:EXPORT_REPORT]", NavigationTarget.EXPORT_REPORT, Pair("تصدير تقرير Excel", "Export Excel Report"))
        )

        for ((tag, target, labels) in targets) {
            if (currentText.contains(tag)) {
                currentText = currentText.replace(tag, "").trim()
                navActions.add(
                    AiAction.Navigate(
                        target = target,
                        labelAr = labels.first,
                        labelEn = labels.second
                    )
                )
            }
        }
        return Pair(currentText, navActions)
    }

    private fun getCategoryBreakdown(db: SayItDatabase, startOfMonth: Long): List<String> {
        val categorySummaries = mutableListOf<String>()
        try {
            val cursor = db.readableDatabase.rawQuery(
                """
                SELECT COALESCE(c.name_ar, 'أخرى') as cat_name, SUM(t.amount) as total
                FROM transactions t
                LEFT JOIN categories c ON t.category_id = c.id
                WHERE t.type = 'EXPENSE' AND t.timestamp >= ?
                GROUP BY cat_name
                ORDER BY total DESC
                LIMIT 6
                """.trimIndent(),
                arrayOf(startOfMonth.toString())
            )
            cursor.use {
                while (it.moveToNext()) {
                    val name = it.getString(0)
                    val sum = it.getDouble(1)
                    categorySummaries.add("$name: ${sum.toInt()} ج.م")
                }
            }
        } catch (e: android.database.SQLException) {
            android.util.Log.e("AiCopilotRepo", "Failed to query category breakdown", e)
        }
        return categorySummaries
    }

    private fun getActiveInstallments(db: SayItDatabase): List<String> {
        val installmentSummaries = mutableListOf<String>()
        try {
            val cursor = db.readableDatabase.rawQuery(
                """
                SELECT name, monthly_amount, due_day_of_month, status
                FROM installments
                WHERE status = 'ACTIVE'
                LIMIT 5
                """.trimIndent(),
                null
            )
            cursor.use {
                while (it.moveToNext()) {
                    val title = it.getString(0)
                    val amount = it.getDouble(1)
                    val dueDay = it.getInt(2)
                    installmentSummaries.add("$title: ${amount.toInt()} ج.م (يوم $dueDay)")
                }
            }
        } catch (e: android.database.SQLException) {
            android.util.Log.e("AiCopilotRepo", "Failed to query active installments", e)
        }
        return installmentSummaries
    }

    private fun getRecentTransactions(db: SayItDatabase): List<String> {
        val recentTransactions = mutableListOf<String>()
        try {
            val cursor = db.readableDatabase.rawQuery(
                """
                SELECT id, merchant, amount, type
                FROM transactions
                ORDER BY timestamp DESC
                LIMIT 5
                """.trimIndent(),
                null
            )
            cursor.use {
                while (it.moveToNext()) {
                    val id = it.getString(0)
                    val merchant = it.getString(1).ifBlank { "معاملة" }
                    val amount = it.getDouble(2)
                    val type = if (it.getString(3) == "EXPENSE") "مصروف" else "دخل"
                    recentTransactions.add("$merchant: ${amount.toInt()} ج.م ($type) [معرف: $id]")
                }
            }
        } catch (e: android.database.SQLException) {
            android.util.Log.e("AiCopilotRepo", "Failed to query recent transactions", e)
        }
        return recentTransactions
    }

    private fun getDetailedActiveInstallments(db: SayItDatabase): List<DetailedInstallment> {
        val list = mutableListOf<DetailedInstallment>()
        try {
            val cursor = db.readableDatabase.rawQuery(
                """
                SELECT name, provider, monthly_amount, due_day_of_month, total_amount
                FROM installments
                WHERE status = 'ACTIVE'
                ORDER BY due_day_of_month ASC
                LIMIT 10
                """.trimIndent(),
                null
            )
            cursor.use {
                while (it.moveToNext()) {
                    val name = it.getString(0)
                    val provider = it.getString(1).ifBlank { "جهة التقسيط" }
                    val monthlyAmount = it.getDouble(2)
                    val dueDay = it.getInt(3)
                    val totalAmount = it.getDouble(4)
                    list.add(DetailedInstallment(name, provider, monthlyAmount, dueDay, totalAmount))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AiCopilotRepo", "Failed to query detailed installments", e)
        }
        return list
    }

    private fun getSpecificCategoryDetail(
        db: SayItDatabase,
        categoryIds: List<String>,
        startOfMonth: Long
    ): SpecificCategoryDetail {
        var totalAmount = 0.0
        var count = 0
        val topTransactions = mutableListOf<String>()
        var catName = ""

        if (categoryIds.isEmpty()) return SpecificCategoryDetail("", 0.0, 0, emptyList())

        val placeholders = categoryIds.joinToString(",") { "?" }
        val args = (categoryIds + listOf("EXPENSE", startOfMonth.toString())).toTypedArray()

        try {
            val totalCursor = db.readableDatabase.rawQuery(
                """
                SELECT COUNT(*), COALESCE(SUM(amount), 0.0)
                FROM transactions
                WHERE category_id IN ($placeholders) AND type = ? AND timestamp >= ?
                """.trimIndent(),
                args
            )
            totalCursor.use {
                if (it.moveToNext()) {
                    count = it.getInt(0)
                    totalAmount = it.getDouble(1)
                }
            }

            val txCursor = db.readableDatabase.rawQuery(
                """
                SELECT COALESCE(merchant, 'معاملة'), amount
                FROM transactions
                WHERE category_id IN ($placeholders) AND type = ? AND timestamp >= ?
                ORDER BY timestamp DESC
                LIMIT 10
                """.trimIndent(),
                args
            )
            txCursor.use {
                while (it.moveToNext()) {
                    val merchant = it.getString(0).ifBlank { "معاملة" }
                    val amount = it.getDouble(1)
                    val amtStr = String.format(Locale.US, "%,d", amount.toInt())
                    topTransactions.add("$merchant: $amtStr ج.م")
                }
            }

            val catCursor = db.readableDatabase.rawQuery(
                "SELECT COALESCE(name_ar, name_en) FROM categories WHERE id = ? LIMIT 1",
                arrayOf(categoryIds.first())
            )
            catCursor.use {
                if (it.moveToNext()) {
                    catName = it.getString(0) ?: ""
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AiCopilotRepo", "Failed to query specific category detail", e)
        }

        return SpecificCategoryDetail(
            categoryName = catName,
            totalAmount = totalAmount,
            transactionCount = count,
            topTransactions = topTransactions
        )
    }

    private fun getMerchantDetail(
        db: SayItDatabase,
        query: String,
        startOfMonth: Long
    ): MerchantDetail {
        var totalAmount = 0.0
        var count = 0
        var resolvedName = query
        val transactions = mutableListOf<String>()

        try {
            val cursor = db.readableDatabase.rawQuery(
                """
                SELECT merchant, amount, timestamp
                FROM transactions
                WHERE LOWER(merchant) LIKE ? AND type = 'EXPENSE' AND timestamp >= ?
                ORDER BY timestamp DESC
                LIMIT 10
                """.trimIndent(),
                arrayOf("%${query.lowercase(Locale.ROOT)}%", startOfMonth.toString())
            )
            cursor.use {
                while (it.moveToNext()) {
                    val m = it.getString(0)
                    val a = it.getDouble(1)
                    resolvedName = m
                    totalAmount += a
                    count++
                    val amtStr = String.format(Locale.US, "%,d", a.toInt())
                    transactions.add("$m: $amtStr ج.م")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AiCopilotRepo", "Failed to query merchant detail", e)
        }

        return MerchantDetail(
            merchantName = resolvedName,
            totalAmount = totalAmount,
            count = count,
            transactions = transactions
        )
    }
}

data class DetailedInstallment(
    val name: String,
    val provider: String,
    val monthlyAmount: Double,
    val dueDay: Int,
    val totalAmount: Double
)

data class SpecificCategoryDetail(
    val categoryName: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val topTransactions: List<String>
)

data class MerchantDetail(
    val merchantName: String,
    val totalAmount: Double,
    val count: Int,
    val transactions: List<String>
)
