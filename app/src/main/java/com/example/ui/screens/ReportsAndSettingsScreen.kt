package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.db.BackupHelper
import com.example.data.db.BackupPayload
import com.example.data.db.ElectricityBillEntity
import com.example.data.db.PaymentReceiptEntity
import com.example.data.db.RentEntryEntity
import com.example.data.db.TenantEntity
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownField
import com.example.ui.theme.CardSurface
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.StatusRedBorder
import com.example.ui.theme.StatusRedText
import com.example.ui.viewmodel.RrrUiState
import com.example.util.CsvExportHelper
import com.example.util.DateUtils
import com.example.util.MoneyUtils
import com.example.util.PdfAndPrintHelper
import com.example.util.ReportPrintData
import kotlinx.coroutines.launch

enum class ReportKind(val title: String) {
    TENANT("Tenant Report"),
    RENT("Rent Report"),
    ELECTRICITY("Electricity Report"),
    COLLECTION("Payment Collection Report"),
    FOLLOW_UP("Follow-Up Report")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsAndSettingsScreen(
    uiState: RrrUiState,
    initialSubTab: Int = 0, // 0=Reports, 1=Backup & Restore, 2=Settings & Data
    onUpdateSettings: (ratePaise: Long, reminderDays: Int) -> Unit,
    onExportBackupJson: suspend () -> Result<String>,
    onValidateBackupJson: suspend (String) -> Result<BackupPayload>,
    onRestoreBackup: (BackupPayload) -> Unit,
    onDeleteTenant: (Long) -> Unit,
    onDeleteRent: (Long) -> Unit,
    onDeleteBill: (Long) -> Unit,
    onDeleteReceipt: (Long) -> Unit,
    onDeleteAllData: () -> Unit,
    onSuccessMsg: (String) -> Unit,
    onErrorMsg: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }

