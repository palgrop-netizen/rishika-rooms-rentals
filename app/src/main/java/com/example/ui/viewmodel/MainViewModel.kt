package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AdvanceTransactionEntity
import com.example.data.db.AppDatabase
import com.example.data.db.AppSettingsEntity
import com.example.data.db.BackupPayload
import com.example.data.db.ElectricityBillEntity
import com.example.data.db.FollowUpEntity
import com.example.data.db.PaymentAllocationEntity
import com.example.data.db.PaymentReceiptEntity
import com.example.data.db.RentEntryEntity
import com.example.data.db.RoomEntity
import com.example.data.db.RoomTransferEntity
import com.example.data.db.TenantEntity
import com.example.data.db.WriteOffEntity
import com.example.data.repository.PaymentAllocationPreview
import com.example.data.repository.RentalRepository
import com.example.util.DateUtils
import com.example.util.KycFileHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RoomStatus {
    CLEAR,
    DUE,
    VACANT
}

data class RoomTileUi(
    val room: RoomEntity,
    val tenant: TenantEntity?,
    val rentPendingPaise: Long,
    val electricityPendingPaise: Long,
    val totalDuePaise: Long,
    val status: RoomStatus
)

data class DashboardSummary(
    val totalRooms: Int = AppDatabase.TOTAL_ROOMS,
    val occupiedRooms: Int = 0,
    val vacantRooms: Int = AppDatabase.TOTAL_ROOMS,
    val totalRentReceivedPaise: Long = 0L,
    val totalRentPendingPaise: Long = 0L,
    val totalElectricityPendingPaise: Long = 0L,
    val totalAmountCollectedPaise: Long = 0L,
    val totalAdvanceBalancePaise: Long = 0L
)

data class RrrUiState(
    val isReady: Boolean = false,
    val roomTiles: List<RoomTileUi> = emptyList(),
    val summary: DashboardSummary = DashboardSummary(),
    val tenants: List<TenantEntity> = emptyList(),
    val rentEntries: List<RentEntryEntity> = emptyList(),
    val electricityBills: List<ElectricityBillEntity> = emptyList(),
    val receipts: List<PaymentReceiptEntity> = emptyList(),
    val allocations: List<PaymentAllocationEntity> = emptyList(),
    val advanceTransactions: List<AdvanceTransactionEntity> = emptyList(),
    val writeOffs: List<WriteOffEntity> = emptyList(),
    val followUps: List<FollowUpEntity> = emptyList(),
    val transfers: List<RoomTransferEntity> = emptyList(),
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val overdueFollowUps: List<FollowUpEntity> = emptyList(),
    val showBackupReminder: Boolean = false
)

sealed class UiEvent {
    data class Success(val message: String) : UiEvent()
    data class Error(val message: String) : UiEvent()
}

private data class CoreBundle(
    val rooms: List<RoomEntity>,
    val tenants: List<TenantEntity>,
    val rents: List<RentEntryEntity>,
    val bills: List<ElectricityBillEntity>,
    val receipts: List<PaymentReceiptEntity>
)

private data class LedgerBundle(
    val allocations: List<PaymentAllocationEntity>,
    val advanceTxs: List<AdvanceTransactionEntity>,
    val writeOffs: List<WriteOffEntity>,
    val followUps: List<FollowUpEntity>,
    val transfers: List<RoomTransferEntity>,
    val settings: AppSettingsEntity?
)

