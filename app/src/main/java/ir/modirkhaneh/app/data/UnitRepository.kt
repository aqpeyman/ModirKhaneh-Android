package ir.modirkhaneh.app.data

import kotlinx.coroutines.flow.Flow

class UnitRepository(private val db: AppDatabase) {
    fun observeUnits(): Flow<List<BuildingUnitEntity>> = db.unitDao().observeAll()
    fun observeCharges(): Flow<List<ChargePeriodEntity>> = db.chargeDao().observeAll()
    fun observePayments(): Flow<List<PaymentEntity>> = db.paymentDao().observeAll()
    fun observeExpenses(): Flow<List<ExpenseEntity>> = db.expenseDao().observeAll()
    suspend fun addUnit(unit: BuildingUnitEntity) = db.unitDao().insert(unit)
    suspend fun addCharge(period: ChargePeriodEntity) = db.addChargePeriod(period)
    suspend fun addPayment(unitId: String, monthKey: Int, amount: Long, note: String = "") = db.registerPayment(unitId, monthKey, amount, note)
    suspend fun addExpense(item: ExpenseEntity) {
        require(item.title.isNotBlank()) { "عنوان هزینه را وارد کنید" }
        require(item.amountRial > 0) { "مبلغ هزینه باید بیشتر از صفر باشد" }
        db.expenseDao().insert(item)
    }
}
