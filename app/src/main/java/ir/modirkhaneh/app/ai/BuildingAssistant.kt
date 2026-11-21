package ir.modirkhaneh.app.ai

import ir.modirkhaneh.app.data.BuildingUnitEntity
import ir.modirkhaneh.app.data.ChargePeriodEntity
import ir.modirkhaneh.app.data.ExpenseEntity
import ir.modirkhaneh.app.data.PaymentEntity
import java.text.NumberFormat
import java.util.Locale

/** Deterministic, fully-offline assistant for verified building data and common management questions.
 * It never changes financial records. Mutating actions must be confirmed through the app's forms.
 */
object BuildingAssistant {
    data class Snapshot(
        val units: List<BuildingUnitEntity>,
        val charges: List<ChargePeriodEntity>,
        val payments: List<PaymentEntity>,
        val expenses: List<ExpenseEntity>
    )

    fun normalize(raw: String): String = raw.trim()
        .replace('ي', 'ی').replace('ك', 'ک')
        .map { ch -> when (ch) {
            '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
            '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
            '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'
            '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
            else -> ch
        } }.joinToString("").lowercase(Locale.ROOT)

    private fun rial(value: Long): String = NumberFormat.getNumberInstance(Locale("fa", "IR")).format(value) + " ریال"
    private fun monthKey(text: String): Int? {
        val m = Regex("(13\\d{2}|14\\d{2}|15\\d{2})\\s*[/.-]\\s*(\\d{1,2})").find(text) ?: return null
        val y = m.groupValues[1].toIntOrNull() ?: return null
        val month = m.groupValues[2].toIntOrNull() ?: return null
        return if (month in 1..12) y * 12 + month else null
    }
    // Storage convention is year * 12 + month; decode without producing month 0 in Esfand.
    private fun displayMonth(key: Int): String = "${(key - 1) / 12}/${((key - 1) % 12) + 1}"
    private fun unitMention(text: String, units: List<BuildingUnitEntity>): BuildingUnitEntity? {
        val explicit = Regex("(?:واحد|طبقه)\\s*([\\w-]+)").find(text)?.groupValues?.get(1)
        return units.firstOrNull { u ->
            (explicit != null && normalize(u.number) == normalize(explicit)) ||
                (u.occupantName.isNotBlank() && text.contains(normalize(u.occupantName)))
        }
    }

