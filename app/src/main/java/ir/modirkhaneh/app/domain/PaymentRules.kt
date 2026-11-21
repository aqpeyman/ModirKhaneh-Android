package ir.modirkhaneh.app.domain

/** Pure domain guard; the database transaction must call this before inserting a payment. */
object PaymentRules {
    fun remaining(chargeAmount: Long, alreadyPaid: Long): Long {
        require(chargeAmount > 0) { "مبلغ شارژ باید بیشتر از صفر باشد" }
        require(alreadyPaid >= 0) { "مجموع پرداخت قبلی نامعتبر است" }
        return (chargeAmount - alreadyPaid).coerceAtLeast(0L)
    }

    fun validatePayment(amount: Long, remaining: Long) {
        require(amount > 0) { "مبلغ پرداخت باید بیشتر از صفر باشد" }
        require(remaining > 0) { "برای این ماه مانده قابل پرداخت وجود ندارد" }
        require(amount <= remaining) { "مبلغ پرداخت از مانده شارژ این ماه بیشتر است" }
    }
}
