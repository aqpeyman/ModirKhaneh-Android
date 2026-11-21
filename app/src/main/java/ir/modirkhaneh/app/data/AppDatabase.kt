package ir.modirkhaneh.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(
    entities = [BuildingUnitEntity::class, ChargePeriodEntity::class, PaymentEntity::class, ResidentHistoryEntity::class, ExpenseEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun unitDao(): UnitDao
    abstract fun chargeDao(): ChargeDao
    abstract fun paymentDao(): PaymentDao
    abstract fun residentHistoryDao(): ResidentHistoryDao
    abstract fun expenseDao(): ExpenseDao

    /** Executes a read/validate/write sequence atomically against the same SQLite transaction. */
    suspend fun registerPayment(unitId: String, monthKey: Int, amountRial: Long, note: String = ""): PaymentEntity = withTransaction {
        require(unitDao().findById(unitId) != null) { "واحد موردنظر پیدا نشد" }
        require(amountRial > 0) { "مبلغ پرداخت باید بیشتر از صفر باشد" }
        val charge = chargeDao().findForMonth(monthKey)
            ?: throw IllegalStateException("برای این ماه مبلغ شارژ ثبت نشده است؛ پرداخت مجاز نیست")
        val paid = paymentDao().totalPaid(unitId, monthKey)
        val remaining = ir.modirkhaneh.app.domain.PaymentRules.remaining(charge.amountRial, paid)
        ir.modirkhaneh.app.domain.PaymentRules.validatePayment(amountRial, remaining)
        val payment = PaymentEntity(unitId = unitId, monthKey = monthKey, amountRial = amountRial, note = note)
        paymentDao().insert(payment)
        payment
    }

    /** Validates non-overlap and inserts a charge period in one transaction. */
    suspend fun addChargePeriod(period: ChargePeriodEntity) = withTransaction {
        ir.modirkhaneh.app.domain.ChargeRules.validate(period, chargeDao().getAllOnce())
        chargeDao().insert(period)
    }

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "modirkhaneh.db").build().also { INSTANCE = it }
        }
    }
}
