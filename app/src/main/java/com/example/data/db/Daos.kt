package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY roomNumber ASC")
    fun observeAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms ORDER BY roomNumber ASC")
    suspend fun getAllRooms(): List<RoomEntity>

    @Query("SELECT * FROM rooms WHERE roomNumber = :roomNumber")
    suspend fun getRoom(roomNumber: Int): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoomsIgnore(rooms: List<RoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomsReplace(rooms: List<RoomEntity>)

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Query("UPDATE rooms SET currentTenantId = NULL, lastMeterReading = 0.0")
    suspend fun resetAllRooms()
}

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants ORDER BY isActive DESC, currentRoomNumber ASC, name ASC")
    fun observeAllTenants(): Flow<List<TenantEntity>>

    @Query("SELECT * FROM tenants ORDER BY id ASC")
    suspend fun getAllTenants(): List<TenantEntity>

    @Query("SELECT * FROM tenants WHERE isActive = 1 ORDER BY currentRoomNumber ASC")
    suspend fun getActiveTenants(): List<TenantEntity>

    @Query("SELECT * FROM tenants WHERE id = :id")
    suspend fun getTenantById(id: Long): TenantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: TenantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenantsReplace(tenants: List<TenantEntity>)

    @Update
    suspend fun updateTenant(tenant: TenantEntity)

    @Query("DELETE FROM tenants WHERE id = :id")
    suspend fun deleteTenantById(id: Long)

    @Query("DELETE FROM tenants")
    suspend fun deleteAllTenants()
}

@Dao
interface RentDao {
    @Query("SELECT * FROM rent_entries ORDER BY dueDate DESC, id DESC")
    fun observeAllRentEntries(): Flow<List<RentEntryEntity>>

    @Query("SELECT * FROM rent_entries ORDER BY id ASC")
    suspend fun getAllRentEntries(): List<RentEntryEntity>

    @Query("SELECT * FROM rent_entries WHERE id = :id")
    suspend fun getRentById(id: Long): RentEntryEntity?

    @Query("SELECT * FROM rent_entries WHERE tenantId = :tenantId ORDER BY dueDate ASC, id ASC")
    suspend fun getRentEntriesForTenantAsc(tenantId: Long): List<RentEntryEntity>

    @Query("SELECT * FROM rent_entries WHERE tenantId = :tenantId AND pendingPaise > 0 ORDER BY dueDate ASC, id ASC")
    suspend fun getPendingRentForTenantAsc(tenantId: Long): List<RentEntryEntity>

