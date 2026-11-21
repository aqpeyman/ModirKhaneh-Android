package ir.modirkhaneh.app

import ir.modirkhaneh.app.ai.BuildingAssistant
import ir.modirkhaneh.app.data.*
import org.junit.Assert.*
import org.junit.Test

class BuildingAssistantTest {
    private fun sample() = BuildingAssistant.Snapshot(
        units = listOf(
            BuildingUnitEntity(id = "u1", number = "1", occupantName = "علی رضایی", phone = "09120000001"),
            BuildingUnitEntity(id = "u2", number = "2", occupantName = "مینا احمدی")
        ),
        charges = listOf(ChargePeriodEntity(startMonthKey = 16861, endMonthKey = 16872, amountRial = 1_000_000)),
        payments = listOf(PaymentEntity(unitId = "u1", monthKey = 16869, amountRial = 400_000)),
        expenses = listOf(ExpenseEntity(title = "نظافت", category = "خدمات", amountRial = 250_000, monthKey = 16869))
    )

    @Test fun normalizesPersianDigitsAndLetters() {
        assertEquals("1405/08", BuildingAssistant.normalize("۱۴۰۵/۰۸").replace('ي','ی'))
        assertEquals("کیف", BuildingAssistant.normalize("كيف"))
    }

    @Test fun answersBuildingSummaryFromLocalSnapshot() {
        val result = BuildingAssistant.answer("خلاصه وضعیت ساختمان", sample())
        assertTrue(result.contains("واحدها: 2"))
        assertTrue(result.contains("جمع پرداخت‌ها"))
    }

    @Test fun calculatesUnitBalanceForMonth() {
        val result = BuildingAssistant.answer("بدهی واحد 1 در 1405/09 چقدر است؟", sample())
        assertTrue(result.contains("600,000") || result.contains("۶۰۰٬۰۰۰"))
        assertTrue(result.contains("تسویه" ).not())
    }

    @Test fun reportsNoChargeAsNotPayable() {
        val result = BuildingAssistant.answer("بدهی واحد 1 در 1406/01", sample())
        assertTrue(result.contains("شارژی تعریف نشده"))
    }

    @Test fun doesNotPretendToExecuteMutations() {
        val result = BuildingAssistant.answer("واحد 4 ثبت کن", sample())
        assertTrue(result.contains("ثبت یا ویرایش را فعلاً از فرم مربوط انجام دهید"))
    }

    @Test fun displaysEsfandAsMonthTwelveNotZero() {
        val result = BuildingAssistant.answer("شارژ ماه 1405/12 چقدر است؟", sample())
        assertTrue(result.contains("1405/12"))
        assertFalse(result.contains("1406/0"))
    }

    @Test fun reportsVacantUnits() {
        val data = sample().copy(units = sample().units + BuildingUnitEntity(id = "u3", number = "3", isVacant = true))
        val result = BuildingAssistant.answer("چند واحد خالی داریم؟", data)
        assertTrue(result.contains("تعداد واحدهای خالی: 1"))
        assertTrue(result.contains("واحد 3"))
    }

    @Test fun reportsRecordedNetFinancialPositionWithCaveat() {
        val result = BuildingAssistant.answer("تراز مالی ساختمان", sample())
        assertTrue(result.contains("خالص"))
        assertTrue(result.contains("موجودی واقعی صندوق نیست"))
    }

    @Test fun unknownQuestionsAreHandledHonestly() {
        val result = BuildingAssistant.answer("پیش‌بینی قیمت دلار فردا", sample())
        assertTrue(result.contains("پاسخ حدسی"))
    }
}
