package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AdvanceTransactionEntity
import com.example.data.db.ElectricityBillEntity
import com.example.data.db.PaymentReceiptEntity
import com.example.data.db.RentEntryEntity
import com.example.data.db.RoomEntity
import com.example.data.db.TenantEntity
import com.example.data.db.WriteOffEntity
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownField
import com.example.ui.components.MoneyText
import com.example.ui.components.RrrBrandTopBar
import com.example.ui.components.StatusPill
import com.example.ui.dialogs.TenantFormDialog
import com.example.ui.dialogs.TransferRoomDialog
import com.example.ui.dialogs.VacateTenantDialog
import com.example.ui.theme.CardSurface
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusGreenBorder
import com.example.ui.theme.StatusGreenText
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.StatusRedBorder
import com.example.ui.theme.StatusRedText
import com.example.ui.theme.StatusVacantBg
import com.example.ui.theme.StatusVacantBorder
import com.example.ui.theme.StatusVacantText
import com.example.ui.viewmodel.RoomStatus
import com.example.ui.viewmodel.RrrUiState
import com.example.util.DateUtils
import com.example.util.KycFileHelper
import com.example.util.MoneyUtils
import com.example.util.PdfAndPrintHelper
import com.example.util.WhatsAppHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomDetailScreen(
    roomNumber: Int,
    uiState: RrrUiState,
    onBack: () -> Unit,
    onOpenReceivePayment: (tenantId: Long) -> Unit,
    onAddTenantToRoom: (
        name: String, mobile: String, roomNum: Int, monthlyRentPaise: Long,
        entryDate: String, kycType: String, kycNumber: String,
        kycDocPath: String?, kycDocName: String?, notes: String, onDone: () -> Unit
    ) -> Unit,
    onEditTenant: (
        tenantId: Long, name: String, mobile: String, monthlyRentPaise: Long,
        entryDate: String, kycType: String, kycNumber: String,
        kycDocPath: String?, kycDocName: String?, notes: String, oldKycPath: String?, onDone: () -> Unit
    ) -> Unit,
    onTransferTenant: (tenantId: Long, toRoom: Int, date: String, remarks: String, onDone: () -> Unit) -> Unit,
    onVacateTenant: (tenantId: Long, date: String, onDone: () -> Unit) -> Unit,
    onDeleteTenant: (tenantId: Long, onDone: () -> Unit) -> Unit,
    onAddManualRent: (tenantId: Long, dueDate: String, amountPaise: Long, onDone: () -> Unit) -> Unit,
    onEditRent: (rentId: Long, amountPaise: Long, dueDate: String, onDone: () -> Unit) -> Unit,
    onDeleteRent: (rentId: Long) -> Unit,
    onWriteOffRent: (rentId: Long, amountPaise: Long, date: String, remarks: String, onDone: () -> Unit) -> Unit,
    onUndoWriteOff: (writeOffId: Long) -> Unit,
    onApplyAdvance: (tenantId: Long, rentId: Long, amountPaise: Long, date: String, remarks: String, onDone: () -> Unit) -> Unit,
    onAddElectricityBill: (roomNum: Int, tenantId: Long?, from: String, to: String, prev: Double, curr: Double, ratePaise: Long, onDone: () -> Unit) -> Unit,
    onEditElectricityBill: (billId: Long, from: String, to: String, prev: Double, curr: Double, ratePaise: Long, onDone: () -> Unit) -> Unit,
    onDeleteElectricityBill: (billId: Long) -> Unit,
    onDeleteReceipt: (paymentId: Long) -> Unit,
    onAddFollowUp: (tenantId: Long, fDate: String, pDate: String, pendingPaise: Long, remarks: String, onDone: () -> Unit) -> Unit,
    onSuccessMsg: (String) -> Unit,
    onErrorMsg: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val tile = uiState.roomTiles.firstOrNull { it.room.roomNumber == roomNumber }
    val tenant = tile?.tenant
    val rooms = uiState.roomTiles.map { it.room }
    val vacantRooms = remember(rooms) { rooms.filter { it.currentTenantId == null } }

    // Previous occupants of this room (vacated or transferred out)
    val previousOccupants = remember(uiState.tenants, uiState.transfers, uiState.rentEntries, uiState.electricityBills, roomNumber, tenant?.id) {
        val transferredFromIds = uiState.transfers.filter { it.fromRoomNumber == roomNumber }.map { it.tenantId }.toSet()
        val roomRentIds = uiState.rentEntries.filter { it.roomNumber == roomNumber }.map { it.tenantId }.toSet()
        val roomElecIds = uiState.electricityBills.filter { it.roomNumber == roomNumber }.map { it.tenantId }.toSet()
        uiState.tenants.filter { t ->
            t.id != tenant?.id && (t.currentRoomNumber == roomNumber || t.id in transferredFromIds || t.id in roomRentIds || t.id in roomElecIds)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Rent, 1=Electricity, 2=Receipts, 3=Advance & History
    var showPreviousOccupants by remember { mutableStateOf(false) }
    var inspectingHistoryTenant by remember { mutableStateOf<TenantEntity?>(null) }

    // Dialog states
    var showAllotDialog by remember { mutableStateOf(false) }
    var showEditTenantDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showVacateDialog by remember { mutableStateOf(false) }
    var showDeleteTenantDialog by remember { mutableStateOf(false) }
    var kycViewTenant by remember { mutableStateOf<TenantEntity?>(null) }

    var showAddRentDialog by remember { mutableStateOf(false) }
    var editingRent by remember { mutableStateOf<RentEntryEntity?>(null) }
    var deletingRent by remember { mutableStateOf<RentEntryEntity?>(null) }
    var writeOffTargetRent by remember { mutableStateOf<RentEntryEntity?>(null) }
    var showApplyAdvanceDialog by remember { mutableStateOf<RentEntryEntity?>(null) }

    var showAddElecDialog by remember { mutableStateOf(false) }
    var editingElec by remember { mutableStateOf<ElectricityBillEntity?>(null) }
    var deletingElec by remember { mutableStateOf<ElectricityBillEntity?>(null) }

    var viewingReceipt by remember { mutableStateOf<PaymentReceiptEntity?>(null) }
    var deletingReceipt by remember { mutableStateOf<PaymentReceiptEntity?>(null) }
    var pendingPdfReceipt by remember { mutableStateOf<PaymentReceiptEntity?>(null) }

    var showAddFollowUpDialog by remember { mutableStateOf(false) }
    var undoWriteOffTarget by remember { mutableStateOf<WriteOffEntity?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val rec = pendingPdfReceipt
        pendingPdfReceipt = null
        if (uri != null && rec != null) {
            val tName = uiState.tenants.firstOrNull { it.id == rec.tenantId }?.name ?: "Tenant"
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfAndPrintHelper.writeReceiptPdf(context, out, rec.toPrintData(tName))
                } ?: error("Cannot open output file.")
            }.onSuccess {
                onSuccessMsg("Receipt PDF saved (${rec.receiptNumber}).")
            }.onFailure {
                onErrorMsg("Failed to save PDF: ${it.message}")
            }
        }
    }

    val activeTenantForLedger = inspectingHistoryTenant ?: tenant
    val tenantRents = remember(uiState.rentEntries, activeTenantForLedger?.id) {
        activeTenantForLedger?.let { t -> uiState.rentEntries.filter { it.tenantId == t.id } } ?: emptyList()
    }
    val tenantBills = remember(uiState.electricityBills, activeTenantForLedger?.id) {
        activeTenantForLedger?.let { t -> uiState.electricityBills.filter { it.tenantId == t.id } } ?: emptyList()
    }
    val tenantReceipts = remember(uiState.receipts, activeTenantForLedger?.id) {
        activeTenantForLedger?.let { t -> uiState.receipts.filter { it.tenantId == t.id } } ?: emptyList()
    }
    val tenantAdvanceTxs = remember(uiState.advanceTransactions, activeTenantForLedger?.id) {
        activeTenantForLedger?.let { t -> uiState.advanceTransactions.filter { it.tenantId == t.id } } ?: emptyList()
    }
    val tenantWriteOffs = remember(uiState.writeOffs, activeTenantForLedger?.id) {
        activeTenantForLedger?.let { t -> uiState.writeOffs.filter { it.tenantId == t.id } } ?: emptyList()
    }
    val tenantTransfers = remember(uiState.transfers, activeTenantForLedger?.id) {
        activeTenantForLedger?.let { t -> uiState.transfers.filter { it.tenantId == t.id } } ?: emptyList()
    }
    val tenantFollowUps = remember(uiState.followUps, activeTenantForLedger?.id) {
        activeTenantForLedger?.let { t -> uiState.followUps.filter { it.tenantId == t.id } } ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        RrrBrandTopBar(
            title = if (inspectingHistoryTenant != null) {
                "History: ${inspectingHistoryTenant!!.name}"
            } else {
                "Room $roomNumber"
            },
            subtitle = when {
                inspectingHistoryTenant != null -> "Archived / Previous Occupant"
                tile?.status == RoomStatus.CLEAR -> "Rent & Electricity Clear"
                tile?.status == RoomStatus.DUE -> "Total Due: ${MoneyUtils.formatRupees(tile.totalDuePaise)}"
                else -> "Vacant Room"
            },
            showBack = true,
            onBack = {
                if (inspectingHistoryTenant != null) inspectingHistoryTenant = null else onBack()
            },
            actions = {
                TextButton(
                    onClick = { showPreviousOccupants = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    modifier = Modifier.testTag("room_previous_occupants_btn")
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Past (${previousOccupants.size})")
                }
            }
        )

        if (activeTenantForLedger == null) {
            // Vacant room state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                StatusPill(
                    text = "Vacant Room $roomNumber",
                    bgColor = StatusVacantBg,
                    borderColor = StatusVacantBorder,
                    textColor = StatusVacantText
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Room $roomNumber is currently available.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Last meter reading: ${MoneyUtils.formatUnits(tile?.room?.lastMeterReading ?: 0.0)} units",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { showAllotDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("allot_room_btn")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Allot Room $roomNumber / Add Tenant")
                }
                if (previousOccupants.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showPreviousOccupants = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.History, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View Previous Occupants (${previousOccupants.size})")
                    }
                }
            }
        } else {
            val dispTenant = activeTenantForLedger
            val rPending = tenantRents.sumOf { it.pendingPaise }
            val ePending = tenantBills.sumOf { it.pendingPaise }
            val totalDue = rPending + ePending

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Tenant Profile Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        border = BorderStroke(1.dp, OutlineSubtle),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dispTenant.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Mobile: ${dispTenant.mobileNumber} · Entry: ${DateUtils.formatDisplayDate(dispTenant.entryDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(
                                    text = if (dispTenant.isActive) "Active" else "Vacated",
                                    bgColor = if (dispTenant.isActive) StatusGreenBg else StatusVacantBg,
                                    borderColor = if (dispTenant.isActive) StatusGreenBorder else StatusVacantBorder,
                                    textColor = if (dispTenant.isActive) StatusGreenText else StatusVacantText
                                )
                            }

                            Surface(
                                color = NavySurfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Monthly Rent", style = MaterialTheme.typography.bodySmall)
                                        MoneyText(MoneyUtils.formatRupees(dispTenant.monthlyRentPaise))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("KYC (${dispTenant.kycType})", style = MaterialTheme.typography.bodySmall)
                                        Text(dispTenant.kycNumber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    if (!dispTenant.vacatedDate.isNullOrBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Vacated Date", style = MaterialTheme.typography.bodySmall)
                                            Text(DateUtils.formatDisplayDate(dispTenant.vacatedDate), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (dispTenant.notes.isNotBlank()) {
                                        Text(
                                            "Notes: ${dispTenant.notes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { kycViewTenant = dispTenant },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("tenant_view_kyc_btn")
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("KYC Record")
                                }
                                if (dispTenant.isActive && inspectingHistoryTenant == null) {
                                    OutlinedButton(
                                        onClick = { showEditTenantDialog = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("tenant_edit_btn")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                    OutlinedButton(
                                        onClick = { showTransferDialog = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("tenant_transfer_btn")
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Transfer")
                                    }
                                    OutlinedButton(
                                        onClick = { showVacateDialog = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("tenant_vacate_btn")
                                    ) {
                                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Vacate")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showDeleteTenantDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRedBorder),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("tenant_delete_btn")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }

                // Financial Summary Strip
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (totalDue == 0L) StatusGreenBg else StatusRedBg
                        ),
                        border = BorderStroke(1.dp, if (totalDue == 0L) StatusGreenBorder else StatusRedBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SummaryMetric("Rent Pending", rPending, if (rPending > 0) StatusRedText else StatusGreenText)
                                SummaryMetric("Elec Pending", ePending, if (ePending > 0) StatusRedText else StatusGreenText)
                                SummaryMetric("Total Due", totalDue, if (totalDue > 0) StatusRedText else StatusGreenText)
                                SummaryMetric("Advance Bal", dispTenant.advanceBalancePaise, GoldDark)
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { onOpenReceivePayment(dispTenant.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    modifier = Modifier.testTag("detail_receive_payment_btn")
                                ) {
                                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Payment Received")
                                }
                                OutlinedButton(
                                    onClick = { showAddRentDialog = true },
                                    modifier = Modifier.testTag("detail_add_rent_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Rent")
                                }
                                if (dispTenant.isActive && inspectingHistoryTenant == null) {
                                    OutlinedButton(
                                        onClick = { showAddElecDialog = true },
                                        modifier = Modifier.testTag("detail_add_elec_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Elec Bill")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showAddFollowUpDialog = true },
                                    modifier = Modifier.testTag("detail_add_followup_btn")
                                ) {
                                    Text("+ Follow-Up")
                                }
                            }
                        }
                    }
                }

                // Ledger Tabs
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            label = { Text("Rent (${tenantRents.size})") }
                        )
                        FilterChip(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            label = { Text("Electricity (${tenantBills.size})") }
                        )
                        FilterChip(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            label = { Text("Receipts (${tenantReceipts.size})") }
                        )
                        FilterChip(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            label = { Text("Advance & History") }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        if (tenantRents.isEmpty()) {
                            item { EmptyLedgerText("No rent entries yet.") }
                        } else {
                            items(tenantRents, key = { it.id }) { rent ->
                                val activeWo = tenantWriteOffs.firstOrNull { it.rentEntryId == rent.id && !it.isReversed }
                                RentEntryCard(
                                    rent = rent,
                                    canApplyAdvance = dispTenant.advanceBalancePaise > 0L && rent.pendingPaise > 0L,
                                    hasActiveWriteOff = activeWo != null,
                                    onEdit = { editingRent = rent },
                                    onDelete = { deletingRent = rent },
                                    onWriteOff = { writeOffTargetRent = rent },
                                    onUndoWriteOff = { activeWo?.let { undoWriteOffTarget = it } },
                                    onApplyAdvance = { showApplyAdvanceDialog = rent }
                                )
                            }
                        }
                    }

                    1 -> {
                        if (tenantBills.isEmpty()) {
                            item { EmptyLedgerText("No electricity bills yet.") }
                        } else {
                            items(tenantBills, key = { it.id }) { bill ->
                                ElectricityBillCard(
                                    bill = bill,
                                    tenantName = dispTenant.name,
                                    onEdit = { editingElec = bill },
                                    onDelete = { deletingElec = bill },
                                    onWhatsApp = {
                                        val msg = WhatsAppHelper.buildElectricityReminderMessage(
                                            roomNumber = bill.roomNumber,
                                            tenantName = dispTenant.name,
                                            periodFrom = bill.periodFrom,
                                            periodTo = bill.periodTo,
                                            unitsConsumed = bill.unitsConsumed,
                                            ratePerUnitPaise = bill.ratePerUnitPaise,
                                            billAmountPaise = bill.totalBillPaise,
                                            pendingAmountPaise = bill.pendingPaise
                                        )
                                        WhatsAppHelper.sendMessage(context, msg)
                                    }
                                )
                            }
                        }
                    }

                    2 -> {
                        if (tenantReceipts.isEmpty()) {
                            item { EmptyLedgerText("No payment receipts yet.") }
                        } else {
                            items(tenantReceipts, key = { it.id }) { rec ->
                                ReceiptCard(
                                    receipt = rec,
                                    tenantName = dispTenant.name,
                                    onView = { viewingReceipt = rec },
                                    onPrint = {
                                        PdfAndPrintHelper.printReceipt(context, rec.toPrintData(dispTenant.name))
                                    },
                                    onSavePdf = {
                                        pendingPdfReceipt = rec
                                        savePdfLauncher.launch("Receipt_${rec.receiptNumber}.pdf")
                                    },
                                    onDelete = { deletingReceipt = rec }
                                )
                            }
                        }
                    }

                    3 -> {
                        item {
                            AdvanceSummaryCard(
                                tenant = dispTenant,
                                transactions = tenantAdvanceTxs,
                                pendingRents = tenantRents.filter { it.pendingPaise > 0L },
                                onApplyAdvanceToRent = { rent -> showApplyAdvanceDialog = rent }
                            )
                        }

                        if (tenantWriteOffs.isNotEmpty()) {
                            item {
                                Text(
                                    "Write-Off History",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }
                            items(tenantWriteOffs, key = { "wo_${it.id}" }) { wo ->
                                WriteOffHistoryCard(
                                    writeOff = wo,
                                    onUndo = { undoWriteOffTarget = wo }
                                )
                            }
                        }

                        if (tenantTransfers.isNotEmpty()) {
                            item {
                                Text(
                                    "Room Transfer History",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }
                            items(tenantTransfers, key = { "tr_${it.id}" }) { tr ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                                    border = BorderStroke(1.dp, OutlineSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Room ${tr.fromRoomNumber} → Room ${tr.toRoomNumber}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Date: ${DateUtils.formatDisplayDate(tr.transferDate)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (tr.remarks.isNotBlank()) {
                                            Text(tr.remarks, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }

                        if (tenantFollowUps.isNotEmpty()) {
                            item {
                                Text(
                                    "Follow-Up History",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }
                            items(tenantFollowUps, key = { "fu_${it.id}" }) { fu ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                                    border = BorderStroke(1.dp, OutlineSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "${fu.status} · Promised: ${DateUtils.formatDisplayDate(fu.promisedPaymentDate)}",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Pending at follow-up: ${MoneyUtils.formatRupees(fu.pendingAmountPaise)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (fu.remarks.isNotBlank()) {
                                            Text(fu.remarks, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== DIALOGS ====================

    if (showAllotDialog) {
        TenantFormDialog(
            vacantRooms = vacantRooms,
            initialRoomNumber = roomNumber,
            editingTenant = null,
            onSave = { name, mob, rNum, rent, entry, kType, kNum, kPath, kName, notes ->
                onAddTenantToRoom(name, mob, rNum, rent, entry, kType, kNum, kPath, kName, notes) {
                    showAllotDialog = false
                }
            },
            onDismiss = { showAllotDialog = false }
        )
    }

    if (showEditTenantDialog && tenant != null) {
        TenantFormDialog(
            vacantRooms = vacantRooms,
            initialRoomNumber = tenant.currentRoomNumber,
            editingTenant = tenant,
            onSave = { name, mob, _, rent, entry, kType, kNum, kPath, kName, notes ->
                onEditTenant(
                    tenant.id, name, mob, rent, entry, kType, kNum, kPath, kName, notes,
                    tenant.kycDocumentPath
                ) {
                    showEditTenantDialog = false
                }
            },
            onDismiss = { showEditTenantDialog = false }
        )
    }

    if (showTransferDialog && tenant != null) {
        TransferRoomDialog(
            tenant = tenant,
            vacantRooms = vacantRooms,
            onConfirmTransfer = { toRoom, date, remarks ->
                onTransferTenant(tenant.id, toRoom, date, remarks) {
                    showTransferDialog = false
                    onBack()
                }
            },
            onDismiss = { showTransferDialog = false }
        )
    }

    if (showVacateDialog && tenant != null) {
        VacateTenantDialog(
            tenant = tenant,
            onConfirmVacate = { date ->
                onVacateTenant(tenant.id, date) {
                    showVacateDialog = false
                }
            },
            onDismiss = { showVacateDialog = false }
        )
    }

    if (showDeleteTenantDialog && activeTenantForLedger != null) {
        ConfirmActionDialog(
            title = "Delete Tenant ${activeTenantForLedger.name}?",
            message = "This will permanently remove ${activeTenantForLedger.name} along with all their rent entries, electricity bills, receipts, advance ledger, write-offs, and follow-ups. Other tenants' balances will not be affected.",
            confirmText = "Delete Tenant",
            isDestructive = true,
            onConfirm = {
                val tid = activeTenantForLedger.id
                showDeleteTenantDialog = false
                onDeleteTenant(tid) {
                    inspectingHistoryTenant = null
                }
            },
            onDismiss = { showDeleteTenantDialog = false }
        )
    }

    kycViewTenant?.let { kt ->
        KycViewerDialog(
            tenant = kt,
            onDismiss = { kycViewTenant = null }
        )
    }

    if (showPreviousOccupants) {
        AlertDialog(
            onDismissRequest = { showPreviousOccupants = false },
            title = { Text("Previous Occupants — Room $roomNumber", fontWeight = FontWeight.Bold) },
            text = {
                if (previousOccupants.isEmpty()) {
                    Text("No previous occupants recorded for Room $roomNumber.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        previousOccupants.forEach { prev ->
                            Surface(
                                color = NavySurfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prev.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Entry: ${DateUtils.formatDisplayDate(prev.entryDate)}" +
                                                (prev.vacatedDate?.let { " · Vacated: ${DateUtils.formatDisplayDate(it)}" } ?: " · Active in Room ${prev.currentRoomNumber}"),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    TextButton(onClick = {
                                        showPreviousOccupants = false
                                        inspectingHistoryTenant = prev
                                    }) {
                                        Text("History")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPreviousOccupants = false }) { Text("Close") }
            }
        )
    }

    if (showAddRentDialog && activeTenantForLedger != null) {
        AddOrEditRentDialog(
            editingRent = null,
            defaultAmountPaise = activeTenantForLedger.monthlyRentPaise,
            onSave = { dueIso, amountPaise ->
                onAddManualRent(activeTenantForLedger.id, dueIso, amountPaise) {
                    showAddRentDialog = false
                }
            },
            onDismiss = { showAddRentDialog = false }
        )
    }

    editingRent?.let { r ->
        AddOrEditRentDialog(
            editingRent = r,
            defaultAmountPaise = r.rentAmountPaise,
            onSave = { dueIso, amountPaise ->
                onEditRent(r.id, amountPaise, dueIso) {
                    editingRent = null
                }
            },
            onDismiss = { editingRent = null }
        )
    }

    deletingRent?.let { r ->
        ConfirmActionDialog(
            title = "Delete Rent Entry?",
            message = "Delete ${DateUtils.formatMonthYear(r.rentMonth)} rent entry (${MoneyUtils.formatRupees(r.rentAmountPaise)})? Auto-generated entries deleted here will not regenerate automatically on restart.",
            confirmText = "Delete Rent",
            isDestructive = true,
            onConfirm = {
                val id = r.id
                deletingRent = null
                onDeleteRent(id)
            },
            onDismiss = { deletingRent = null }
        )
    }

    writeOffTargetRent?.let { r ->
        WriteOffDialog(
            rent = r,
            onConfirm = { amountPaise, dateIso, remarks ->
                onWriteOffRent(r.id, amountPaise, dateIso, remarks) {
                    writeOffTargetRent = null
                }
            },
            onDismiss = { writeOffTargetRent = null }
        )
    }

    undoWriteOffTarget?.let { wo ->
        ConfirmActionDialog(
            title = "Undo Write-Off?",
            message = "Reverse write-off of ${MoneyUtils.formatRupees(wo.amountPaise)} dated ${DateUtils.formatDisplayDate(wo.writeOffDate)}? This will restore the pending rent balance.",
            confirmText = "Confirm Undo",
            onConfirm = {
                val id = wo.id
                undoWriteOffTarget = null
                onUndoWriteOff(id)
            },
            onDismiss = { undoWriteOffTarget = null }
        )
    }

    showApplyAdvanceDialog?.let { r ->
        if (activeTenantForLedger != null) {
            ApplyAdvanceDialog(
                tenant = activeTenantForLedger,
                rent = r,
                onConfirm = { amountPaise, dateIso, remarks ->
                    onApplyAdvance(activeTenantForLedger.id, r.id, amountPaise, dateIso, remarks) {
                        showApplyAdvanceDialog = null
                    }
                },
                onDismiss = { showApplyAdvanceDialog = null }
            )
        }
    }

    if (showAddElecDialog && tenant != null) {
        ElectricityBillDialog(
            rooms = rooms,
            tenants = uiState.tenants,
            bills = uiState.electricityBills,
            defaultRatePaise = uiState.settings.defaultElectricityRatePaise,
            initialRoomNumber = roomNumber,
            editingBill = null,
            onSave = { rNum, tId, from, to, prev, curr, rate ->
                onAddElectricityBill(rNum, tId, from, to, prev, curr, rate) {
                    showAddElecDialog = false
                }
            },
            onDismiss = { showAddElecDialog = false }
        )
    }

    editingElec?.let { bill ->
        ElectricityBillDialog(
            rooms = rooms,
            tenants = uiState.tenants,
            bills = uiState.electricityBills,
            defaultRatePaise = bill.ratePerUnitPaise,
            initialRoomNumber = bill.roomNumber,
            editingBill = bill,
            onSave = { _, _, from, to, prev, curr, rate ->
                onEditElectricityBill(bill.id, from, to, prev, curr, rate) {
                    editingElec = null
                }
            },
            onDismiss = { editingElec = null }
        )
    }

    deletingElec?.let { bill ->
        ConfirmActionDialog(
            title = "Delete Electricity Bill?",
            message = "Delete bill (${DateUtils.formatDisplayDate(bill.periodFrom)} - ${DateUtils.formatDisplayDate(bill.periodTo)}, ${MoneyUtils.formatRupees(bill.totalBillPaise)})?",
            confirmText = "Delete Bill",
            isDestructive = true,
            onConfirm = {
                val id = bill.id
                deletingElec = null
                onDeleteElectricityBill(id)
            },
            onDismiss = { deletingElec = null }
        )
    }

    viewingReceipt?.let { rec ->
        val tName = activeTenantForLedger?.name ?: "Tenant"
        ReceiptViewerDialog(
            receipt = rec,
            tenantName = tName,
            onPrint = { PdfAndPrintHelper.printReceipt(context, rec.toPrintData(tName)) },
            onSavePdf = {
                pendingPdfReceipt = rec
                savePdfLauncher.launch("Receipt_${rec.receiptNumber}.pdf")
            },
            onDelete = {
                viewingReceipt = null
                deletingReceipt = rec
            },
            onDismiss = { viewingReceipt = null }
        )
    }

    deletingReceipt?.let { rec ->
        ConfirmActionDialog(
            title = "Delete Receipt ${rec.receiptNumber}?",
            message = "Deleting this receipt (${MoneyUtils.formatRupees(rec.totalAmountPaise)}) will reverse its rent, electricity, and advance allocations.",
            confirmText = "Delete Receipt",
            isDestructive = true,
            onConfirm = {
                val id = rec.id
                deletingReceipt = null
                onDeleteReceipt(id)
            },
            onDismiss = { deletingReceipt = null }
        )
    }

    if (showAddFollowUpDialog && activeTenantForLedger != null) {
        val pendingMap = remember(activeTenantForLedger.id, tenantRents, tenantBills) {
            mapOf(activeTenantForLedger.id to (tenantRents.sumOf { it.pendingPaise } + tenantBills.sumOf { it.pendingPaise }))
        }
        FollowUpFormDialog(
            tenants = listOf(activeTenantForLedger),
            pendingByTenant = pendingMap,
            initialTenantId = activeTenantForLedger.id,
            editingFollowUp = null,
            onSave = { tid, fDate, pDate, pendingPaise, remarks ->
                onAddFollowUp(tid, fDate, pDate, pendingPaise, remarks) {
                    showAddFollowUpDialog = false
                }
            },
            onDismiss = { showAddFollowUpDialog = false }
        )
    }
}

@Composable
private fun SummaryMetric(label: String, paise: Long, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(text = MoneyUtils.formatRupees(paise), color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyLedgerText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 20.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RentEntryCard(
    rent: RentEntryEntity,
    canApplyAdvance: Boolean,
    hasActiveWriteOff: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWriteOff: () -> Unit,
    onUndoWriteOff: () -> Unit,
    onApplyAdvance: () -> Unit
) {
    val isClear = rent.pendingPaise == 0L
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, OutlineSubtle),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${DateUtils.formatMonthYear(rent.rentMonth)} · Room ${rent.roomNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Due Date: ${DateUtils.formatDisplayDate(rent.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = rent.status,
                    bgColor = if (isClear) StatusGreenBg else StatusRedBg,
                    borderColor = if (isClear) StatusGreenBorder else StatusRedBorder,
                    textColor = if (isClear) StatusGreenText else StatusRedText
                )
            }

            Surface(
                color = NavySurfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rent Amount", style = MaterialTheme.typography.bodySmall)
                        MoneyText(MoneyUtils.formatRupees(rent.rentAmountPaise))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paid", style = MaterialTheme.typography.bodySmall)
                        MoneyText(MoneyUtils.formatRupees(rent.amountPaidPaise), StatusGreenText)
                    }
                    if (rent.advanceAppliedPaise > 0L) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Advance Applied", style = MaterialTheme.typography.bodySmall)
                            MoneyText(MoneyUtils.formatRupees(rent.advanceAppliedPaise), NavyPrimary)
                        }
                    }
                    if (rent.writeOffPaise > 0L) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Written Off", style = MaterialTheme.typography.bodySmall)
                            MoneyText(MoneyUtils.formatRupees(rent.writeOffPaise), GoldDark)
                        }
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pending", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        MoneyText(
                            MoneyUtils.formatRupees(rent.pendingPaise),
                            if (rent.pendingPaise > 0) StatusRedText else StatusGreenText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("rent_edit_${rent.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelSmall)
                }
                if (rent.pendingPaise > 0L) {
                    OutlinedButton(
                        onClick = onWriteOff,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("rent_writeoff_${rent.id}")
                    ) {
                        Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Write Off", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (hasActiveWriteOff) {
                    OutlinedButton(
                        onClick = onUndoWriteOff,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("rent_undo_writeoff_${rent.id}")
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Undo W/O", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (canApplyAdvance) {
                    OutlinedButton(
                        onClick = onApplyAdvance,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("rent_apply_advance_${rent.id}")
                    ) {
                        Text("Apply Advance", style = MaterialTheme.typography.labelSmall)
                    }
                }
                val isEligibleForDelete = rent.amountPaidPaise == 0L &&
                    rent.advanceAppliedPaise == 0L && rent.writeOffPaise == 0L
                OutlinedButton(
                    onClick = onDelete,
                    enabled = isEligibleForDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRedBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("rent_delete_${rent.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete rent", modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun AdvanceSummaryCard(
    tenant: TenantEntity,
    transactions: List<AdvanceTransactionEntity>,
    pendingRents: List<RentEntryEntity>,
    onApplyAdvanceToRent: (RentEntryEntity) -> Unit
) {
    val totalAdded = transactions.filter { it.type == "ADDED_FROM_PAYMENT" }.sumOf { it.amountPaise }
    val totalUsed = transactions.filter { it.type == "ADJUSTED_TO_RENT" }.sumOf { it.amountPaise }

    Card(
        colors = CardDefaults.cardColors(containerColor = GoldLight),
        border = BorderStroke(1.dp, GoldDark.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Advance / Credit Ledger", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryMetric("Total Added", totalAdded, NavyPrimary)
                SummaryMetric("Total Used", totalUsed, StatusGreenText)
                SummaryMetric("Remaining", tenant.advanceBalancePaise, GoldDark)
            }

            if (tenant.advanceBalancePaise > 0L && pendingRents.isNotEmpty()) {
                Button(
                    onClick = { onApplyAdvanceToRent(pendingRents.first()) },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("apply_advance_oldest_btn")
                ) {
                    Text("Adjust Advance Against ${DateUtils.formatMonthYear(pendingRents.first().rentMonth)} Rent")
                }
            }

            if (transactions.isNotEmpty()) {
                HorizontalDivider()
                transactions.forEach { tx ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (tx.type == "ADDED_FROM_PAYMENT") "Added via Payment" else "Adjusted to Rent",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${DateUtils.formatDisplayDate(tx.transactionDate)} · ${tx.remarks}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MoneyText(
                            text = (if (tx.type == "ADDED_FROM_PAYMENT") "+" else "-") + MoneyUtils.formatRupees(tx.amountPaise),
                            color = if (tx.type == "ADDED_FROM_PAYMENT") StatusGreenText else NavyPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WriteOffHistoryCard(
    writeOff: WriteOffEntity,
    onUndo: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = MoneyUtils.formatRupees(writeOff.amountPaise) +
                        if (writeOff.isReversed) " (REVERSED)" else " (ACTIVE)",
                    fontWeight = FontWeight.Bold,
                    color = if (writeOff.isReversed) MaterialTheme.colorScheme.onSurfaceVariant else StatusRedText
                )
                Text(
                    text = "Date: ${DateUtils.formatDisplayDate(writeOff.writeOffDate)}" +
                        (writeOff.reversedDate?.let { " · Undone: ${DateUtils.formatDisplayDate(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall
                )
                if (writeOff.remarks.isNotBlank()) {
                    Text("Remarks: ${writeOff.remarks}", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!writeOff.isReversed) {
                OutlinedButton(
                    onClick = onUndo,
                    modifier = Modifier.testTag("undo_writeoff_${writeOff.id}")
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Undo")
                }
            }
        }
    }
}

@Composable
private fun KycViewerDialog(
    tenant: TenantEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val path = tenant.kycDocumentPath
    val bitmap = remember(path) {
        if (path != null && KycFileHelper.isImageFile(path)) {
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("KYC Record — ${tenant.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Document Type: ${tenant.kycType}", fontWeight = FontWeight.SemiBold)
                Text("Document Number: ${tenant.kycNumber}", fontWeight = FontWeight.Bold)
                if (!tenant.kycDocumentName.isNullOrBlank()) {
                    Text("Uploaded File: ${tenant.kycDocumentName}", style = MaterialTheme.typography.bodySmall)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "KYC document preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    )
                } else if (!path.isNullOrBlank()) {
                    Button(
                        onClick = { KycFileHelper.openExternally(context, path) },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Open KYC Document")
                    }
                } else {
                    Text(
                        "No KYC file image/PDF uploaded. Use Edit Tenant to attach one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun AddOrEditRentDialog(
    editingRent: RentEntryEntity?,
    defaultAmountPaise: Long,
    onSave: (dueDateIso: String, amountPaise: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var dueDate by remember(editingRent) { mutableStateOf(editingRent?.dueDate ?: DateUtils.todayIso()) }
    var amountInput by remember(editingRent, defaultAmountPaise) {
        mutableStateOf(MoneyUtils.formatRupeesInput(editingRent?.rentAmountPaise ?: defaultAmountPaise))
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingRent == null) "Add Rent Entry" else "Edit Rent Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = StatusRedText, style = MaterialTheme.typography.bodySmall)
                }
                DatePickerField(
                    label = "Due Date *",
                    isoDate = dueDate,
                    onDateSelected = { dueDate = it },
                    testTag = "rent_due_date_field"
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it; errorMsg = null },
                    label = { Text("Rent Amount (Rs.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rent_amount_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = MoneyUtils.parseRupeesToPaise(amountInput)
                    if (amt == null || amt <= 0L) {
                        errorMsg = "Enter a valid rent amount."
                    } else {
                        onSave(dueDate, amt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("rent_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun WriteOffDialog(
    rent: RentEntryEntity,
    onConfirm: (amountPaise: Long, dateIso: String, remarks: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember(rent) { mutableStateOf(MoneyUtils.formatRupeesInput(rent.pendingPaise)) }
    var dateIso by remember { mutableStateOf(DateUtils.todayIso()) }
    var remarks by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write Off Rent", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Month: ${DateUtils.formatMonthYear(rent.rentMonth)} · Outstanding: ${MoneyUtils.formatRupees(rent.pendingPaise)}. Write-off reduces pending rent without counting as payment received.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = StatusRedText, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it; errorMsg = null },
                    label = { Text("Write-Off Amount (Rs.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("writeoff_amount_input")
                )
                DatePickerField(
                    label = "Write-Off Date",
                    isoDate = dateIso,
                    onDateSelected = { dateIso = it }
                )
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (Optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("writeoff_remarks_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = MoneyUtils.parseRupeesToPaise(amountInput)
                    when {
                        amt == null || amt <= 0L -> errorMsg = "Enter amount greater than 0."
                        amt > rent.pendingPaise -> errorMsg = "Cannot exceed outstanding rent (${MoneyUtils.formatRupees(rent.pendingPaise)})."
                        else -> onConfirm(amt, dateIso, remarks)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("writeoff_confirm_btn")
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("writeoff_cancel_btn")) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ApplyAdvanceDialog(
    tenant: TenantEntity,
    rent: RentEntryEntity,
    onConfirm: (amountPaise: Long, dateIso: String, remarks: String) -> Unit,
    onDismiss: () -> Unit
) {
    val maxApplicable = minOf(tenant.advanceBalancePaise, rent.pendingPaise)
    var amountInput by remember(maxApplicable) { mutableStateOf(MoneyUtils.formatRupeesInput(maxApplicable)) }
    var dateIso by remember { mutableStateOf(DateUtils.todayIso()) }
    var remarks by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply Advance to Rent", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Available Advance: ${MoneyUtils.formatRupees(tenant.advanceBalancePaise)}\nRent Pending (${DateUtils.formatMonthYear(rent.rentMonth)}): ${MoneyUtils.formatRupees(rent.pendingPaise)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = StatusRedText, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it; errorMsg = null },
                    label = { Text("Amount to Apply (Rs.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("apply_advance_amount_input")
                )
                DatePickerField(
                    label = "Adjustment Date",
                    isoDate = dateIso,
                    onDateSelected = { dateIso = it }
                )
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = MoneyUtils.parseRupeesToPaise(amountInput)
                    when {
                        amt == null || amt <= 0L -> errorMsg = "Enter a valid amount."
                        amt > maxApplicable -> errorMsg = "Cannot exceed ${MoneyUtils.formatRupees(maxApplicable)}."
                        else -> onConfirm(amt, dateIso, remarks)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("apply_advance_confirm_btn")
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
