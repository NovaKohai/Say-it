# خطة تنفيذ: تعديل بيانات المستخدم عبر الذكاء الاصطناعي مع ميزة الأمان والتأكيد (plan_ai.md)

تحويل المساعد المالي (AI Copilot) إلى **وكيل مالي ذاتي (Autonomous Financial Agent)** يستطيع فهم طلبات المستخدم وتنفيذها مباشرة داخل التطبيق (قاعدة بيانات SQLite وتفضيلات المستخدم SharedPreferences)، مع وجود **طبقة أمان تفاعلية (Safety & Confirmation Guard)** للعمليات المالية والحذف لمنع أي تعديل خاطئ.

---

## 🛡️ بنية ميزة الأمان (Safety Architecture)

لتحقيق أقصى درجات السلاسة مع ضمان الأمان المالي، سيتم تقسيم العمليات إلى مستويين:

1. **العمليات الآمنة (Safe Immediate Execution):**
   * **تعديل الاسم المفضل (`userPreferredName`)**: مثل *"خلّي اسمي عمر"* ⬅️ ينفذ فوراً في `SayItPreferences` دون إزعاج المستخدم بنوافذ تأكيد.
   * **تعديل الميزانية الشهرية (`monthlyBudget`)**: مثل *"خلّي ميزانيتي 20 ألف"* ⬅️ يحدث في التفضيلات فوراً ويخبر الـ AI المستخدم بالرصيد المتبقي بعد التعديل.

2. **العمليات المالية والحساسة (Safety-Guarded Operations with Interactive Cards):**
   * **تسجيل مصاريف أو دخل جديد (`ADD_TX`)**: مثل *"سجل إني صرفت 250 جنيه في ماكدونالدز"* ⬅️ بدلاً من الحفظ العشوائي بدون علم المستخدم بتفاصيل التصنيف والمحفظة، يُخرج الـ AI رد توضيحي مع **كارت تأكيد تفاعلي (Interactive Confirmation Card)** داخل الشات يوضح:
     * المبلغ بخط عريض (مثلاً: `250 ج.م`).
     * اسم التاجر والتصنيف التلقائي المناسب (مثلاً: `طعام ومشروبات`).
     * زرين بفيزياء Emil Kowalski اللمسية: **"تأكيد وحفظ ✅"** و **"إلغاء ❌"**.
     * بمجرد الضغط على "تأكيد"، يتم الحفظ في SQLite فوراً وتتحول البطاقة لحالة "تم الحفظ بنجاح".
   * **حذف المعاملات (`DELETE_TX`)**: مثل *"احذف معاملة كارفور"* ⬅️ يعرض كارت تأكيد حذف صريح يمنع فقدان البيانات بالخطأ.
   * **سداد الأقساط (`PAY_INSTALLMENT`)**: مثل *"دفعت قسط الآيفون بتاع الشهر ده"* ⬅️ يعرض كارت تأكيد لتحديث سجل القسط إلى `PAID`.

---

## 📐 التغييرات المقترحة بالملفات (Proposed Changes)

### 1. طبقة الـ Domain والنماذج (Domain Layer)

#### [MODIFY] [AiAction.kt](file:///f:/Projects/Say%20it/app/src/main/java/com/example/sayit/domain/model/AiAction.kt)
* ترقية وتوسيع كلاس `AiAction.ConfirmTransaction` ليشمل:
  * `id`: معرف فريد لكل كارت تفاعلي لمنع التكرار.
  * `status`: حالة الإجراء (`PENDING`, `CONFIRMED`, `CANCELLED`).
  * `amount`: المبلغ.
  * `type`: نوع المعاملة (`EXPENSE` أو `INCOME`).
  * `merchant`: اسم المتجر أو جهة الصرف.
  * `categoryId`: معرّف التصنيف المقترح.
  * `categoryName`: اسم التصنيف العربي/الإنجليزي.
  * `paymentSource`: وسيلة الدفع (`CASH`, `INSTAPAY`, `WALLET`, إلخ).
* إضافة `AiAction.ConfirmDeleteTransaction`:
  * `transactionId`: معرّف المعاملة المراد حذفها.
  * `merchant`: اسم المتجر.
  * `amount`: المبلغ.
  * `status`: حالة الحذف (`PENDING`, `CONFIRMED`, `CANCELLED`).

---

### 2. طبقة مستودع الذكاء الاصطناعي (Repository Layer)

#### [MODIFY] [AiCopilotRepositoryImpl.kt](file:///f:/Projects/Say%20it/app/src/main/java/com/example/sayit/data/repository/AiCopilotRepositoryImpl.kt)
* **تحديث الـ System Prompt**:
  * تعليم الموديل بروتوكول الأوامر التنفيذية:
    * `[EXEC:SET_NAME:<name>]` لتعديل اسم المستخدم.
    * `[EXEC:SET_BUDGET:<amount>]` لتعديل الميزانية.
    * `[PROPOSE:ADD_TX:{"amount":250,"merchant":"McDonalds","category":"cat_food","type":"EXPENSE","source":"CASH"}]` لاقتراح معاملة مالية وتوليد كارت تأكيد.
    * `[PROPOSE:DELETE_TX:{"id":"<tx_id>","merchant":"Carrefour","amount":850}]` لاقتراح حذف معاملة.
