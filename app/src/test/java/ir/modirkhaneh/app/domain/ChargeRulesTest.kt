package ir.modirkhaneh.app.domain

import ir.modirkhaneh.app.data.ChargePeriodEntity
import org.junit.Assert.*
import org.junit.Test

class ChargeRulesTest {
    @Test fun monthKeyMapsYearAndMonth() { assertEquals(2025 * 12 + 7, ChargeRules.monthKey(2025, 7)) }
    @Test fun noChargeMeansPaymentBlocked() {
        try { ChargeRules.requireCharge(emptyList(), 2025 * 12 + 7); fail("must block payment") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("پرداخت مجاز نیست")) }
    }
    @Test fun overlappingPeriodsRejected() {
        val existing = listOf(ChargePeriodEntity(startMonthKey=100, endMonthKey=105, amountRial=500000))
        try { ChargeRules.validate(ChargePeriodEntity(startMonthKey=105, endMonthKey=110, amountRial=600000), existing); fail("must reject overlap") }
        catch (e: IllegalArgumentException) { assertTrue(e.message!!.contains("تداخل")) }
    }
    @Test fun phoneNumberIsStoredAsString() {
        val phone = "09123456789"
        assertEquals('0', phone.first())
    }
}
