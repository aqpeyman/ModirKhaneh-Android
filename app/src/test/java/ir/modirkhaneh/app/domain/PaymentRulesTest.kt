package ir.modirkhaneh.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PaymentRulesTest {
    @Test fun partialPaymentLeavesBalance() { assertEquals(650L, PaymentRules.remaining(1000L, 350L)) }
    @Test fun fullyPaidHasNoRemainingBalance() { assertEquals(0L, PaymentRules.remaining(1000L, 1200L)) }
    @Test fun rejectsZeroPayment() { assertThrows(IllegalArgumentException::class.java) { PaymentRules.validatePayment(0L, 500L) } }
    @Test fun rejectsOverpayment() { assertThrows(IllegalArgumentException::class.java) { PaymentRules.validatePayment(501L, 500L) } }
    @Test fun rejectsPaymentWithoutBalance() { assertThrows(IllegalArgumentException::class.java) { PaymentRules.validatePayment(1L, 0L) } }
}