* **تطوير دالة `extractActionsFromResponse`**:
  * فحص وسوم `[EXEC:SET_NAME:...]` وحفظ الاسم في `preferences.userPreferredName` فوراً.
  * فحص وسوم `[EXEC:SET_BUDGET:...]` وتحديث `preferences.monthlyBudget` فوراً وإطلاق إشعار التحديث في قاعدة البيانات.
  * فحص وسوم `[PROPOSE:ADD_TX:...]` وتحويلها لـ `AiAction.ConfirmTransaction(status = PENDING)` وعرضها ككارت تأكيد في الشات.
  * تنظيف نص الرد الموجه للمستخدم من أي وسوم برمجية ليظل النص طبيعياً وجذاباً بالمصري الصرف.

---

### 3. طبقة إدارة الحالة (ViewModel Layer)

#### [MODIFY] [AiCopilotViewModel.kt](file:///f:/Projects/Say%20it/app/src/main/java/com/example/sayit/presentation/ai/AiCopilotViewModel.kt)
* إضافة حقن لمستودع المعاملات `TransactionRepository` وقاعدة البيانات `SayItDatabase`.
* إضافة دالة `onConfirmTransaction(action: AiAction.ConfirmTransaction, isArabic: Boolean)`:
  * تنشئ كائن `Transaction` حقيقي وتحفظه في قاعدة البيانات `database.insertTransaction(...)`.
  * تحدث حالة الكارت في قائمة الرسائل إلى `CONFIRMED`.
  * تضيف رسالة استجابة خفيفة من المساعد: *"تمام يا بطل! سجلتلك المعاملة في حساباتك وخصمتها من الميزانية ✅"*.
* إضافة دالة `onCancelAction(actionId: String, isArabic: Boolean)`:
  * تحدث حالة الكارت إلى `CANCELLED` لمنع التفاعل معه مجدداً.
* إضافة دالة `onConfirmDeleteTransaction(action: AiAction.ConfirmDeleteTransaction, isArabic: Boolean)`:
  * تحذف المعاملة من SQLite عبر `database.deleteTransaction(action.transactionId)`.
  * تطلق `notifyTxChanged()` لتحديث كل شاشات التطبيق في نفس اللحظة.

---

### 4. طبقة واجهة المستخدم (UI & Presentation Layer)

#### [MODIFY] [RichCopilotWidgets.kt](file:///f:/Projects/Say%20it/app/src/main/java/com/example/sayit/presentation/ai/components/RichCopilotWidgets.kt)
* تصميم وبرمجة **كارت تأكيد المعاملة التفاعلي (`TransactionConfirmationCard`)**:
  * تصميم عصري FinTech زجاجي أنيق بحدود متوهجة خفيفة.
  * أيقونة الفئة ولونها المميز.
  * تفاصيل المعاملة: المبلغ، التاجر، التصنيف، طريقة الدفع.
  * في حالة `PENDING`: زرين بحجم لمسي مريح وتأثير `pressScale(0.96f)` واهتزاز حسي:
    * زر تأكيد أخضر زمردي (`Emerald600`) مكتوب عليه "تأكيد وتسجيل".
    * زر تراجع رمادي أنيق مكتوب عليه "إلغاء".
  * في حالة `CONFIRMED`: إظهار شارة خضراء أنيقة "تم التسجيل في حساباتك بنجاح ✅".
  * في حالة `CANCELLED`: إظهار شارة رمادية "تم الإلغاء ❌".

#### [MODIFY] [AiCopilotScreen.kt](file:///f:/Projects/Say%20it/app/src/main/java/com/example/sayit/presentation/ai/AiCopilotScreen.kt)
* ربط كارت التأكيد في فقاعة رسالة المساعد `ChatMessageBubble` ليمرر ضغطات المستخدم إلى ViewModel.

---

## 🧪 خطة التحقق والاختبار (Verification Plan)

### Automated & Build Verification
1. التأكد من نجاح البناء:
   `./gradlew assembleDebug`
2. فحص سلامة استخراج الـ JSON والوسوم من ردود الذكاء الاصطناعي.

### Live Emulator Verification
1. **اختبار تعديل الاسم والميزانية**:
   * إرسال: *"خلّي اسمي عمر"* و *"عدل ميزانيتي الشهرية لـ 20 ألف جنيه"* والتأكد من التحديث الفوري المباشر.
2. **اختبار كارت تأكيد المعاملة المالية**:
   * إرسال: *"سجل إني دفعت 150 جنيه في ستاربكس كاش"*.
   * التحقق من ظهور كارت الأمان بكامل التفاصيل وزري (تأكيد / إلغاء).
   * الضغط على "تأكيد وتسجيل" والتأكد من حفظ المعاملة فوراً في قاعدة البيانات SQLite وتحديث الشاشة الرئيسية.
