package ir.modirkhaneh.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "building_units", indices = [Index(value = ["number"], unique = true)])
data class BuildingUnitEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val number: String,
    val occupantName: String = "",
    // Keep as String so leading zeroes are never lost.
    val phone: String = "",
    val residentType: String = "owner",
    val peopleCount: Int = 1,
    val isVacant: Boolean = false,
    val openingBalance: Long = 0,
    val openingBalancePaid: Long = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "charge_periods", indices = [Index(value = ["startMonthKey", "endMonthKey"])])
data class ChargePeriodEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val startMonthKey: Int,
    val endMonthKey: Int,
    val amountRial: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments", indices = [Index("unitId"), Index("monthKey")])
data class PaymentEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val unitId: String,
    val monthKey: Int,
    val amountRial: Long,
    val paidAt: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "resident_history", indices = [Index("unitId")])
data class ResidentHistoryEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val unitId: String,
    val unitNumberSnapshot: String,
    val occupantName: String,
    val phone: String,
    val residentType: String,
    val peopleCount: Int,
    val fromDate: String,
    val toDate: String,
    val paidTotalRial: Long,
    val balanceRial: Long,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses", indices = [Index("monthKey")])
data class ExpenseEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val amountRial: Long,
    val allocationMethod: String = "equal",
    val monthKey: Int,
    val createdAt: Long = System.currentTimeMillis()
)