class MainViewModel(
    application: Application,
    private val customRepository: RentalRepository? = null
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, null)

    private val repository: RentalRepository = customRepository
        ?: RentalRepository(AppDatabase.getInstance(application))

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private val _initialized = MutableStateFlow(false)

    private val coreFlow = combine(
        repository.roomsFlow,
        repository.tenantsFlow,
        repository.rentEntriesFlow,
        repository.electricityBillsFlow,
        repository.receiptsFlow
    ) { rooms, tenants, rents, bills, receipts ->
        CoreBundle(rooms, tenants, rents, bills, receipts)
    }

    private val ledgerFlow = combine(
        repository.allocationsFlow,
        repository.advanceTransactionsFlow,
        repository.writeOffsFlow,
        repository.followUpsFlow,
        repository.transfersFlow
    ) { allocations, advTxs, writeOffs, followUps, transfers ->
        LedgerBundle(allocations, advTxs, writeOffs, followUps, transfers, null)
    }.combine(repository.settingsFlow) { bundle, settings ->
        bundle.copy(settings = settings)
    }

    val uiState: StateFlow<RrrUiState> = combine(
        _initialized,
        coreFlow,
        ledgerFlow
    ) { initialized, core, ledger ->
        val settings = ledger.settings ?: AppSettingsEntity()
        val tenantMap = core.tenants.associateBy { it.id }
        val rentPendingByTenant = core.rents.groupBy { it.tenantId }
            .mapValues { (_, list) -> list.sumOf { it.pendingPaise } }
        val elecPendingByTenant = core.bills.groupBy { it.tenantId }
            .mapValues { (_, list) -> list.sumOf { it.pendingPaise } }

        val tiles = core.rooms.sortedBy { it.roomNumber }.map { room ->
            val activeTenant = room.currentTenantId?.let { tenantMap[it] }?.takeIf { it.isActive }
            if (activeTenant == null) {
                RoomTileUi(
                    room = room,
                    tenant = null,
                    rentPendingPaise = 0L,
                    electricityPendingPaise = 0L,
                    totalDuePaise = 0L,
                    status = RoomStatus.VACANT
                )
            } else {
                val rPend = rentPendingByTenant[activeTenant.id] ?: 0L
                val ePend = elecPendingByTenant[activeTenant.id] ?: 0L
                val totalDue = rPend + ePend
                val status = if (rPend == 0L && ePend == 0L) RoomStatus.CLEAR else RoomStatus.DUE
                RoomTileUi(
                    room = room,
                    tenant = activeTenant,
                    rentPendingPaise = rPend,
                    electricityPendingPaise = ePend,
                    totalDuePaise = totalDue,
                    status = status
                )
            }
        }

        val occupied = tiles.count { it.tenant != null }
        val summary = DashboardSummary(
            totalRooms = tiles.size.coerceAtLeast(AppDatabase.TOTAL_ROOMS),
            occupiedRooms = occupied,
            vacantRooms = (tiles.size - occupied).coerceAtLeast(0),
            totalRentReceivedPaise = core.rents.sumOf { it.amountPaidPaise + it.advanceAppliedPaise },
            totalRentPendingPaise = core.rents.sumOf { it.pendingPaise },
            totalElectricityPendingPaise = core.bills.sumOf { it.pendingPaise },
            totalAmountCollectedPaise = core.receipts.sumOf { it.totalAmountPaise },
            totalAdvanceBalancePaise = core.tenants.sumOf { it.advanceBalancePaise }
        )

        val today = DateUtils.todayIso()
        val overdue = ledger.followUps.filter {
            it.status == "PENDING" && it.promisedPaymentDate <= today
        }

        val hasAnyData = core.tenants.isNotEmpty() || core.receipts.isNotEmpty()
        val daysSinceBackup = DateUtils.epochDaysSince(settings.lastBackupEpochMs)
        val showBackupReminder = hasAnyData && daysSinceBackup >= settings.backupReminderDays

        RrrUiState(
            isReady = initialized,
            roomTiles = tiles,
            summary = summary,
            tenants = core.tenants,
            rentEntries = core.rents,
            electricityBills = core.bills,
            receipts = core.receipts,
            allocations = ledger.allocations,
            advanceTransactions = ledger.advanceTxs,
            writeOffs = ledger.writeOffs,
            followUps = ledger.followUps,
            transfers = ledger.transfers,
            settings = settings,
            overdueFollowUps = overdue,
            showBackupReminder = showBackupReminder
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = RrrUiState()
    )

    init {
        viewModelScope.launch {
            runCatching {
                repository.ensureInitialized()
            }.onFailure { err ->
                _events.tryEmit(UiEvent.Error("Startup check failed: ${err.message}"))
            }
            _initialized.value = true
        }
    }

    fun generateDueRent() {
        viewModelScope.launch {
            repository.generateDueRentForAllActiveTenants()
                .onSuccess { count ->
                    val msg = if (count > 0) {
                        "Generated $count due rent ${if (count == 1) "entry" else "entries"}."
                    } else {
                        "All active tenants are up to date."
                    }
                    _events.emit(UiEvent.Success(msg))
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to generate rent.")) }
        }
    }

    fun addTenant(
        name: String,
        mobileNumber: String,
        roomNumber: Int,
        monthlyRentPaise: Long,
        entryDate: String,
        kycType: String,
        kycNumber: String,
        kycDocumentPath: String?,
        kycDocumentName: String?,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.addTenant(
                name, mobileNumber, roomNumber, monthlyRentPaise, entryDate,
                kycType, kycNumber, kycDocumentPath, kycDocumentName, notes
            ).onSuccess {
                _events.emit(UiEvent.Success("Tenant added to Room $roomNumber."))
                onSuccess()
            }.onFailure {
                _events.emit(UiEvent.Error(it.message ?: "Failed to add tenant."))
            }
        }
    }

    fun editTenant(
        tenantId: Long,
        name: String,
        mobileNumber: String,
        monthlyRentPaise: Long,
        entryDate: String,
        kycType: String,
        kycNumber: String,
        kycDocumentPath: String?,
        kycDocumentName: String?,
        notes: String,
        oldKycPathToClean: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.editTenant(
                tenantId, name, mobileNumber, monthlyRentPaise, entryDate,
                kycType, kycNumber, kycDocumentPath, kycDocumentName, notes
            ).onSuccess {
                if (oldKycPathToClean != null && oldKycPathToClean != kycDocumentPath) {
                    KycFileHelper.deleteFileIfExists(oldKycPathToClean)
                }
                _events.emit(UiEvent.Success("Tenant details updated."))
                onSuccess()
            }.onFailure {
                _events.emit(UiEvent.Error(it.message ?: "Failed to update tenant."))
            }
        }
    }

    fun transferTenant(
        tenantId: Long,
        toRoomNumber: Int,
        transferDate: String,
        remarks: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.transferTenant(tenantId, toRoomNumber, transferDate, remarks)
                .onSuccess {
                    _events.emit(UiEvent.Success("Tenant transferred to Room $toRoomNumber."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to transfer tenant.")) }
        }
    }

    fun vacateTenant(tenantId: Long, vacatedDate: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.vacateTenant(tenantId, vacatedDate)
                .onSuccess {
                    _events.emit(UiEvent.Success("Tenant vacated. Room is now vacant and history preserved."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to vacate tenant.")) }
        }
    }

    fun deleteTenant(tenantId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTenant(tenantId)
                .onSuccess { kycPath ->
                    KycFileHelper.deleteFileIfExists(kycPath)
                    _events.emit(UiEvent.Success("Tenant and linked records deleted."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to delete tenant.")) }
        }
    }

    fun addManualRentEntry(
        tenantId: Long,
        dueDateIso: String,
        rentAmountPaise: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.addManualRentEntry(tenantId, dueDateIso, rentAmountPaise)
                .onSuccess {
                    _events.emit(UiEvent.Success("Rent entry added."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to add rent entry.")) }
        }
    }

    fun editRentEntry(
        rentId: Long,
        newRentAmountPaise: Long,
        newDueDateIso: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.editRentEntry(rentId, newRentAmountPaise, newDueDateIso)
                .onSuccess {
                    _events.emit(UiEvent.Success("Rent entry updated."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to update rent entry.")) }
        }
    }

    fun deleteRentEntry(rentId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteRentEntry(rentId)
                .onSuccess {
                    _events.emit(UiEvent.Success("Rent entry deleted."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to delete rent entry.")) }
        }
    }

    suspend fun previewPaymentAllocation(tenantId: Long, amountPaise: Long): PaymentAllocationPreview =
        repository.previewPaymentAllocation(tenantId, amountPaise)

    fun receivePayment(
        tenantId: Long,
        amountPaise: Long,
        paymentDate: String,
        paymentMode: String,
        remarks: String,
        onSuccess: (PaymentReceiptEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.receivePayment(tenantId, amountPaise, paymentDate, paymentMode, remarks)
                .onSuccess { receipt ->
                    _events.emit(UiEvent.Success("Payment saved. Receipt ${receipt.receiptNumber} generated."))
                    onSuccess(receipt)
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to record payment.")) }
        }
    }

    fun deleteReceipt(paymentId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteReceipt(paymentId)
                .onSuccess {
                    _events.emit(UiEvent.Success("Receipt deleted and balances reversed."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to delete receipt.")) }
        }
    }

    fun applyAdvanceToRent(
        tenantId: Long,
        rentEntryId: Long,
        amountPaise: Long,
        dateIso: String,
        remarks: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.applyAdvanceToRent(tenantId, rentEntryId, amountPaise, dateIso, remarks)
                .onSuccess {
                    _events.emit(UiEvent.Success("Advance applied to rent."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to apply advance.")) }
        }
    }

    fun addElectricityBill(
        roomNumber: Int,
        tenantId: Long?,
        periodFrom: String,
        periodTo: String,
        previousReading: Double,
        currentReading: Double,
        ratePerUnitPaise: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.addElectricityBill(
                roomNumber, tenantId, periodFrom, periodTo,
                previousReading, currentReading, ratePerUnitPaise
            ).onSuccess {
                _events.emit(UiEvent.Success("Electricity bill saved for Room $roomNumber."))
                onSuccess()
            }.onFailure {
                _events.emit(UiEvent.Error(it.message ?: "Failed to save electricity bill."))
            }
        }
    }

    fun editElectricityBill(
        billId: Long,
        periodFrom: String,
        periodTo: String,
        previousReading: Double,
        currentReading: Double,
        ratePerUnitPaise: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.editElectricityBill(
                billId, periodFrom, periodTo, previousReading, currentReading, ratePerUnitPaise
            ).onSuccess {
                _events.emit(UiEvent.Success("Electricity bill updated and reconciled."))
                onSuccess()
            }.onFailure {
                _events.emit(UiEvent.Error(it.message ?: "Failed to update electricity bill."))
            }
        }
    }

    fun deleteElectricityBill(billId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteElectricityBill(billId)
                .onSuccess {
                    _events.emit(UiEvent.Success("Electricity bill deleted."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to delete bill.")) }
        }
    }

    fun writeOffRent(
        rentEntryId: Long,
        amountPaise: Long,
        writeOffDate: String,
        remarks: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.writeOffRent(rentEntryId, amountPaise, writeOffDate, remarks)
                .onSuccess {
                    _events.emit(UiEvent.Success("Rent write-off recorded."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to write off rent.")) }
        }
    }

    fun undoWriteOff(writeOffId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.undoWriteOff(writeOffId)
                .onSuccess {
                    _events.emit(UiEvent.Success("Write-off undone and balance restored."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to undo write-off.")) }
        }
    }

    fun addFollowUp(
        tenantId: Long,
        followUpDate: String,
        promisedPaymentDate: String,
        pendingAmountPaise: Long,
        remarks: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.addFollowUp(tenantId, followUpDate, promisedPaymentDate, pendingAmountPaise, remarks)
                .onSuccess {
                    _events.emit(UiEvent.Success("Follow-up saved."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to save follow-up.")) }
        }
    }

    fun editFollowUp(
        followUpId: Long,
        followUpDate: String,
        promisedPaymentDate: String,
        pendingAmountPaise: Long,
        remarks: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.editFollowUp(followUpId, followUpDate, promisedPaymentDate, pendingAmountPaise, remarks)
                .onSuccess {
                    _events.emit(UiEvent.Success("Follow-up updated."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to update follow-up.")) }
        }
    }

    fun markFollowUpCompleted(followUpId: Long) {
        viewModelScope.launch {
            repository.markFollowUpCompleted(followUpId)
                .onSuccess { _events.emit(UiEvent.Success("Follow-up marked as completed.")) }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to complete follow-up.")) }
        }
    }

    fun reopenFollowUp(followUpId: Long) {
        viewModelScope.launch {
            repository.reopenFollowUp(followUpId)
                .onSuccess { _events.emit(UiEvent.Success("Follow-up reopened.")) }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to reopen follow-up.")) }
        }
    }

    fun deleteFollowUp(followUpId: Long) {
        viewModelScope.launch {
            repository.deleteFollowUp(followUpId)
                .onSuccess { _events.emit(UiEvent.Success("Follow-up deleted.")) }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to delete follow-up.")) }
        }
    }

    fun updateSettings(ratePaise: Long, reminderDays: Int) {
        viewModelScope.launch {
            repository.updateSettings(ratePaise, reminderDays)
                .onSuccess { _events.emit(UiEvent.Success("Settings saved.")) }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to save settings.")) }
        }
    }

    suspend fun exportBackupJson(): Result<String> = repository.exportBackupJson(markBackupCompleted = true)

    suspend fun validateBackupJson(json: String): Result<BackupPayload> = repository.validateBackupJson(json)

    fun restoreBackup(payload: BackupPayload, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.restoreBackupPayload(payload)
                .onSuccess {
                    _events.emit(UiEvent.Success("Backup restored successfully (${payload.tenants.size} tenants, ${payload.paymentReceipts.size} receipts)."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Restore failed.")) }
        }
    }

    fun deleteAllData(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAllApplicationData()
                .onSuccess { kycPaths ->
                    kycPaths.forEach { KycFileHelper.deleteFileIfExists(it) }
                    _events.emit(UiEvent.Success("All application data deleted. 13 vacant rooms reset."))
                    onSuccess()
                }
                .onFailure { _events.emit(UiEvent.Error(it.message ?: "Failed to delete data.")) }
        }
    }

    fun emitSuccess(message: String) {
        viewModelScope.launch { _events.emit(UiEvent.Success(message)) }
    }

    fun emitError(message: String) {
        viewModelScope.launch { _events.emit(UiEvent.Error(message)) }
    }
}
