package ir.modirkhaneh.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface UnitDao {
    @Query("SELECT * FROM building_units ORDER BY number") fun observeAll(): Flow<List<BuildingUnitEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(unit: BuildingUnitEntity)
    @Update suspend fun update(unit: BuildingUnitEntity)
    @Query("SELECT * FROM building_units WHERE id = :id LIMIT 1") suspend fun findById(id: String): BuildingUnitEntity?
}

@Dao interface ChargeDao {
    @Query("SELECT * FROM charge_periods WHERE startMonthKey <= :monthKey AND endMonthKey >= :monthKey LIMIT 1") suspend fun findForMonth(monthKey: Int): ChargePeriodEntity?
    @Query("SELECT * FROM charge_periods ORDER BY startMonthKey DESC") fun observeAll(): Flow<List<ChargePeriodEntity>>
    @Query("SELECT * FROM charge_periods ORDER BY startMonthKey") suspend fun getAllOnce(): List<ChargePeriodEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(period: ChargePeriodEntity)
}

@Dao interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paidAt DESC") fun observeAll(): Flow<List<PaymentEntity>>
    @Query("SELECT * FROM payments WHERE unitId = :unitId ORDER BY paidAt DESC") fun observeForUnit(unitId: String): Flow<List<PaymentEntity>>
    @Query("SELECT COALESCE(SUM(amountRial), 0) FROM payments WHERE unitId = :unitId AND monthKey = :monthKey") suspend fun totalPaid(unitId: String, monthKey: Int): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(payment: PaymentEntity)
}

@Dao interface ResidentHistoryDao {
    @Query("SELECT * FROM resident_history WHERE unitId = :unitId ORDER BY savedAt DESC") fun observeForUnit(unitId: String): Flow<List<ResidentHistoryEntity>>
    @Insert suspend fun insert(item: ResidentHistoryEntity)
}

@Dao interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC") fun observeAll(): Flow<List<ExpenseEntity>>
    @Insert suspend fun insert(item: ExpenseEntity)
}