    Column(modifier = modifier.fillMaxSize()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                label = { Text("Reports") },
                modifier = Modifier.testTag("subtab_reports")
            )
            FilterChip(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                label = { Text("Backup & Restore") },
                modifier = Modifier.testTag("subtab_backup")
            )
            FilterChip(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                label = { Text("Settings & Data") },
                modifier = Modifier.testTag("subtab_settings")
            )
        }

        when (subTab) {
            0 -> ReportsTabContent(uiState, onSuccessMsg, onErrorMsg)
            1 -> BackupTabContent(
                uiState = uiState,
                onExportBackupJson = onExportBackupJson,
                onValidateBackupJson = onValidateBackupJson,
                onRestoreBackup = onRestoreBackup,
                onSuccessMsg = onSuccessMsg,
                onErrorMsg = onErrorMsg
            )
            2 -> SettingsAndDataTabContent(
                uiState = uiState,
                onUpdateSettings = onUpdateSettings,
                onDeleteTenant = onDeleteTenant,
                onDeleteRent = onDeleteRent,
                onDeleteBill = onDeleteBill,
                onDeleteReceipt = onDeleteReceipt,
                onDeleteAllData = onDeleteAllData
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportsTabContent(
    uiState: RrrUiState,
    onSuccessMsg: (String) -> Unit,
    onErrorMsg: (String) -> Unit
) {
    val context = LocalContext.current
    val tenantMap = remember(uiState.tenants) { uiState.tenants.associateBy { it.id } }

    var selectedReport by remember { mutableStateOf(ReportKind.TENANT) }
    var searchQuery by remember { mutableStateOf("") }
    var roomFilter by remember { mutableStateOf<Int?>(null) }
    var statusFilter by remember { mutableStateOf("ALL") }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }

    val receiptDateByRentId = remember(uiState.allocations, uiState.receipts) {
        val recMap = uiState.receipts.associateBy { it.id }
        uiState.allocations
            .filter { it.targetType == "RENT" && it.targetId != null }
            .groupBy { it.targetId!! }
            .mapValues { (_, allocs) ->
                allocs.mapNotNull { recMap[it.paymentId]?.paymentDate }.maxOrNull() ?: "-"
            }
    }

    val reportData = remember(
        selectedReport, uiState, searchQuery, roomFilter, statusFilter, dateFrom, dateTo, receiptDateByRentId
    ) {
        buildReportData(
            kind = selectedReport,
            uiState = uiState,
            tenantMap = tenantMap,
            receiptDateByRentId = receiptDateByRentId,
            search = searchQuery.trim().lowercase(),
            roomFilter = roomFilter,
            statusFilter = statusFilter,
            dateFrom = dateFrom,
            dateTo = dateTo
        )
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val csv = CsvExportHelper.buildCsv(reportData.headers, reportData.rows)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open CSV output stream.")
            }.onSuccess {
                onSuccessMsg("${selectedReport.title} CSV exported.")
            }.onFailure {
                onErrorMsg("CSV export failed: ${it.message}")
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfAndPrintHelper.writeReportPdf(context, out, reportData)
                } ?: error("Could not open PDF output stream.")
            }.onSuccess {
                onSuccessMsg("${selectedReport.title} PDF saved.")
            }.onFailure {
                onErrorMsg("PDF export failed: ${it.message}")
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_lazy_column")
    ) {
        item {
            DropdownField(
                label = "Select Report",
                options = ReportKind.entries,
                selectedOption = selectedReport,
                optionLabel = { it.title },
                onOptionSelected = {
                    selectedReport = it
                    statusFilter = "ALL"
                },
                testTag = "report_kind_dropdown"
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name, room, KYC, remarks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("report_search_input")
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val roomOpts = listOf<Int?>(null) + (1..13).toList()
                DropdownField(
                    label = "Room",
                    options = roomOpts,
                    selectedOption = roomFilter,
                    optionLabel = { if (it == null) "All Rooms" else "Room $it" },
                    onOptionSelected = { roomFilter = it },
                    modifier = Modifier.weight(1f)
                )

                val statusOpts = when (selectedReport) {
                    ReportKind.TENANT -> listOf("ALL", "ACTIVE", "VACATED")
                    ReportKind.RENT -> listOf("ALL", "PENDING", "PARTIAL", "PAID", "WRITTEN_OFF")
                    ReportKind.ELECTRICITY -> listOf("ALL", "PENDING", "PARTIAL", "PAID")
                    ReportKind.COLLECTION -> listOf("ALL", "Cash", "UPI", "Bank Transfer", "Other")
                    ReportKind.FOLLOW_UP -> listOf("ALL", "PENDING", "COMPLETED")
                }
                DropdownField(
                    label = "Filter",
                    options = statusOpts,
                    selectedOption = statusFilter,
                    optionLabel = { if (it == "ALL") "All" else it },
                    onOptionSelected = { statusFilter = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DatePickerField(
                    label = "From Date",
                    isoDate = dateFrom,
                    onDateSelected = { dateFrom = it },
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = "To Date",
                    isoDate = dateTo,
                    onDateSelected = { dateTo = it },
                    modifier = Modifier.weight(1f)
                )
                if (dateFrom.isNotBlank() || dateTo.isNotBlank()) {
                    TextButton(onClick = { dateFrom = ""; dateTo = "" }) {
                        Text("Clear")
                    }
                }
            }
        }

        // Export & Print Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val safeName = selectedReport.title.replace(" ", "_")
                        csvLauncher.launch("RRR_${safeName}_${DateUtils.todayIso()}.csv")
                    },
                    modifier = Modifier.weight(1f).testTag("report_csv_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CSV")
                }
                OutlinedButton(
                    onClick = { PdfAndPrintHelper.printReport(context, reportData) },
                    modifier = Modifier.weight(1f).testTag("report_print_btn")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Print")
                }
                Button(
                    onClick = {
                        val safeName = selectedReport.title.replace(" ", "_")
                        pdfLauncher.launch("RRR_${safeName}_${DateUtils.todayIso()}.pdf")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.weight(1f).testTag("report_pdf_btn")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF")
                }
            }
        }

        // Summary Box
        if (reportData.summaryLines.isNotEmpty()) {
            item {
                Surface(
                    color = GoldLight,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldDark.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${reportData.title} Summary (${reportData.rows.size} rows)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        reportData.summaryLines.forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Report Row Cards
        if (reportData.rows.isEmpty()) {
            item {
                Text(
                    text = "No records match the current search and filters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(reportData.rows.size) { index ->
                val row = reportData.rows[index]
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, OutlineSubtle),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reportData.headers.forEachIndexed { colIdx, header ->
                            val value = row.getOrElse(colIdx) { "" }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun buildReportData(
    kind: ReportKind,
    uiState: RrrUiState,
    tenantMap: Map<Long, TenantEntity>,
    receiptDateByRentId: Map<Long, String>,
    search: String,
    roomFilter: Int?,
    statusFilter: String,
    dateFrom: String,
    dateTo: String
): ReportPrintData {
    fun inDateRange(iso: String): Boolean {
        if (dateFrom.isNotBlank() && iso < dateFrom) return false
        if (dateTo.isNotBlank() && iso > dateTo) return false
        return true
    }

    val subtitle = buildString {
        append("Generated ${DateUtils.todayDisplay()}")
        if (roomFilter != null) append(" · Room $roomFilter")
        if (dateFrom.isNotBlank() || dateTo.isNotBlank()) {
            append(" · ${dateFrom.ifBlank { "Start" }} to ${dateTo.ifBlank { "Today" }}")
        }
    }

    return when (kind) {
        ReportKind.TENANT -> {
            // Note: Uses exact kycNumber and NEVER kycDocumentName!
            val filtered = uiState.tenants.filter { t ->
                val matchRoom = roomFilter == null || t.currentRoomNumber == roomFilter
                val matchStatus = when (statusFilter) {
                    "ACTIVE" -> t.isActive
                    "VACATED" -> !t.isActive
                    else -> true
                }
                val matchDate = inDateRange(t.entryDate)
                val matchSearch = search.isEmpty() ||
                    t.name.lowercase().contains(search) ||
                    t.mobileNumber.lowercase().contains(search) ||
                    t.kycNumber.lowercase().contains(search) ||
                    "room ${t.currentRoomNumber}".contains(search)
                matchRoom && matchStatus && matchDate && matchSearch
            }
            val headers = listOf("Tenant Name", "Room", "Mobile", "Entry Date", "Monthly Rent", "KYC Type", "KYC Number", "Status")
            val rows = filtered.map { t ->
                listOf(
                    t.name,
                    "Room ${t.currentRoomNumber}",
                    t.mobileNumber,
                    DateUtils.formatDisplayDate(t.entryDate),
                    MoneyUtils.formatRupees(t.monthlyRentPaise),
                    t.kycType,
                    t.kycNumber,
                    if (t.isActive) "Active" else "Vacated"
                )
            }
            ReportPrintData(
                title = kind.title,
                subtitle = subtitle,
                headers = headers,
                rows = rows,
                summaryLines = listOf(
                    "Total Tenants Listed: ${filtered.size} (Active: ${filtered.count { it.isActive }}, Vacated: ${filtered.count { !it.isActive }})"
                )
            )
        }

        ReportKind.RENT -> {
            val filtered = uiState.rentEntries.filter { r ->
                val tName = tenantMap[r.tenantId]?.name.orEmpty().lowercase()
                val matchRoom = roomFilter == null || r.roomNumber == roomFilter
                val matchStatus = statusFilter == "ALL" || r.status == statusFilter
                val matchDate = inDateRange(r.dueDate)
                val matchSearch = search.isEmpty() || tName.contains(search) || "room ${r.roomNumber}".contains(search) || r.rentMonth.contains(search)
                matchRoom && matchStatus && matchDate && matchSearch
            }
            val headers = listOf("Tenant", "Room", "Due Date", "Generated", "Received", "Write-Off", "Pending", "Payment Date", "Status")
            val rows = filtered.map { r ->
                val tName = tenantMap[r.tenantId]?.name ?: "Tenant #${r.tenantId}"
                val received = r.amountPaidPaise + r.advanceAppliedPaise
                val payDate = receiptDateByRentId[r.id]?.let { if (it == "-") "-" else DateUtils.formatDisplayDate(it) } ?: "-"
                listOf(
                    tName,
                    "Room ${r.roomNumber}",
                    DateUtils.formatDisplayDate(r.dueDate),
                    MoneyUtils.formatRupees(r.rentAmountPaise),
                    MoneyUtils.formatRupees(received),
                    MoneyUtils.formatRupees(r.writeOffPaise),
                    MoneyUtils.formatRupees(r.pendingPaise),
                    payDate,
                    r.status
                )
            }
            val totalGen = filtered.sumOf { it.rentAmountPaise }
            val totalRec = filtered.sumOf { it.amountPaidPaise + it.advanceAppliedPaise }
            val totalWo = filtered.sumOf { it.writeOffPaise }
            val totalPend = filtered.sumOf { it.pendingPaise }
            ReportPrintData(
                title = kind.title,
                subtitle = subtitle,
                headers = headers,
                rows = rows,
                summaryLines = listOf(
                    "Total Rent Generated: ${MoneyUtils.formatRupees(totalGen)} · Received: ${MoneyUtils.formatRupees(totalRec)}",
                    "Total Write-Off: ${MoneyUtils.formatRupees(totalWo)} · Outstanding: ${MoneyUtils.formatRupees(totalPend)}"
                )
            )
        }

        ReportKind.ELECTRICITY -> {
            val filtered = uiState.electricityBills.filter { b ->
                val tName = tenantMap[b.tenantId]?.name.orEmpty().lowercase()
                val matchRoom = roomFilter == null || b.roomNumber == roomFilter
                val matchStatus = statusFilter == "ALL" || b.status == statusFilter
                val matchDate = inDateRange(b.periodTo)
                val matchSearch = search.isEmpty() || tName.contains(search) || "room ${b.roomNumber}".contains(search)
                matchRoom && matchStatus && matchDate && matchSearch
            }
            val headers = listOf("Room", "Tenant", "Period", "Prev", "Curr", "Units", "Rate", "Bill Amt", "Paid", "Pending")
            val rows = filtered.map { b ->
                val tName = tenantMap[b.tenantId]?.name ?: "Tenant #${b.tenantId}"
                listOf(
                    "Room ${b.roomNumber}",
                    tName,
                    "${DateUtils.formatDisplayDate(b.periodFrom)} - ${DateUtils.formatDisplayDate(b.periodTo)}",
                    MoneyUtils.formatUnits(b.previousReading),
                    MoneyUtils.formatUnits(b.currentReading),
                    MoneyUtils.formatUnits(b.unitsConsumed),
                    MoneyUtils.formatRupees(b.ratePerUnitPaise),
                    MoneyUtils.formatRupees(b.totalBillPaise),
                    MoneyUtils.formatRupees(b.amountPaidPaise),
                    MoneyUtils.formatRupees(b.pendingPaise)
                )
            }
            ReportPrintData(
                title = kind.title,
                subtitle = subtitle,
                headers = headers,
                rows = rows,
                summaryLines = listOf(
                    "Total Units: ${MoneyUtils.formatUnits(filtered.sumOf { it.unitsConsumed })} · Total Billed: ${MoneyUtils.formatRupees(filtered.sumOf { it.totalBillPaise })}",
                    "Total Paid: ${MoneyUtils.formatRupees(filtered.sumOf { it.amountPaidPaise })} · Pending: ${MoneyUtils.formatRupees(filtered.sumOf { it.pendingPaise })}"
                )
            )
        }

        ReportKind.COLLECTION -> {
            // Count actual collection by real payment date; exclude write-offs and unapplied advance from rent collected!
            val filtered = uiState.receipts.filter { rec ->
                val tName = tenantMap[rec.tenantId]?.name.orEmpty().lowercase()
                val matchRoom = roomFilter == null || rec.roomNumber == roomFilter
                val matchMode = statusFilter == "ALL" || rec.paymentMode == statusFilter
                val matchDate = inDateRange(rec.paymentDate)
                val matchSearch = search.isEmpty() ||
                    tName.contains(search) ||
                    rec.receiptNumber.lowercase().contains(search) ||
                    "room ${rec.roomNumber}".contains(search)
                matchRoom && matchMode && matchDate && matchSearch
            }
            val headers = listOf("Payment Date", "Receipt #", "Tenant", "Room", "Mode", "Rent Coll.", "Elec Coll.", "Advance Recd", "Total")
            val rows = filtered.map { rec ->
                val tName = tenantMap[rec.tenantId]?.name ?: "Tenant #${rec.tenantId}"
                listOf(
                    DateUtils.formatDisplayDate(rec.paymentDate),
                    rec.receiptNumber,
                    tName,
                    "Room ${rec.roomNumber}",
                    rec.paymentMode,
                    MoneyUtils.formatRupees(rec.rentAdjustedPaise),
                    MoneyUtils.formatRupees(rec.electricityAdjustedPaise),
                    MoneyUtils.formatRupees(rec.advanceAddedPaise),
                    MoneyUtils.formatRupees(rec.totalAmountPaise)
                )
            }
            val rentColl = filtered.sumOf { it.rentAdjustedPaise }
            val elecColl = filtered.sumOf { it.electricityAdjustedPaise }
            val advColl = filtered.sumOf { it.advanceAddedPaise }
            val totalColl = filtered.sumOf { it.totalAmountPaise }
            ReportPrintData(
                title = kind.title,
                subtitle = subtitle,
                headers = headers,
                rows = rows,
                summaryLines = listOf(
                    "Rent Collected: ${MoneyUtils.formatRupees(rentColl)} · Electricity Collected: ${MoneyUtils.formatRupees(elecColl)}",
                    "Advance/Credit Received: ${MoneyUtils.formatRupees(advColl)} · Total Collected: ${MoneyUtils.formatRupees(totalColl)}"
                )
            )
        }

        ReportKind.FOLLOW_UP -> {
            val filtered = uiState.followUps.filter { fu ->
                val tName = tenantMap[fu.tenantId]?.name.orEmpty().lowercase()
                val matchRoom = roomFilter == null || fu.roomNumber == roomFilter
                val matchStatus = statusFilter == "ALL" || fu.status == statusFilter
                val matchDate = inDateRange(fu.followUpDate)
                val matchSearch = search.isEmpty() || tName.contains(search) || fu.remarks.lowercase().contains(search)
                matchRoom && matchStatus && matchDate && matchSearch
            }.sortedWith(compareByDescending<com.example.data.db.FollowUpEntity> { it.status == "PENDING" }.thenBy { it.promisedPaymentDate })

            val headers = listOf("Status", "Tenant", "Room", "Follow-Up Date", "Promised Date", "Pending Amt", "Remarks")
            val rows = filtered.map { fu ->
                val tName = tenantMap[fu.tenantId]?.name ?: "Tenant #${fu.tenantId}"
                listOf(
                    fu.status,
                    tName,
                    "Room ${fu.roomNumber}",
                    DateUtils.formatDisplayDate(fu.followUpDate),
                    DateUtils.formatDisplayDate(fu.promisedPaymentDate),
                    MoneyUtils.formatRupees(fu.pendingAmountPaise),
                    fu.remarks.ifBlank { "-" }
                )
            }
            val pendCount = filtered.count { it.status == "PENDING" }
            val compCount = filtered.count { it.status == "COMPLETED" }
            ReportPrintData(
                title = kind.title,
                subtitle = subtitle,
                headers = headers,
                rows = rows,
                summaryLines = listOf(
                    "Pending Follow-Ups: $pendCount · Completed Follow-Ups: $compCount"
                )
            )
        }
    }
}

@Composable
private fun BackupTabContent(
    uiState: RrrUiState,
    onExportBackupJson: suspend () -> Result<String>,
    onValidateBackupJson: suspend (String) -> Result<BackupPayload>,
    onRestoreBackup: (BackupPayload) -> Unit,
    onSuccessMsg: (String) -> Unit,
    onErrorMsg: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingRestorePayload by remember { mutableStateOf<BackupPayload?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                onExportBackupJson()
                    .onSuccess { json ->
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(json.toByteArray(Charsets.UTF_8))
                            } ?: error("Cannot write backup file.")
                        }.onSuccess {
                            onSuccessMsg("Full JSON backup exported successfully.")
                        }.onFailure {
                            onErrorMsg("Failed to write backup: ${it.message}")
                        }
                    }
                    .onFailure { onErrorMsg("Backup generation failed: ${it.message}") }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: error("Cannot read selected file.")
                }.onSuccess { rawJson ->
                    onValidateBackupJson(rawJson)
                        .onSuccess { payload -> pendingRestorePayload = payload }
                        .onFailure { onErrorMsg(it.message ?: "Backup validation failed.") }
                }.onFailure {
                    onErrorMsg("Failed to read backup file: ${it.message}")
                }
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, OutlineSubtle),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Backup Status & Version",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text("Backup Format Version: v${BackupHelper.CURRENT_BACKUP_VERSION}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Last Backup: ${DateUtils.formatTimestamp(uiState.settings.lastBackupEpochMs)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Includes: All 13 Rooms, Tenants, Rent Entries, Receipts, Payment Allocations, Electricity Bills, Advance Ledger, Write-Offs, Follow-Ups, Room Transfers, and KYC References.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    exportBackupLauncher.launch("RRR_Backup_${DateUtils.todayIso()}.json")
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("export_backup_btn")
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Full Backup (JSON)")
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("restore_backup_btn")
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Restore Backup from JSON")
            }
        }
    }

    pendingRestorePayload?.let { payload ->
        ConfirmActionDialog(
            title = "Restore Backup?",
            message = "Validated RRR Backup v${payload.backupVersion} (${payload.tenants.size} tenants, ${payload.rentEntries.size} rent entries, ${payload.electricityBills.size} electricity bills, ${payload.paymentReceipts.size} receipts). Restoring will replace current database records cleanly without duplicating transactions.",
            confirmText = "Confirm Restore",
            isDestructive = true,
            onConfirm = {
                val p = payload
                pendingRestorePayload = null
                onRestoreBackup(p)
            },
            onDismiss = { pendingRestorePayload = null }
        )
    }
}

@Composable
private fun SettingsAndDataTabContent(
    uiState: RrrUiState,
    onUpdateSettings: (ratePaise: Long, reminderDays: Int) -> Unit,
    onDeleteTenant: (Long) -> Unit,
    onDeleteRent: (Long) -> Unit,
    onDeleteBill: (Long) -> Unit,
    onDeleteReceipt: (Long) -> Unit,
    onDeleteAllData: () -> Unit
) {
    val tenantMap = remember(uiState.tenants) { uiState.tenants.associateBy { it.id } }
    var rateInput by remember(uiState.settings.defaultElectricityRatePaise) {
        mutableStateOf(MoneyUtils.formatRupeesInput(uiState.settings.defaultElectricityRatePaise))
    }
    var reminderDaysInput by remember(uiState.settings.backupReminderDays) {
        mutableStateOf(uiState.settings.backupReminderDays.toString())
    }
    var settingsError by remember { mutableStateOf<String?>(null) }

    var selectedTenantToDelete by remember(uiState.tenants) { mutableStateOf<TenantEntity?>(uiState.tenants.firstOrNull()) }
    val eligibleUnpaidRents = remember(uiState.rentEntries) {
        uiState.rentEntries.filter { it.amountPaidPaise == 0L && it.advanceAppliedPaise == 0L && it.writeOffPaise == 0L }
    }
    var selectedRentToDelete by remember(eligibleUnpaidRents) { mutableStateOf(eligibleUnpaidRents.firstOrNull()) }

    val eligibleUnpaidBills = remember(uiState.electricityBills) {
        uiState.electricityBills.filter { it.amountPaidPaise == 0L }
    }
    var selectedBillToDelete by remember(eligibleUnpaidBills) { mutableStateOf(eligibleUnpaidBills.firstOrNull()) }
    var selectedReceiptToDelete by remember(uiState.receipts) { mutableStateOf(uiState.receipts.firstOrNull()) }

    var confirmType by remember { mutableStateOf<String?>(null) } // TENANT, RENT, BILL, RECEIPT, ALL

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // App Preferences
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, OutlineSubtle),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Property Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    if (settingsError != null) {
                        Text(settingsError!!, color = StatusRedText, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it; settingsError = null },
                        label = { Text("Default Electricity Rate Per Unit (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_rate_input")
                    )
                    OutlinedTextField(
                        value = reminderDaysInput,
                        onValueChange = { reminderDaysInput = it; settingsError = null },
                        label = { Text("Backup Reminder Interval (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_reminder_days_input")
                    )
                    Button(
                        onClick = {
                            val r = MoneyUtils.parseRupeesToPaise(rateInput)
                            val d = reminderDaysInput.toIntOrNull()
                            when {
                                r == null || r < 0L -> settingsError = "Enter a valid rate."
                                d == null || d < 1 -> settingsError = "Reminder interval must be at least 1 day."
                                else -> onUpdateSettings(r, d)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("settings_save_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Settings")
                    }
                }
            }
        }

        // Granular Data Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, OutlineSubtle),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Data Management (Targeted Deletes)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)

                    // Delete selected tenant
                    DropdownField(
                        label = "Select Tenant to Delete",
                        options = uiState.tenants,
                        selectedOption = selectedTenantToDelete,
                        optionLabel = { "${it.name} (Room ${it.currentRoomNumber})" },
                        onOptionSelected = { selectedTenantToDelete = it },
                        enabled = uiState.tenants.isNotEmpty()
                    )
                    OutlinedButton(
                        enabled = selectedTenantToDelete != null,
                        onClick = { confirmType = "TENANT" },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRedBorder),
                        modifier = Modifier.fillMaxWidth().testTag("dm_delete_tenant_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete Selected Tenant")
                    }

                    HorizontalDivider()

                    // Delete selected unpaid rent entry
                    DropdownField(
                        label = "Select Unpaid Rent Entry",
                        options = eligibleUnpaidRents,
                        selectedOption = selectedRentToDelete,
                        optionLabel = { r ->
                            val t = tenantMap[r.tenantId]?.name ?: "Tenant"
                            "$t · Room ${r.roomNumber} · ${DateUtils.formatMonthYear(r.rentMonth)} (${MoneyUtils.formatRupees(r.rentAmountPaise)})"
                        },
                        onOptionSelected = { selectedRentToDelete = it },
                        enabled = eligibleUnpaidRents.isNotEmpty()
                    )
                    OutlinedButton(
                        enabled = selectedRentToDelete != null,
                        onClick = { confirmType = "RENT" },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRedBorder),
                        modifier = Modifier.fillMaxWidth().testTag("dm_delete_rent_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete Selected Rent Entry")
                    }

                    HorizontalDivider()

                    // Delete selected unpaid electricity bill
                    DropdownField(
                        label = "Select Unpaid Electricity Bill",
                        options = eligibleUnpaidBills,
                        selectedOption = selectedBillToDelete,
                        optionLabel = { b ->
                            "Room ${b.roomNumber} · ${DateUtils.formatDisplayDate(b.periodFrom)} (${MoneyUtils.formatRupees(b.totalBillPaise)})"
                        },
                        onOptionSelected = { selectedBillToDelete = it },
                        enabled = eligibleUnpaidBills.isNotEmpty()
                    )
                    OutlinedButton(
                        enabled = selectedBillToDelete != null,
                        onClick = { confirmType = "BILL" },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRedBorder),
                        modifier = Modifier.fillMaxWidth().testTag("dm_delete_bill_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete Selected Electricity Bill")
                    }

                    HorizontalDivider()

                    // Delete selected receipt
                    DropdownField(
                        label = "Select Payment Receipt",
                        options = uiState.receipts,
                        selectedOption = selectedReceiptToDelete,
                        optionLabel = { rec ->
                            val t = tenantMap[rec.tenantId]?.name ?: "Tenant"
                            "${rec.receiptNumber} · $t · ${MoneyUtils.formatRupees(rec.totalAmountPaise)}"
                        },
                        onOptionSelected = { selectedReceiptToDelete = it },
                        enabled = uiState.receipts.isNotEmpty()
                    )
                    OutlinedButton(
                        enabled = selectedReceiptToDelete != null,
                        onClick = { confirmType = "RECEIPT" },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRedBorder),
                        modifier = Modifier.fillMaxWidth().testTag("dm_delete_receipt_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete Selected Receipt")
                    }
                }
            }
        }

        // Danger Zone: Delete All Application Data
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, StatusRedBorder.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Danger Zone", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = StatusRedText)
                    Text(
                        "Delete all tenants, rent entries, electricity bills, receipts, advance records, write-offs, and follow-ups. We recommend exporting a JSON backup first.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { confirmType = "ALL" },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRedBorder),
                        modifier = Modifier.fillMaxWidth().testTag("dm_delete_all_btn")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Delete All Entries")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    when (confirmType) {
        "TENANT" -> selectedTenantToDelete?.let { t ->
            ConfirmActionDialog(
                title = "Delete Tenant ${t.name}?",
                message = "Deletes ${t.name} and all their linked financial records without affecting other tenants.",
                confirmText = "Confirm Delete",
                isDestructive = true,
                onConfirm = { confirmType = null; onDeleteTenant(t.id) },
                onDismiss = { confirmType = null }
            )
        }
        "RENT" -> selectedRentToDelete?.let { r ->
            ConfirmActionDialog(
                title = "Delete Rent Entry?",
                message = "Delete ${DateUtils.formatMonthYear(r.rentMonth)} rent entry (${MoneyUtils.formatRupees(r.rentAmountPaise)})? Auto-generated entries deleted here will not regenerate.",
                confirmText = "Confirm Delete",
                isDestructive = true,
                onConfirm = { confirmType = null; onDeleteRent(r.id) },
                onDismiss = { confirmType = null }
            )
        }
        "BILL" -> selectedBillToDelete?.let { b ->
            ConfirmActionDialog(
                title = "Delete Electricity Bill?",
                message = "Delete unpaid bill for Room ${b.roomNumber} (${MoneyUtils.formatRupees(b.totalBillPaise)})?",
                confirmText = "Confirm Delete",
                isDestructive = true,
                onConfirm = { confirmType = null; onDeleteBill(b.id) },
                onDismiss = { confirmType = null }
            )
        }
        "RECEIPT" -> selectedReceiptToDelete?.let { rec ->
            ConfirmActionDialog(
                title = "Delete Receipt ${rec.receiptNumber}?",
                message = "Deleting ${rec.receiptNumber} (${MoneyUtils.formatRupees(rec.totalAmountPaise)}) will safely reverse all its rent, electricity, and advance allocations.",
                confirmText = "Confirm Delete",
                isDestructive = true,
                onConfirm = { confirmType = null; onDeleteReceipt(rec.id) },
                onDismiss = { confirmType = null }
            )
        }
        "ALL" -> ConfirmActionDialog(
            title = "Delete All Application Data?",
            message = "Warning: This permanently deletes all tenants, rent entries, electricity bills, receipts, advance balances, write-offs, and follow-ups, resetting all 13 rooms to Vacant. Please make sure you have exported a JSON backup before proceeding.",
            confirmText = "Confirm Delete All",
            isDestructive = true,
            onConfirm = { confirmType = null; onDeleteAllData() },
            onDismiss = { confirmType = null }
        )
    }
}
