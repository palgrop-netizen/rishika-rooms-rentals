package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.PaymentReceiptEntity
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.RrrBrandTopBar
import com.example.ui.dialogs.PaymentReceivedDialog
import com.example.ui.dialogs.TenantFormDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ElectricityScreen
import com.example.ui.screens.FollowUpsScreen
import com.example.ui.screens.PaymentsAndReceiptsScreen
import com.example.ui.screens.ReceiptViewerDialog
import com.example.ui.screens.ReportsAndSettingsScreen
import com.example.ui.screens.RoomDetailScreen
import com.example.ui.screens.toPrintData
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.RrrTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.RrrUiState
import com.example.ui.viewmodel.UiEvent
import com.example.util.PdfAndPrintHelper
import kotlinx.coroutines.flow.collectLatest

enum class MainNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Rooms", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    RECEIPTS("Receipts", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    ELECTRICITY("Electricity", Icons.Filled.Bolt, Icons.Outlined.Bolt),
    FOLLOW_UPS("Follow-Ups", Icons.Filled.EventNote, Icons.Outlined.EventNote),
    REPORTS("Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RrrTheme {
                RrrAppRoot()
            }
        }
    }
}

@Composable
fun RrrAppRoot(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(MainNavTab.DASHBOARD) }
    var reportsSubTab by remember { mutableIntStateOf(0) }
    var selectedRoomNumber by remember { mutableStateOf<Int?>(null) }

    var showAddTenantDialog by remember { mutableStateOf<Int?>(null) } // initial room number or -1
    var showPaymentDialogForTenant by remember { mutableStateOf<Long?>(null) } // null=closed, -1L=any, >0=specific tenant
    var newlyCreatedReceipt by remember { mutableStateOf<PaymentReceiptEntity?>(null) }
    var deletingNewReceipt by remember { mutableStateOf<PaymentReceiptEntity?>(null) }
    var pendingPdfReceipt by remember { mutableStateOf<PaymentReceiptEntity?>(null) }

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
                viewModel.emitSuccess("Receipt PDF saved (${rec.receiptNumber}).")
            }.onFailure {
                viewModel.emitError("Failed to save PDF: ${it.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            val msg = when (event) {
                is UiEvent.Success -> event.message
                is UiEvent.Error -> event.message
            }
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Back handling when on non-home tab
    if (selectedRoomNumber == null && currentTab != MainNavTab.DASHBOARD) {
        BackHandler {
            currentTab = MainNavTab.DASHBOARD
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp
        val overdueCount = uiState.overdueFollowUps.size

        Scaffold(
            snackbarHost = {
                val hostMod = if (selectedRoomNumber == null && !isWideScreen) {
                    Modifier
                } else {
                    Modifier.navigationBarsPadding()
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = hostMod
                )
            },
            bottomBar = {
                if (selectedRoomNumber == null && !isWideScreen) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 4.dp
                    ) {
                        MainNavTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (tab == MainNavTab.REPORTS) reportsSubTab = 0
                                    currentTab = tab
                                },
                                icon = { NavTabIcon(tab, selected, overdueCount) },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NavyPrimary,
                                    selectedTextColor = NavyPrimary,
                                    indicatorColor = NavySurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedRoomNumber != null) {
                    RoomDetailScreen(
                        roomNumber = selectedRoomNumber!!,
                        uiState = uiState,
                        onBack = { selectedRoomNumber = null },
                        onOpenReceivePayment = { tid -> showPaymentDialogForTenant = tid },
                        onAddTenantToRoom = { name, mob, rNum, rent, entry, kType, kNum, kPath, kName, notes, onDone ->
                            viewModel.addTenant(name, mob, rNum, rent, entry, kType, kNum, kPath, kName, notes, onDone)
                        },
                        onEditTenant = { tid, name, mob, rent, entry, kType, kNum, kPath, kName, notes, oldPath, onDone ->
                            viewModel.editTenant(tid, name, mob, rent, entry, kType, kNum, kPath, kName, notes, oldPath, onDone)
                        },
                        onTransferTenant = { tid, toRoom, date, remarks, onDone ->
                            viewModel.transferTenant(tid, toRoom, date, remarks, onDone)
                        },
                        onVacateTenant = { tid, date, onDone ->
                            viewModel.vacateTenant(tid, date, onDone)
                        },
                        onDeleteTenant = { tid, onDone ->
                            viewModel.deleteTenant(tid, onDone)
                        },
                        onAddManualRent = { tid, dueIso, amt, onDone ->
                            viewModel.addManualRentEntry(tid, dueIso, amt, onDone)
                        },
                        onEditRent = { rid, amt, dueIso, onDone ->
                            viewModel.editRentEntry(rid, amt, dueIso, onDone)
                        },
                        onDeleteRent = { rid -> viewModel.deleteRentEntry(rid) },
                        onWriteOffRent = { rid, amt, date, remarks, onDone ->
                            viewModel.writeOffRent(rid, amt, date, remarks, onDone)
                        },
                        onUndoWriteOff = { wid -> viewModel.undoWriteOff(wid) },
                        onApplyAdvance = { tid, rid, amt, date, remarks, onDone ->
                            viewModel.applyAdvanceToRent(tid, rid, amt, date, remarks, onDone)
                        },
                        onAddElectricityBill = { rNum, tid, from, to, prev, curr, rate, onDone ->
                            viewModel.addElectricityBill(rNum, tid, from, to, prev, curr, rate, onDone)
                        },
                        onEditElectricityBill = { bid, from, to, prev, curr, rate, onDone ->
                            viewModel.editElectricityBill(bid, from, to, prev, curr, rate, onDone)
                        },
                        onDeleteElectricityBill = { bid -> viewModel.deleteElectricityBill(bid) },
                        onDeleteReceipt = { pid -> viewModel.deleteReceipt(pid) },
                        onAddFollowUp = { tid, fDate, pDate, pend, rem, onDone ->
                            viewModel.addFollowUp(tid, fDate, pDate, pend, rem, onDone)
                        },
                        onSuccessMsg = { viewModel.emitSuccess(it) },
                        onErrorMsg = { viewModel.emitError(it) }
                    )
                } else if (isWideScreen) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail(
                            containerColor = Color.White,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            MainNavTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        if (tab == MainNavTab.REPORTS) reportsSubTab = 0
                                        currentTab = tab
                                    },
                                    icon = {
                                        NavTabIcon(tab, selected, overdueCount)
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            RrrBrandTopBar(
                                title = "Rishika Rooms & Rentals",
                                subtitle = "RRR · 13-Room Management"
                            )
                            TabContentArea(
                                currentTab = currentTab,
                                reportsSubTab = reportsSubTab,
                                uiState = uiState,
                                viewModel = viewModel,
                                onRoomClick = { selectedRoomNumber = it.room.roomNumber },
                                onOpenAddTenant = { showAddTenantDialog = it ?: -1 },
                                onOpenReceivePayment = { showPaymentDialogForTenant = -1L },
                                onOpenOverdueFollowUps = { currentTab = MainNavTab.FOLLOW_UPS },
                                onOpenBackupSection = {
                                    reportsSubTab = 1
                                    currentTab = MainNavTab.REPORTS
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RrrBrandTopBar(
                            title = "Rishika Rooms & Rentals",
                            subtitle = "RRR · 13-Room Management"
                        )
                        TabContentArea(
                            currentTab = currentTab,
                            reportsSubTab = reportsSubTab,
                            uiState = uiState,
                            viewModel = viewModel,
                            onRoomClick = { selectedRoomNumber = it.room.roomNumber },
                            onOpenAddTenant = { showAddTenantDialog = it ?: -1 },
                            onOpenReceivePayment = { showPaymentDialogForTenant = -1L },
                            onOpenOverdueFollowUps = { currentTab = MainNavTab.FOLLOW_UPS },
                            onOpenBackupSection = {
                                reportsSubTab = 1
                                currentTab = MainNavTab.REPORTS
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Global New Tenant Dialog
    showAddTenantDialog?.let { initRoom ->
        val vacantRooms = uiState.roomTiles.filter { it.tenant == null }.map { it.room }
        TenantFormDialog(
            vacantRooms = vacantRooms,
            initialRoomNumber = initRoom.takeIf { it > 0 },
            editingTenant = null,
            onSave = { name, mob, rNum, rent, entry, kType, kNum, kPath, kName, notes ->
                viewModel.addTenant(name, mob, rNum, rent, entry, kType, kNum, kPath, kName, notes) {
                    showAddTenantDialog = null
                }
            },
            onDismiss = { showAddTenantDialog = null }
        )
    }

    // Global Single "Payment Received" Dialog
    showPaymentDialogForTenant?.let { initTid ->
        PaymentReceivedDialog(
            tenants = uiState.tenants,
            initialTenantId = initTid.takeIf { it > 0L },
            onPreviewAllocation = { tid, amt -> viewModel.previewPaymentAllocation(tid, amt) },
            onConfirmPayment = { tid, amt, date, mode, remarks ->
                viewModel.receivePayment(tid, amt, date, mode, remarks) { receipt ->
                    showPaymentDialogForTenant = null
                    newlyCreatedReceipt = receipt
                }
            },
            onDismiss = { showPaymentDialogForTenant = null }
        )
    }

    // Immediate Receipt Preview after Payment Saved
    newlyCreatedReceipt?.let { rec ->
        val tName = uiState.tenants.firstOrNull { it.id == rec.tenantId }?.name ?: "Tenant"
        ReceiptViewerDialog(
            receipt = rec,
            tenantName = tName,
            onPrint = { PdfAndPrintHelper.printReceipt(context, rec.toPrintData(tName)) },
            onSavePdf = {
                pendingPdfReceipt = rec
                savePdfLauncher.launch("Receipt_${rec.receiptNumber}.pdf")
            },
            onDelete = {
                newlyCreatedReceipt = null
                deletingNewReceipt = rec
            },
            onDismiss = { newlyCreatedReceipt = null }
        )
    }

    deletingNewReceipt?.let { rec ->
        ConfirmActionDialog(
            title = "Delete Receipt ${rec.receiptNumber}?",
            message = "Delete this receipt and reverse all rent, electricity, and advance allocations?",
            confirmText = "Delete Receipt",
            isDestructive = true,
            onConfirm = {
                val id = rec.id
                deletingNewReceipt = null
                viewModel.deleteReceipt(id)
            },
            onDismiss = { deletingNewReceipt = null }
        )
    }
}

@Composable
private fun NavTabIcon(tab: MainNavTab, selected: Boolean, overdueCount: Int) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab == MainNavTab.FOLLOW_UPS && overdueCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = GoldAccent, contentColor = NavyPrimary) {
                    Text(overdueCount.toString())
                }
            }
        ) {
            Icon(icon, contentDescription = tab.label)
        }
    } else {
        Icon(icon, contentDescription = tab.label)
    }
}

@Composable
private fun TabContentArea(
    currentTab: MainNavTab,
    reportsSubTab: Int,
    uiState: RrrUiState,
    viewModel: MainViewModel,
    onRoomClick: (com.example.ui.viewmodel.RoomTileUi) -> Unit,
    onOpenAddTenant: (Int?) -> Unit,
    onOpenReceivePayment: () -> Unit,
    onOpenOverdueFollowUps: () -> Unit,
    onOpenBackupSection: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (currentTab) {
        MainNavTab.DASHBOARD -> DashboardScreen(
            uiState = uiState,
            onRoomClick = onRoomClick,
            onOpenAddTenant = onOpenAddTenant,
            onOpenReceivePayment = onOpenReceivePayment,
            onGenerateDueRent = { viewModel.generateDueRent() },
            onOpenOverdueFollowUps = onOpenOverdueFollowUps,
            onOpenBackupSection = onOpenBackupSection,
            modifier = modifier
        )

        MainNavTab.RECEIPTS -> PaymentsAndReceiptsScreen(
            receipts = uiState.receipts,
            tenants = uiState.tenants,
            onOpenReceivePayment = onOpenReceivePayment,
            onDeleteReceipt = { viewModel.deleteReceipt(it) },
            onPdfSaved = { viewModel.emitSuccess(it) },
            onPdfError = { viewModel.emitError(it) },
            modifier = modifier
        )

        MainNavTab.ELECTRICITY -> ElectricityScreen(
            rooms = uiState.roomTiles.map { it.room },
            tenants = uiState.tenants,
            bills = uiState.electricityBills,
            defaultRatePaise = uiState.settings.defaultElectricityRatePaise,
            onAddBill = { rNum, tid, from, to, prev, curr, rate, onDone ->
                viewModel.addElectricityBill(rNum, tid, from, to, prev, curr, rate, onDone)
            },
            onEditBill = { bid, from, to, prev, curr, rate, onDone ->
                viewModel.editElectricityBill(bid, from, to, prev, curr, rate, onDone)
            },
            onDeleteBill = { viewModel.deleteElectricityBill(it) },
            modifier = modifier
        )

        MainNavTab.FOLLOW_UPS -> FollowUpsScreen(
            tenants = uiState.tenants,
            rentEntries = uiState.rentEntries,
            electricityBills = uiState.electricityBills,
            followUps = uiState.followUps,
            onAddFollowUp = { tid, fDate, pDate, pend, rem, onDone ->
                viewModel.addFollowUp(tid, fDate, pDate, pend, rem, onDone)
            },
            onEditFollowUp = { fid, fDate, pDate, pend, rem, onDone ->
                viewModel.editFollowUp(fid, fDate, pDate, pend, rem, onDone)
            },
            onMarkCompleted = { viewModel.markFollowUpCompleted(it) },
            onReopen = { viewModel.reopenFollowUp(it) },
            onDeleteFollowUp = { viewModel.deleteFollowUp(it) },
            modifier = modifier
        )

        MainNavTab.REPORTS -> ReportsAndSettingsScreen(
            uiState = uiState,
            initialSubTab = reportsSubTab,
            onUpdateSettings = { rate, days -> viewModel.updateSettings(rate, days) },
            onExportBackupJson = { viewModel.exportBackupJson() },
            onValidateBackupJson = { viewModel.validateBackupJson(it) },
            onRestoreBackup = { viewModel.restoreBackup(it) },
            onDeleteTenant = { viewModel.deleteTenant(it) },
            onDeleteRent = { viewModel.deleteRentEntry(it) },
            onDeleteBill = { viewModel.deleteElectricityBill(it) },
            onDeleteReceipt = { viewModel.deleteReceipt(it) },
            onDeleteAllData = { viewModel.deleteAllData() },
            onSuccessMsg = { viewModel.emitSuccess(it) },
            onErrorMsg = { viewModel.emitError(it) },
            modifier = modifier
        )
    }
}
