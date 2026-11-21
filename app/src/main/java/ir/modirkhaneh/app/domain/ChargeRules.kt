package ir.modirkhaneh.app.domain

import ir.modirkhaneh.app.data.ChargePeriodEntity

/** Gregorian-independent month key: year * 12 + month (month is 1..12). */
object ChargeRules {
    fun monthKey(year: Int, month: Int): Int {
        require(month in 1..12) { "ماه باید بین ۱ تا ۱۲ باشد" }
        return year * 12 + month
    }

    fun validate(period: ChargePeriodEntity, existing: List<ChargePeriodEntity>) {
        require(period.startMonthKey > 0 && period.endMonthKey >= period.startMonthKey) { "بازه شارژ معتبر نیست" }
        require(period.amountRial > 0) { "مبلغ شارژ باید بیشتر از صفر باشد" }
        val overlaps = existing.any { old -> period.startMonthKey <= old.endMonthKey && period.endMonthKey >= old.startMonthKey }
        require(!overlaps) { "بازه شارژ با یک بازه ثبت‌شده تداخل دارد" }
    }

    fun requireCharge(periods: List<ChargePeriodEntity>, monthKey: Int): ChargePeriodEntity {
        return periods.firstOrNull { monthKey in it.startMonthKey..it.endMonthKey }
            ?: throw IllegalStateException("برای این ماه مبلغ شارژ ثبت نشده است؛ پرداخت مجاز نیست")
    }
}
