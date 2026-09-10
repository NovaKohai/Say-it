package com.example.sayit.data.ai

import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.data.local.SayItPreferences
import com.example.sayit.domain.model.AiAction
import com.example.sayit.domain.model.AiEngineType
import com.example.sayit.domain.model.AiMessage
import com.example.sayit.domain.model.MerchantSpend
import com.example.sayit.domain.model.MessageSender
import com.example.sayit.domain.model.NavigationTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

data class LocalFinancialSummary(
    val totalExpenseMonth: Double,
    val totalIncomeMonth: Double,
    val transactionCountMonth: Int,
    val monthlyBudget: Double,
    val remainingBudget: Double,
    val dailyBurn: Double,
    val topMerchants: List<MerchantSpend>
)

class LocalToolsExecutor(
    private val database: SayItDatabase,
    private val preferences: SayItPreferences
) {

    suspend fun getFinancialSummary(): LocalFinancialSummary = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = cal.timeInMillis

        var expense = 0.0
        var income = 0.0
        var count = 0

        try {
            val db = database.readableDatabase
            val cursor = db.rawQuery(
                """
                SELECT type, SUM(amount), COUNT(*) 
                FROM transactions 
                WHERE timestamp >= ? 
                GROUP BY type
                """.trimIndent(),
                arrayOf(startOfMonth.toString())
            )

            cursor.use {
                while (it.moveToNext()) {
                    val type = it.getString(0)
                    val sum = it.getDouble(1)
                    val cnt = it.getInt(2)
                    if (type == "EXPENSE") {
                        expense = sum
                        count += cnt
                    } else if (type == "INCOME") {
                        income = sum
                        count += cnt
                    }
                }
            }
        } catch (_: Exception) {
            // Guard against SQLite locks or unreadable DB
        }

        val budget = try { preferences.monthlyBudget } catch (_: Exception) { 0.0 }
        val remaining = (budget - expense).coerceAtLeast(0.0)

        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val dailyBurn = expense / dayOfMonth

        val topMerchants = getTopMerchantsInternal(5, startOfMonth)

        LocalFinancialSummary(
            totalExpenseMonth = expense,
            totalIncomeMonth = income,
            transactionCountMonth = count,
            monthlyBudget = budget,
            remainingBudget = remaining,
            dailyBurn = dailyBurn,
            topMerchants = topMerchants
        )
    }

    suspend fun getTopMerchants(limit: Int = 5): List<MerchantSpend> = withContext(Dispatchers.IO) {
        getTopMerchantsInternal(limit, 0L)
    }

    private fun getTopMerchantsInternal(limit: Int, sinceTime: Long): List<MerchantSpend> {
        val list = mutableListOf<MerchantSpend>()
        try {
            val db = database.readableDatabase
            val query = if (sinceTime > 0) {
                """
                SELECT merchant, SUM(amount) as total, COUNT(*) as cnt 
                FROM transactions 
                WHERE type = 'EXPENSE' AND timestamp >= ? 
                GROUP BY merchant 
                ORDER BY total DESC 
                LIMIT ?
                """.trimIndent()
            } else {
                """
                SELECT merchant, SUM(amount) as total, COUNT(*) as cnt 
                FROM transactions 
                WHERE type = 'EXPENSE' 
                GROUP BY merchant 
                ORDER BY total DESC 
                LIMIT ?
                """.trimIndent()
            }

            val args = if (sinceTime > 0) {
                arrayOf(sinceTime.toString(), limit.toString())
            } else {
                arrayOf(limit.toString())
            }

            val cursor = db.rawQuery(query, args)
            cursor.use {
                while (it.moveToNext()) {
                    val name = it.getString(0) ?: "غير محدد"
                    val total = it.getDouble(1)
                    val cnt = it.getInt(2)
                    list.add(MerchantSpend(merchantName = name, totalAmount = total, transactionCount = cnt))
                }
            }
        } catch (_: Exception) {
            // Safe fallback on database error
        }
        return list
    }

    suspend fun detectDirectIntentAndRespond(
        prompt: String,
        isArabic: Boolean,
        userName: String = ""
    ): AiMessage? = withContext(Dispatchers.IO) {
        val lower = prompt.lowercase(Locale.ROOT).trim()
        val greetingPrefix = if (userName.isNotBlank()) {
            if (isArabic) "أهلاً يا $userName، " else "Hello $userName, "
        } else ""

        // 1. Navigation / How-To Intents
        if (lower.contains("استيراد") || lower.contains("رسائل") || lower.contains("sms") || lower.contains("sync") || lower.contains("مزامنة")) {
            return@withContext AiMessage(
                sender = MessageSender.ASSISTANT,
                text = if (isArabic) {
                    "${greetingPrefix}يمكنك استيراد ومزامنة رسائل البنك والمحافظ (إنستاباي، فودافون كاش، كروت البنوك) المسجلة على هاتفك بنقرة واحدة مع منع التكرار تلقائياً:"
                } else {
                    "${greetingPrefix}You can scan and import past bank SMS messages (InstaPay, Vodafone Cash, Bank Cards) directly from your inbox with auto-deduplication:"
                },
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.SMS_IMPORT,
                        labelAr = "مزامنة رسائل البنك الآن",
                        labelEn = "Sync Bank SMS Messages"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST
            )
        }

        if (lower.contains("تعديل الميزانية") || lower.contains("اغير الميزانية") || lower.contains("سقف الميزانية") || lower.contains("edit budget") || lower.contains("set budget")) {
            return@withContext AiMessage(
                sender = MessageSender.ASSISTANT,
                text = if (isArabic) {
                    "${greetingPrefix}تستطيع تحديد وتعديل ميزانيتك الشهرية في أي وقت لحساب معدل الحرق اليومي وموعد نفاد الميزانية بدقة:"
                } else {
                    "${greetingPrefix}You can adjust your monthly spending budget anytime to track daily burn rate and depletion runway:"
                },
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.BUDGET_EDIT,
                        labelAr = "تعديل الميزانية الشهرية",
                        labelEn = "Edit Monthly Budget"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST
            )
        }

        if (lower.contains("تصدير") || lower.contains("اكسل") || lower.contains("excel") || lower.contains("csv") || lower.contains("export")) {
            return@withContext AiMessage(
                sender = MessageSender.ASSISTANT,
                text = if (isArabic) {
                    "${greetingPrefix}يمكنك تصدير تقريرك المالي كاملاً كملف Excel CSV بترميز UTF-8 سليم ومتوافق مع اللغة العربية، أو مشاركة ملخص سريع عبر تطبيقات الهاتف:"
                } else {
                    "${greetingPrefix}You can export all your financial transactions to a clean Excel CSV file with UTF-8 encoding, or share a formatted summary:"
                },
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.EXPORT_REPORT,
                        labelAr = "تصدير ومشاركة تقرير Excel CSV",
                        labelEn = "Export & Share Excel CSV"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST
            )
        }

        if (lower.contains("تحليلات") || lower.contains("analytics") || lower.contains("احصائيات") || lower.contains("رسم بياني")) {
            return@withContext AiMessage(
                sender = MessageSender.ASSISTANT,
                text = if (isArabic) {
                    "${greetingPrefix}شاشة التحليلات توضح لك تفصيل الصرف حسب كل فئة (أكل، مواصلات، فواتير) وقائمة المتاجر الأكثر إنفاقاً:"
                } else {
                    "${greetingPrefix}The Analytics tab provides a breakdown by category (Food, Transport, Bills) and your highest spend merchants:"
                },
                actions = listOf(
                    AiAction.Navigate(
                        target = NavigationTarget.ANALYTICS,
                        labelAr = "فتح شاشة التحليلات التفصيلية",
                        labelEn = "Open Detailed Analytics"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST
            )
        }

        // 2. Comprehensive Financial Safety Diagnostic Intent
        // Directly answers: "ميزانيتي في أمان؟", "هل ميزانيتي في أمان", "وضعي المالي", "هل صرفي في خطر", "is my budget safe"
        val isSafetyQuery = lower.contains("أمان") || lower.contains("امان") ||
                lower.contains("في خطر") || lower.contains("خطر") ||
                lower.contains("سليم") || lower.contains("سليمة") ||
                lower.contains("وضعي المالي") || lower.contains("وضع الميزانية") ||
                lower.contains("صرفي كويس") || lower.contains("safe") ||
                lower.contains("in danger") || lower.contains("healthy")

        if (isSafetyQuery) {
            val summary = getFinancialSummary()
            val cal = Calendar.getInstance()
            val currentDay = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
            val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val remainingDaysInMonth = (maxDaysInMonth - currentDay).coerceAtLeast(1)

            val budget = summary.monthlyBudget
            val spent = summary.totalExpenseMonth
            val remaining = summary.remainingBudget
            val dailyBurn = summary.dailyBurn

            val text = if (isArabic) {
                if (budget <= 0) {
                    "${greetingPrefix}لم تقم بتحديد ميزانية شهرية بعد. لتحديد ما إذا كانت ميزانيتك في أمان، يرجى تعيين ميزانية شهرية لمقارنة معدل حرقك اليومي بها."
                } else if (remaining <= 0 || spent >= budget) {
                    val overspent = String.format(Locale.US, "%,d", (spent - budget).toInt())
                    val spentStr = String.format(Locale.US, "%,d", spent.toInt())
                    val budStr = String.format(Locale.US, "%,d", budget.toInt())
                    "${greetingPrefix}تنبيه مالي عاجل: ميزانيتك الشهرية تجاوزت السقف المحدد وليست في أمان حالياً.\n\n" +
                            "• الميزانية المحددة: $budStr ج.م\n" +
                            "• إجمالي المصروفات: $spentStr ج.م\n" +
                            "• العجز الحالي: $overspent ج.م\n\n" +
                            "نصيحة مالية: ننصح بوقف أي مصاريف غير أساسية لبقية الشهر ($remainingDaysInMonth يوم) للحد من العجز."
                } else if (dailyBurn <= 0.0) {
                    val budStr = String.format(Locale.US, "%,d", budget.toInt())
                    "${greetingPrefix}نعم، ميزانيتك في أمان تام بنسبة 100% ومستقرة تماماً.\n\n" +
                            "ميزانيتك البالغة $budStr ج.م كاملة ولم يتم تسجيل أي مصاريف حتى الآن لهذا الشهر."
                } else {
                    val runwayDays = if (dailyBurn > 0) (remaining / dailyBurn).toInt() else 999
                    val allowableDailyForRestOfMonth = remaining / remainingDaysInMonth
                    val projectedTotalSpend = (dailyBurn * maxDaysInMonth)
                    val spentPct = ((spent / budget) * 100).toInt()
                    val monthPct = ((currentDay.toDouble() / maxDaysInMonth.toDouble()) * 100).toInt()

                    val spentStr = String.format(Locale.US, "%,d", spent.toInt())
                    val budStr = String.format(Locale.US, "%,d", budget.toInt())
                    val remStr = String.format(Locale.US, "%,d", remaining.toInt())
                    val burnStr = String.format(Locale.US, "%,d", dailyBurn.toInt())
                    val allowDailyStr = String.format(Locale.US, "%,d", allowableDailyForRestOfMonth.toInt())

                    if (dailyBurn <= allowableDailyForRestOfMonth) {
                        val projectedSurplus = String.format(Locale.US, "%,d", (budget - projectedTotalSpend).coerceAtLeast(0.0).toInt())
                        "${greetingPrefix}نعم، ميزانيتك في أمان تام ومستقرة جداً حتى الآن.\n\n" +
                                "تفاصيل وضعك المالي:\n" +
                                "• نسبة الصرف: أنفقت $spentStr ج.م ($spentPct%) بعد مرور $currentDay يوماً ($monthPct% من الشهر).\n" +
                                "• المتبقي الفعلي: $remStr ج.م من أصل $budStr ج.م يكفيك بمعدل صرفك الحالي لـ $runwayDays يوماً قادمة (المتبقي من الشهر $remainingDaysInMonth يوماً فقط).\n" +
                                "• معدل الحرق اليومي: تصرف حالياً $burnStr ج.م/يوم، في حين يمكنك صرف حتى $allowDailyStr ج.م/يوم لبقية الشهر دون تجاوز الميزانية.\n\n" +
                                "الخلاصة: أنت تسير بانضباط ممتاز ومن المتوقع أن تنهي الشهر بفائض مالي قدره $projectedSurplus ج.م تقريباً."
                    } else {
                        val projectedDeficit = String.format(Locale.US, "%,d", (projectedTotalSpend - budget).toInt())
                        val depletionDay = (currentDay + runwayDays).coerceAtMost(maxDaysInMonth)
                        "${greetingPrefix}تنبيه مالي: ميزانيتك تحت ضغط وتحتاج إلى ترشيد للوصول لبر الأمان.\n\n" +
                                "تحليل الموقف:\n" +
                                "• معدل الحرق الحالي: تصرف $burnStr ج.م/يوم، بينما الحد الآمن للأيام المتبقية هو $allowDailyStr ج.م/يوم فقط.\n" +
                                "• المتبقي من الميزانية: $remStr ج.م يكفيك لمدة $runwayDays يوماً فقط، ومن المتوقع نفاد الميزانية يوم $depletionDay من الشهر بعجز متوقع $projectedDeficit ج.م.\n\n" +
                                "نصيحة مالية: لكي تستعيد ميزانيتك أمانها، يُرجى خفض إنفاقك اليومي إلى أقل من $allowDailyStr ج.م/يوم خلال الـ $remainingDaysInMonth يوماً القادمة."
                    }
                }
            } else {
                if (budget <= 0) {
                    "${greetingPrefix}You haven't set a monthly budget yet. Set a budget to evaluate whether your spending pace is safe."
                } else if (remaining <= 0 || spent >= budget) {
                    val overspent = String.format(Locale.US, "%,d", (spent - budget).toInt())
                    val spentStr = String.format(Locale.US, "%,d", spent.toInt())
                    val budStr = String.format(Locale.US, "%,d", budget.toInt())
                    "${greetingPrefix}Urgent notice: Your monthly budget has exceeded its limit and is currently at risk.\n\n" +
                            "• Budget: $budStr EGP\n" +
                            "• Total Spent: $spentStr EGP\n" +
                            "• Deficit: $overspent EGP\n\n" +
                            "Advice: Pause non-essential expenses for the remaining $remainingDaysInMonth days of the month."
                } else if (dailyBurn <= 0.0) {
                    val budStr = String.format(Locale.US, "%,d", budget.toInt())
                    "${greetingPrefix}Yes, your budget is 100% safe and fully intact.\n\n" +
                            "Your full budget of $budStr EGP remains unspent this month."
                } else {
                    val runwayDays = if (dailyBurn > 0) (remaining / dailyBurn).toInt() else 999
                    val allowableDailyForRestOfMonth = remaining / remainingDaysInMonth
                    val projectedTotalSpend = (dailyBurn * maxDaysInMonth)
                    val spentPct = ((spent / budget) * 100).toInt()
                    val monthPct = ((currentDay.toDouble() / maxDaysInMonth.toDouble()) * 100).toInt()

                    val spentStr = String.format(Locale.US, "%,d", spent.toInt())
                    val budStr = String.format(Locale.US, "%,d", budget.toInt())
                    val remStr = String.format(Locale.US, "%,d", remaining.toInt())
                    val burnStr = String.format(Locale.US, "%,d", dailyBurn.toInt())
                    val allowDailyStr = String.format(Locale.US, "%,d", allowableDailyForRestOfMonth.toInt())

                    if (dailyBurn <= allowableDailyForRestOfMonth) {
                        val projectedSurplus = String.format(Locale.US, "%,d", (budget - projectedTotalSpend).coerceAtLeast(0.0).toInt())
                        "${greetingPrefix}Yes, your budget is completely safe and in excellent health.\n\n" +
                                "Financial Breakdown:\n" +
                                "• Spent: $spentStr EGP ($spentPct%) with $currentDay days passed ($monthPct% of the month).\n" +
                                "• Remaining: $remStr EGP out of $budStr EGP, covering $runwayDays days at current rate (only $remainingDaysInMonth days left in month).\n" +
                                "• Burn Rate: Currently spending $burnStr EGP/day vs safe allowable rate of $allowDailyStr EGP/day.\n\n" +
                                "Summary: You are managing finances very well and projected to end the month with ~$projectedSurplus EGP surplus."
                    } else {
                        val projectedDeficit = String.format(Locale.US, "%,d", (projectedTotalSpend - budget).toInt())
                        val depletionDay = (currentDay + runwayDays).coerceAtMost(maxDaysInMonth)
                        "${greetingPrefix}Caution: Your budget is under pressure and requires adjustment to stay safe.\n\n" +
                                "Risk Analysis:\n" +
                                "• Current burn rate: $burnStr EGP/day vs safe allowable rate of $allowDailyStr EGP/day.\n" +
                                "• Remaining budget: $remStr EGP will last only $runwayDays days, expected to run out on day $depletionDay with a projected deficit of $projectedDeficit EGP.\n\n" +
                                "Advice: Keep daily spending below $allowDailyStr EGP/day for the next $remainingDaysInMonth days."
                    }
                }
            }

            return@withContext AiMessage(
                sender = MessageSender.ASSISTANT,
                text = text,
                actions = listOf(
                    AiAction.ShowBudgetSnapshot(
                        monthlyBudget = summary.monthlyBudget,
                        totalSpent = summary.totalExpenseMonth,
                        remaining = summary.remainingBudget,
                        burnRate = summary.dailyBurn,
                        depletionDate = if (isArabic) "نهاية الشهر" else "End of Month"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST
            )
        }

        // 3. Exact Budget Balance & Status Query (e.g., "كم متبقي", "فاضل كام من الميزانية", "remaining balance")
        val isBalanceQuery = lower.contains("متبقي") || lower.contains("فاضل كام") || lower.contains("كم باقي") ||
                lower.contains("remaining budget") || lower.contains("budget status") ||
                lower.contains("رصيد الميزانية") || (lower.contains("ميزانية") && (lower.contains("كام") || lower.contains("كم") || lower.contains("باقي")))

        if (isBalanceQuery) {
            val summary = getFinancialSummary()
            val text = if (isArabic) {
                if (summary.monthlyBudget <= 0) {
                    "${greetingPrefix}لم تقم بتحديد ميزانية شهرية بعد. تحديد الميزانية يساعد في حساب معدل الحرق اليومي وتنبيهك قبل تجاوز سقف الإنفاق."
                } else {
                    val burnStr = String.format(Locale.US, "%,d", summary.dailyBurn.toInt())
                    val remStr = String.format(Locale.US, "%,d", summary.remainingBudget.toInt())
                    val spentStr = String.format(Locale.US, "%,d", summary.totalExpenseMonth.toInt())
                    val budStr = String.format(Locale.US, "%,d", summary.monthlyBudget.toInt())
                    "${greetingPrefix}ميزانيتك الشهرية: $budStr ج.م.\nصرفت منها حتى الآن: $spentStr ج.م.\nالمتبقي الفعلي: $remStr ج.م.\nمعدل الحرق اليومي: $burnStr ج.م/يوم."
                }
            } else {
                if (summary.monthlyBudget <= 0) {
                    "${greetingPrefix}You haven't configured a monthly budget yet. Setting one activates financial runway tracking and overspend warnings."
                } else {
                    val burnStr = String.format(Locale.US, "%,d", summary.dailyBurn.toInt())
                    val remStr = String.format(Locale.US, "%,d", summary.remainingBudget.toInt())
                    val spentStr = String.format(Locale.US, "%,d", summary.totalExpenseMonth.toInt())
                    val budStr = String.format(Locale.US, "%,d", summary.monthlyBudget.toInt())
                    "${greetingPrefix}Monthly Budget: $budStr EGP.\nSpent this month: $spentStr EGP.\nRemaining: $remStr EGP.\nDaily burn rate: $burnStr EGP/day."
                }
            }

            return@withContext AiMessage(
                sender = MessageSender.ASSISTANT,
                text = text,
                actions = listOf(
                    AiAction.ShowBudgetSnapshot(
                        monthlyBudget = summary.monthlyBudget,
                        totalSpent = summary.totalExpenseMonth,
                        remaining = summary.remainingBudget,
                        burnRate = summary.dailyBurn,
                        depletionDate = if (isArabic) "نهاية الشهر" else "End of Month"
                    )
                ),
                sourceEngine = AiEngineType.LOCAL_FAST
            )
        }

        // 3. What-if Simulation Intent: "لو اشتريت بـ 5000" or "if I buy for 5000"
        val simRegex = Regex("""(?:لو\s+(?:اشتريت|صرفت|دفعت)\s*(?:بـ?|مبلغ)?)?\s*([\d,]+)\s*(?:جنيه|جم|ج\.م|egp)?""", RegexOption.IGNORE_CASE)
        val simMatch = simRegex.find(lower)
        if (simMatch != null && (lower.contains("لو") || lower.contains("if i buy") || lower.contains("simulate") || lower.contains("اشترى") || lower.contains("اشتريت"))) {
            val amountStr = simMatch.groupValues[1].replace(",", "")
            val amount = amountStr.toDoubleOrNull()
            if (amount != null && amount > 0) {
                val summary = getFinancialSummary()
                val budget = summary.monthlyBudget
                val remainingNow = summary.remainingBudget
                val remainingAfter = remainingNow - amount

                val text = if (isArabic) {
                    val amtStr = String.format(Locale.US, "%,d", amount.toInt())
                    if (budget <= 0) {
                        "${greetingPrefix}لو صرفت $amtStr ج.م، سيضاف لمصاريفك الشهرية الحالية (${summary.totalExpenseMonth.toInt()} ج.م). يُفضل تحديد ميزانية لحساب الأثر بدقة."
                    } else if (remainingAfter < 0) {
                        val deficit = String.format(Locale.US, "%,d", (-remainingAfter).toInt())
                        "${greetingPrefix}تنبيه: لو اشتريت بمبلغ $amtStr ج.م ستتجاوز ميزانيتك الشهرية بعجز قدره $deficit ج.م. ننصح بتأجيل الشراء أو تقسيطه."
                    } else {
                        val remStr = String.format(Locale.US, "%,d", remainingAfter.toInt())
                        val pct = ((amount / budget) * 100).toInt()
                        "${greetingPrefix}لو اشتريت بمبلغ $amtStr ج.م (يمثل $pct% من ميزانيتك)، سيتبقى معك $remStr ج.م لباقي الشهر، ووضعك المالي ما زال آمناً."
                    }
                } else {
                    val amtStr = String.format(Locale.US, "%,d", amount.toInt())
                    if (budget <= 0) {
                        "${greetingPrefix}Spending $amtStr EGP will add to your total monthly spending. Configure a budget to analyze the impact precisely."
                    } else if (remainingAfter < 0) {
                        val deficit = String.format(Locale.US, "%,d", (-remainingAfter).toInt())
                        "${greetingPrefix}Notice: Spending $amtStr EGP will exceed your monthly budget cap by $deficit EGP. Consider deferring or splitting."
                    } else {
                        val remStr = String.format(Locale.US, "%,d", remainingAfter.toInt())
                        val pct = ((amount / budget) * 100).toInt()
                        "${greetingPrefix}Spending $amtStr EGP ($pct% of your budget) will leave you with $remStr EGP for the rest of the month. Your budget remains healthy."
                    }
                }

                return@withContext AiMessage(
                    sender = MessageSender.ASSISTANT,
                    text = text,
                    sourceEngine = AiEngineType.LOCAL_FAST
                )
            }
        }

        // 4. Top Merchants Intent
        if (lower.contains("محل") || lower.contains("تاجر") || lower.contains("merchant") || lower.contains("اكتر مكان") || lower.contains("top stores")) {
            val merchants = getTopMerchants(5)
            val text = if (isArabic) {
                if (merchants.isEmpty()) {
                    "${greetingPrefix}لم تسجل أي مصروفات حتى الآن لتحديد المتاجر الأكثر إنفاقاً."
                } else {
                    val top = merchants.first()
                    "${greetingPrefix}أكثر مكان صرفت عنده هو ${top.merchantName} بإجمالي ${String.format(Locale.US, "%,d", top.totalAmount.toInt())} ج.م في ${top.transactionCount} معاملات."
                }
            } else {
                if (merchants.isEmpty()) {
                    "${greetingPrefix}No expense transactions recorded yet to rank your merchants."
                } else {
                    val top = merchants.first()
                    "${greetingPrefix}Your highest spend merchant is ${top.merchantName} with ${String.format(Locale.US, "%,d", top.totalAmount.toInt())} EGP across ${top.transactionCount} transactions."
                }
            }

            return@withContext AiMessage(
                sender = MessageSender.ASSISTANT,
                text = text,
                actions = if (merchants.isNotEmpty()) listOf(AiAction.ShowTopMerchants(merchants)) else emptyList(),
                sourceEngine = AiEngineType.LOCAL_FAST
            )
        }

        // 5. Total Spending Summary Intent
        if (lower.contains("صرفت كام") || lower.contains("إجمالي المصاريف") || lower.contains("how much spent") || lower.contains("spending summary") || lower.contains("لخص مصاريف")) {
            val summary = getFinancialSummary()
            val text = if (isArabic) {
                val expStr = String.format(Locale.US, "%,d", summary.totalExpenseMonth.toInt())
                val incStr = String.format(Locale.US, "%,d", summary.totalIncomeMonth.toInt())
                val count = summary.transactionCountMonth
                "${greetingPrefix}ملخص هذا الشهر:\n• إجمالي المصروفات: $expStr ج.م\n• إجمالي الدخل: $incStr ج.م\n• عدد المعاملات: $count عملية.\n\nيمكنك السؤال عن أي متجر أو فئة محددة للمزيد من التفاصيل."
            } else {
                val expStr = String.format(Locale.US, "%,d", summary.totalExpenseMonth.toInt())
                val incStr = String.format(Locale.US, "%,d", summary.totalIncomeMonth.toInt())
                val count = summary.transactionCountMonth
                "${greetingPrefix}This Month's Summary:\n• Total Expenses: $expStr EGP\n• Total Income: $incStr EGP\n• Total Transactions: $count\n\nAsk me about any merchant or category for deeper insights."
            }

            return@withContext AiMessage(
                sender = MessageSender.ASSISTANT,
                text = text,
                sourceEngine = AiEngineType.LOCAL_FAST
            )
        }

        // Return null if it needs deep conversational reasoning from Gemini
        return@withContext null
    }
}