    @Query("SELECT MAX(cycleIndex) FROM rent_entries WHERE tenantId = :tenantId")
    suspend fun getMaxCycleIndexForTenant(tenantId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRentIgnore(entry: RentEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRentEntriesReplace(entries: List<RentEntryEntity>)

    @Update
    suspend fun updateRentEntry(entry: RentEntryEntity)

    @Query("DELETE FROM rent_entries WHERE id = :id")
    suspend fun deleteRentById(id: Long)

    @Query("DELETE FROM rent_entries")
    suspend fun deleteAllRentEntries()

    // Suppressed rent cycles
    @Query("SELECT * FROM suppressed_rent_cycles")
    suspend fun getAllSuppressedCycles(): List<SuppressedRentCycleEntity>

    @Query("SELECT cycleIndex FROM suppressed_rent_cycles WHERE tenantId = :tenantId")
    suspend fun getSuppressedCyclesForTenant(tenantId: Long): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppressedCycle(cycle: SuppressedRentCycleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppressedCyclesReplace(cycles: List<SuppressedRentCycleEntity>)

    @Query("DELETE FROM suppressed_rent_cycles")
    suspend fun deleteAllSuppressedCycles()
}

@Dao
interface ElectricityDao {
    @Query("SELECT * FROM electricity_bills ORDER BY periodTo DESC, id DESC")
    fun observeAllBills(): Flow<List<ElectricityBillEntity>>

    @Query("SELECT * FROM electricity_bills ORDER BY id ASC")
    suspend fun getAllBills(): List<ElectricityBillEntity>

    @Query("SELECT * FROM electricity_bills WHERE id = :id")
    suspend fun getBillById(id: Long): ElectricityBillEntity?

    @Query("SELECT * FROM electricity_bills WHERE roomNumber = :roomNumber ORDER BY periodTo DESC, id DESC")
    suspend fun getBillsForRoomDesc(roomNumber: Int): List<ElectricityBillEntity>

    @Query("SELECT * FROM electricity_bills WHERE tenantId = :tenantId AND pendingPaise > 0 ORDER BY periodFrom ASC, id ASC")
    suspend fun getPendingBillsForTenantAsc(tenantId: Long): List<ElectricityBillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: ElectricityBillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillsReplace(bills: List<ElectricityBillEntity>)

    @Update
    suspend fun updateBill(bill: ElectricityBillEntity)

    @Query("DELETE FROM electricity_bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    @Query("DELETE FROM electricity_bills")
    suspend fun deleteAllBills()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payment_receipts ORDER BY paymentDate DESC, id DESC")
    fun observeAllReceipts(): Flow<List<PaymentReceiptEntity>>

    @Query("SELECT * FROM payment_receipts ORDER BY id ASC")
    suspend fun getAllReceipts(): List<PaymentReceiptEntity>

    @Query("SELECT * FROM payment_receipts WHERE id = :id")
    suspend fun getReceiptById(id: Long): PaymentReceiptEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceipt(receipt: PaymentReceiptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceiptsReplace(receipts: List<PaymentReceiptEntity>)

    @Update
    suspend fun updateReceipt(receipt: PaymentReceiptEntity)

    @Query("DELETE FROM payment_receipts WHERE id = :id")
    suspend fun deleteReceiptById(id: Long)

    @Query("DELETE FROM payment_receipts")
    suspend fun deleteAllReceipts()

    // Allocations
    @Query("SELECT * FROM payment_allocations ORDER BY id ASC")
    fun observeAllAllocations(): Flow<List<PaymentAllocationEntity>>

    @Query("SELECT * FROM payment_allocations ORDER BY id ASC")
    suspend fun getAllAllocations(): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocations WHERE paymentId = :paymentId ORDER BY id ASC")
    suspend fun getAllocationsForPayment(paymentId: Long): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocations WHERE targetType = :targetType AND targetId = :targetId ORDER BY id DESC")
    suspend fun getAllocationsForTargetDesc(targetType: String, targetId: Long): List<PaymentAllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: PaymentAllocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocationsReplace(allocations: List<PaymentAllocationEntity>)

    @Update
    suspend fun updateAllocation(allocation: PaymentAllocationEntity)

    @Query("DELETE FROM payment_allocations WHERE id = :id")
    suspend fun deleteAllocationById(id: Long)

    @Query("DELETE FROM payment_allocations WHERE paymentId = :paymentId")
    suspend fun deleteAllocationsForPayment(paymentId: Long)

    @Query("DELETE FROM payment_allocations")
    suspend fun deleteAllAllocations()
}

@Dao
interface AdvanceDao {
    @Query("SELECT * FROM advance_transactions ORDER BY transactionDate DESC, id DESC")
    fun observeAllAdvanceTransactions(): Flow<List<AdvanceTransactionEntity>>

    @Query("SELECT * FROM advance_transactions ORDER BY id ASC")
    suspend fun getAllAdvanceTransactions(): List<AdvanceTransactionEntity>

    @Query("SELECT * FROM advance_transactions WHERE tenantId = :tenantId AND type = 'ADJUSTED_TO_RENT' ORDER BY id DESC")
    suspend fun getAdjustedAdvanceForTenantDesc(tenantId: Long): List<AdvanceTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvanceTransaction(tx: AdvanceTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvanceTransactionsReplace(txs: List<AdvanceTransactionEntity>)

    @Update
    suspend fun updateAdvanceTransaction(tx: AdvanceTransactionEntity)

    @Query("DELETE FROM advance_transactions WHERE id = :id")
    suspend fun deleteAdvanceTransactionById(id: Long)

    @Query("DELETE FROM advance_transactions WHERE paymentId = :paymentId")
    suspend fun deleteAdvanceByPaymentId(paymentId: Long)

    @Query("DELETE FROM advance_transactions")
    suspend fun deleteAllAdvanceTransactions()
}

@Dao
interface WriteOffDao {
    @Query("SELECT * FROM write_offs ORDER BY writeOffDate DESC, id DESC")
    fun observeAllWriteOffs(): Flow<List<WriteOffEntity>>

    @Query("SELECT * FROM write_offs ORDER BY id ASC")
    suspend fun getAllWriteOffs(): List<WriteOffEntity>

    @Query("SELECT * FROM write_offs WHERE id = :id")
    suspend fun getWriteOffById(id: Long): WriteOffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWriteOff(writeOff: WriteOffEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWriteOffsReplace(writeOffs: List<WriteOffEntity>)

    @Update
    suspend fun updateWriteOff(writeOff: WriteOffEntity)

    @Query("DELETE FROM write_offs")
    suspend fun deleteAllWriteOffs()
}

@Dao
interface FollowUpDao {
    @Query("SELECT * FROM follow_ups ORDER BY status DESC, promisedPaymentDate ASC, id DESC")
    fun observeAllFollowUps(): Flow<List<FollowUpEntity>>

    @Query("SELECT * FROM follow_ups ORDER BY id ASC")
    suspend fun getAllFollowUps(): List<FollowUpEntity>

    @Query("SELECT * FROM follow_ups WHERE id = :id")
    suspend fun getFollowUpById(id: Long): FollowUpEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUp(followUp: FollowUpEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUpsReplace(followUps: List<FollowUpEntity>)

    @Update
    suspend fun updateFollowUp(followUp: FollowUpEntity)

    @Query("DELETE FROM follow_ups WHERE id = :id")
    suspend fun deleteFollowUpById(id: Long)

    @Query("DELETE FROM follow_ups")
    suspend fun deleteAllFollowUps()
}

@Dao
interface RoomTransferDao {
    @Query("SELECT * FROM room_transfers ORDER BY transferDate DESC, id DESC")
    fun observeAllTransfers(): Flow<List<RoomTransferEntity>>

    @Query("SELECT * FROM room_transfers ORDER BY id ASC")
    suspend fun getAllTransfers(): List<RoomTransferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: RoomTransferEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfersReplace(transfers: List<RoomTransferEntity>)

    @Query("DELETE FROM room_transfers")
    suspend fun deleteAllTransfers()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)
}