    fun answer(question: String, data: Snapshot): String {
        val q = normalize(question)
        if (q.isBlank()) return "سؤالتان را بنویسید. می‌توانید درباره واحدها، ساکنین، شارژ، بدهی، پرداخت‌ها، هزینه‌ها و گزارش ساختمان بپرسید."

        if (listOf("راهنما", "چه کار", "چیکار", "امکانات", "کمک", "نمونه سوال", "چه سوال").any(q::contains)) return help()
        if (listOf("سلام", "درود", "صبح بخیر", "وقت بخیر").any(q::contains)) return "سلام! من دستیار آفلاین مدیرخانه هستم. اطلاعات ثبت‌شده همین دستگاه را بررسی می‌کنم و برای عملیات مالی، قبل از ثبت نهایی باید از فرم مربوط استفاده کنید. چه کمکی از من ساخته است؟"
        if (listOf("بدهکار", "بدهی", "مانده", "طلبکار").any(q::contains)) {
            val key = monthKey(q)
            if (key == null) return "برای محاسبه دقیق بدهی، ماه را هم بنویسید؛ مثلاً: بدهی واحد 3 در 1405/08 چقدر است؟"
            val charge = data.charges.firstOrNull { key in it.startMonthKey..it.endMonthKey }
                ?: return "برای این ماه هیچ شارژی تعریف نشده است؛ بنابراین بدهی شارژ برای این ماه قابل محاسبه نیست و پرداخت نیز نباید ثبت شود."
            val unit = unitMention(q, data.units)
            if (unit != null) {
                val paid = data.payments.filter { it.unitId == unit.id && it.monthKey == key }.sumOf { it.amountRial }
                val remaining = (charge.amountRial - paid).coerceAtLeast(0)
                return "وضعیت واحد ${unit.number} در ماه ${displayMonth(key)}:\nشارژ: ${rial(charge.amountRial)}\nپرداخت ثبت‌شده: ${rial(paid)}\nمانده: ${rial(remaining)}" + if (paid >= charge.amountRial) "\nوضعیت: تسویه‌شده" else "\nوضعیت: دارای مانده"
            }
            val rows = data.units.mapNotNull { u ->
                val paid = data.payments.filter { it.unitId == u.id && it.monthKey == key }.sumOf { it.amountRial }
                val remaining = (charge.amountRial - paid).coerceAtLeast(0)
                if (remaining > 0) "واحد ${u.number} (${u.occupantName.ifBlank { "بدون نام" }}): ${rial(remaining)}" else null
            }
            return if (rows.isEmpty()) "در ماه انتخاب‌شده، برای هیچ واحدی مانده شارژ ثبت نشده است."
                else "واحدهای دارای مانده در ماه ${displayMonth(key)}:\n" + rows.joinToString("\n") + "\nجمع مانده: ${rial(data.units.sumOf { u -> (charge.amountRial - data.payments.filter { it.unitId == u.id && it.monthKey == key }.sumOf { it.amountRial }).coerceAtLeast(0) })}"
        }
        if (listOf("خالی", "بدون ساکن", "واحد خالی").any(q::contains)) {
            val vacant = data.units.filter { it.isVacant || it.occupantName.isBlank() }
            return "تعداد واحدهای خالی: ${vacant.size} از ${data.units.size}\n" +
                (if (vacant.isEmpty()) "همه واحدهای ثبت‌شده دارای ساکن هستند." else vacant.joinToString("\n") { "واحد ${it.number}" })
        }
        if (listOf("واحدهای دارای ساکن", "واحدهای مسکونی", "تعداد ساکن", "چند نفر ساکن").any(q::contains)) {
            val occupied = data.units.filter { !it.isVacant && it.occupantName.isNotBlank() }
            return "واحدهای دارای ساکن: ${occupied.size}\nجمعیت ثبت‌شده: ${occupied.sumOf { it.peopleCount }} نفر\n" +
                occupied.joinToString("\n") { "واحد ${it.number}: ${it.occupantName} (${it.peopleCount} نفر)" }
        }
        if (listOf("چند واحد", "تعداد واحد", "واحدها", "لیست واحد").any(q::contains)) {
            if (data.units.isEmpty()) return "هنوز واحدی ثبت نشده است. از بخش «ساکنین» واحد جدید اضافه کنید."
            return "تعداد واحدهای ثبت‌شده: ${data.units.size}\n" + data.units.joinToString("\n") { "واحد ${it.number} · ${it.occupantName.ifBlank { "بدون نام" }} · ${if (it.phone.isBlank()) "تلفن ثبت نشده" else it.phone}" }
        }
        if (listOf("ساکن", "مالک", "مستاجر", "تلفن", "شماره تماس").any(q::contains)) {
            val u = unitMention(q, data.units) ?: return "شماره واحد یا نام ساکن را مشخص کنید؛ مثلاً: اطلاعات ساکن واحد 2 چیست؟"
            return "اطلاعات واحد ${u.number}:\nنام: ${u.occupantName.ifBlank { "ثبت نشده" }}\nتلفن: ${u.phone.ifBlank { "ثبت نشده" }}\nتعداد نفرات: ${u.peopleCount}\nنوع سکونت: ${if (u.residentType == "owner") "مالک" else "مستأجر"}"
        }
        if (listOf("جمع پرداخت", "کل پرداخت", "پرداخت ها", "پرداخت‌ها", "دریافتی").any(q::contains)) {
            val key = monthKey(q)
            val selected = if (key == null) data.payments else data.payments.filter { it.monthKey == key }
            val unit = unitMention(q, data.units)
            val scoped = if (unit == null) selected else selected.filter { it.unitId == unit.id }
            val period = if (key == null) "کل دوره" else "ماه ${displayMonth(key)}"
            val who = unit?.let { " واحد ${it.number}" } ?: ""
            return "گزارش پرداخت$who در $period:\nتعداد پرداخت‌ها: ${scoped.size}\nجمع مبالغ پرداختی: ${rial(scoped.sumOf { it.amountRial })}"
        }
        if (listOf("هزینه", "مخارج", "خرج").any(q::contains)) {
            val key = monthKey(q)
            val monthFiltered = if (key == null) data.expenses else data.expenses.filter { it.monthKey == key }
            val category = listOf("نظافت", "آسانسور", "آب", "برق", "گاز", "تعمیر", "نگهبانی").firstOrNull { q.contains(normalize(it)) }
            val selected = if (category == null) monthFiltered else monthFiltered.filter { normalize(it.category).contains(normalize(category)) || normalize(it.title).contains(normalize(category)) }
            val period = if (key == null) "کل دوره" else "ماه ${displayMonth(key)}"
            val categoryTitle = category?.let { " دسته $it" } ?: ""
            return "گزارش هزینه$categoryTitle $period:\nتعداد هزینه‌ها: ${selected.size}\nجمع هزینه‌ها: ${rial(selected.sumOf { it.amountRial })}\n" + selected.take(12).joinToString("\n") { "${it.title} (${it.category}): ${rial(it.amountRial)}" }
        }
        if (listOf("شارژ", "تعرفه").any(q::contains)) {
            val key = monthKey(q)
            if (key != null) {
                val c = data.charges.firstOrNull { key in it.startMonthKey..it.endMonthKey }
                return if (c == null) "برای ماه ${displayMonth(key)} شارژی تعریف نشده است."
                else "شارژ ماه ${displayMonth(key)} برای هر واحد: ${rial(c.amountRial)}"
            }
            return "تعداد بازه‌های شارژ ثبت‌شده: ${data.charges.size}\n" + data.charges.joinToString("\n") { "از ${it.startMonthKey} تا ${it.endMonthKey}: ${rial(it.amountRial)} در ماه" }
        }
        if (listOf("تراز مالی", "درآمد و هزینه", "مانده صندوق", "خالص مالی").any(q::contains)) {
            val income = data.payments.sumOf { it.amountRial }
            val expense = data.expenses.sumOf { it.amountRial }
            val net = income - expense
            return "تراز مالی ثبت‌شده کل دوره:\nدریافتی‌ها: ${rial(income)}\nهزینه‌ها: ${rial(expense)}\n" +
                "خالص (دریافتی منهای هزینه): ${rial(net)}\nتوجه: این عدد فقط بر اساس تراکنش‌های ثبت‌شده است و لزوماً معادل موجودی واقعی صندوق نیست."
        }
        if (listOf("گزارش", "خلاصه", "وضعیت ساختمان").any(q::contains))
            return "خلاصه اطلاعات ثبت‌شده ساختمان:\nواحدها: ${data.units.size}\nبازه‌های شارژ: ${data.charges.size}\nپرداخت‌ها: ${data.payments.size}\nجمع پرداخت‌ها: ${rial(data.payments.sumOf { it.amountRial })}\nهزینه‌ها: ${data.expenses.size}\nجمع هزینه‌ها: ${rial(data.expenses.sumOf { it.amountRial })}"
        if (listOf("تعمیر", "نگهداری", "خرابی", "تاسیسات", "تأسیسات").any(q::contains))
            return "برای مدیریت تعمیر و نگهداری، درخواست را با محل خرابی، شرح مشکل، زمان اعلام، اولویت و عکس/مدرک ثبت کنید. سپس مسئول پیگیری، زمان انجام، قطعات مصرفی و هزینه را ثبت و در پایان وضعیت را به «انجام‌شده» تغییر دهید. ماژول مستقل تعمیر و نگهداری در نقشه توسعه مدیرخانه قرار دارد و هنوز در این نسخه عملیاتی نیست."
        if (listOf("جلسه", "مجمع", "صورتجلسه", "تصمیم گیری").any(q::contains))
            return "برای صورت‌جلسه ساختمان، تاریخ و محل جلسه، حاضران، دستور جلسه، تصمیم‌ها، مسئول هر اقدام و مهلت انجام را ثبت کنید. این نسخه هنوز فرم مستقل جلسات و مصوبات ندارد."
        if (listOf("قطعی آب", "قطعی برق", "نشتی", "آسانسور", "آتش نشانی", "آتش‌نشانی").any(q::contains))
            return "این مورد نیازمند پیگیری ایمنی/تأسیسات است. موضوع را به مدیر یا مسئول فنی ساختمان اطلاع دهید؛ در شرایط خطر فوری با خدمات اضطراری محل تماس بگیرید. ثبت و پیگیری ساختاریافته این درخواست‌ها در ماژول نگهداری نسخه‌های بعدی پیش‌بینی می‌شود."
        if (listOf("ثبت کن", "اضافه کن", "پرداخت بزن", "تعریف کن", "حذف کن", "ویرایش کن").any(q::contains))
            return "درخواست شما شبیه یک عملیات تغییردهنده اطلاعات است. برای جلوگیری از ثبت اشتباه، این نسخه دستیار فقط اطلاعات را می‌خواند و محاسبه می‌کند؛ ثبت یا ویرایش را فعلاً از فرم مربوط انجام دهید. اجرای عملیات مستقیم با تأیید صریح در نسخه بعدی اضافه می‌شود."
        return "این پرسش در راهنمای آفلاین فعلی تعریف نشده است و نمی‌خواهم پاسخ حدسی بدهم. می‌توانید درباره تعداد واحدها، مشخصات ساکن، شارژ یک ماه، بدهی واحد، فهرست بدهکاران، جمع پرداخت‌ها، هزینه‌ها یا خلاصه ساختمان بپرسید. برای پرسش‌های عمومی‌تر، پس از اتصال مدل زبانی فارسی GGUF دامنه پاسخ‌گویی گسترده‌تر می‌شود."
    }

    fun help() = "من دستیار مدیریت ساختمان هستم و بدون اینترنت کار می‌کنم. نمونه پرسش‌ها:\n• چند واحد ثبت شده؟\n• اطلاعات ساکن واحد 3 چیست؟\n• شارژ ماه 1405/08 چقدر است؟\n• بدهی واحد 2 در 1405/08 چقدر است؟\n• فهرست بدهکاران 1405/08 را بده\n• جمع پرداخت‌ها در 1405/08 چقدر است؟\n• پرداخت‌های واحد 2 چقدر است؟\n• هزینه‌های ساختمان در 1405/08 چقدر شده؟\n• خلاصه وضعیت ساختمان را بده\n• برای خرابی آسانسور چه‌کار کنم؟\n• صورت‌جلسه ساختمان را چطور تنظیم کنم؟\n\nنکته: در این نسخه پاسخ‌ها بر اساس داده‌های محلی و قواعد مشخص تولید می‌شوند؛ هنوز مدل زبانی آزاد و اجرای فرمان‌های تغییردهنده اطلاعات متصل نشده است."
}
