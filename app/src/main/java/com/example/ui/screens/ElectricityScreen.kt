package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ElectricityBillEntity
import com.example.data.db.RoomEntity
import com.example.data.db.TenantEntity
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownField
import com.example.ui.components.MoneyText
import com.example.ui.components.StatusPill
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
import com.example.util.DateUtils
import com.example.util.MoneyUtils
import com.example.util.WhatsAppHelper

@Composable
fun ElectricityScreen(
    rooms: List<RoomEntity>,
    tenants: List<TenantEntity>,
    bills: List<ElectricityBillEntity>,
    defaultRatePaise: Long,
    onAddBill: (
        roomNumber: Int,
        tenantId: Long?,
        periodFrom: String,
        periodTo: String,
        prevReading: Double,
        currReading: Double,
        ratePaise: Long,
        onDone: () -> Unit
    ) -> Unit,
    onEditBill: (
        billId: Long,
        periodFrom: String,
        periodTo: String,
        prevReading: Double,
        currReading: Double,
        ratePaise: Long,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteBill: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tenantMap = remember(tenants) { tenants.associateBy { it.id } }
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, PAID
    var roomFilter by remember { mutableStateOf<Int?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<ElectricityBillEntity?>(null) }
    var deletingBill by remember { mutableStateOf<ElectricityBillEntity?>(null) }

    val filteredBills = remember(bills, statusFilter, roomFilter) {
        bills.filter { b ->
            val matchStatus = when (statusFilter) {
                "PENDING" -> b.pendingPaise > 0L
                "PAID" -> b.pendingPaise == 0L
                else -> true
            }
            val matchRoom = roomFilter == null || b.roomNumber == roomFilter
            matchStatus && matchRoom
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("add_electricity_bill_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Electricity Bill")
                }

                Surface(
                    color = GoldLight,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GoldDark.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("Saved Rate", style = MaterialTheme.typography.labelSmall, color = GoldDark)
                        MoneyText(
                            text = "${MoneyUtils.formatRupees(defaultRatePaise)}/u",
                            color = NavyPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = statusFilter == "ALL",
                    onClick = { statusFilter = "ALL" },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = statusFilter == "PENDING",
                    onClick = { statusFilter = "PENDING" },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = statusFilter == "PAID",
                    onClick = { statusFilter = "PAID" },
                    label = { Text("Paid") }
                )
            }

            val roomOptions = listOf<Int?>(null) + (1..13).toList()
            DropdownField(
                label = "Filter by Room",
                options = roomOptions,
                selectedOption = roomFilter,
                optionLabel = { if (it == null) "All 13 Rooms" else "Room $it" },
                onOptionSelected = { roomFilter = it },
                testTag = "electricity_room_filter"
            )
        }

        if (filteredBills.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = NavyPrimary.copy(alpha = 0.45f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("No electricity bills found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap Add Electricity Bill above to record room meter readings and calculate charges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredBills, key = { it.id }) { bill ->
                    val tenantName = tenantMap[bill.tenantId]?.name ?: "Tenant #${bill.tenantId}"
                    ElectricityBillCard(
                        bill = bill,
                        tenantName = tenantName,
                        onEdit = { editingBill = bill },
                        onDelete = { deletingBill = bill },
                        onWhatsApp = {
                            val msg = WhatsAppHelper.buildElectricityReminderMessage(
                                roomNumber = bill.roomNumber,
                                tenantName = tenantName,
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
    }

    if (showAddDialog) {
        ElectricityBillDialog(
            rooms = rooms,
            tenants = tenants,
            bills = bills,
            defaultRatePaise = defaultRatePaise,
            initialRoomNumber = roomFilter,
            editingBill = null,
            onSave = { roomNum, tenantId, from, to, prev, curr, rate ->
                onAddBill(roomNum, tenantId, from, to, prev, curr, rate) {
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingBill?.let { bill ->
        ElectricityBillDialog(
            rooms = rooms,
            tenants = tenants,
            bills = bills,
            defaultRatePaise = bill.ratePerUnitPaise,
            initialRoomNumber = bill.roomNumber,
            editingBill = bill,
            onSave = { _, _, from, to, prev, curr, rate ->
                onEditBill(bill.id, from, to, prev, curr, rate) {
                    editingBill = null
                }
            },
            onDismiss = { editingBill = null }
        )
    }

    deletingBill?.let { bill ->
        ConfirmActionDialog(
            title = "Delete Electricity Bill?",
            message = if (bill.amountPaidPaise > 0L) {
                "This bill already has ${MoneyUtils.formatRupees(bill.amountPaidPaise)} paid. Paid bills cannot be deleted directly until the linked payment receipt is deleted first."
            } else {
                "Delete unpaid electricity bill for Room ${bill.roomNumber} (${DateUtils.formatDisplayDate(bill.periodFrom)} - ${DateUtils.formatDisplayDate(bill.periodTo)}, ${MoneyUtils.formatRupees(bill.totalBillPaise)})?"
            },
            confirmText = if (bill.amountPaidPaise > 0L) "Check" else "Delete Bill",
            isDestructive = true,
            onConfirm = {
                val id = bill.id
                deletingBill = null
                onDeleteBill(id)
            },
            onDismiss = { deletingBill = null }
        )
    }
}

@Composable
fun ElectricityBillCard(
    bill: ElectricityBillEntity,
    tenantName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val isPaid = bill.pendingPaise == 0L
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
                        text = "Room ${bill.roomNumber} · $tenantName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${DateUtils.formatDisplayDate(bill.periodFrom)} → ${DateUtils.formatDisplayDate(bill.periodTo)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = bill.status,
                    bgColor = if (isPaid) StatusGreenBg else StatusRedBg,
                    borderColor = if (isPaid) StatusGreenBorder else StatusRedBorder,
                    textColor = if (isPaid) StatusGreenText else StatusRedText
                )
            }

            Surface(
                color = NavySurfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(8.dp),
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
                        Text(
                            "Meter: ${MoneyUtils.formatUnits(bill.previousReading)} → ${MoneyUtils.formatUnits(bill.currentReading)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "${MoneyUtils.formatUnits(bill.unitsConsumed)} units @ ${MoneyUtils.formatRupees(bill.ratePerUnitPaise)}/u",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BillMiniMoney("Total Bill", bill.totalBillPaise)
                        BillMiniMoney("Paid", bill.amountPaidPaise, StatusGreenText)
                        BillMiniMoney("Pending", bill.pendingPaise, if (bill.pendingPaise > 0) StatusRedText else StatusGreenText)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("bill_edit_${bill.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onWhatsApp,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("bill_whatsapp_${bill.id}")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("WhatsApp")
                }
                OutlinedButton(
                    onClick = onDelete,
                    enabled = bill.amountPaidPaise == 0L,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRedBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("bill_delete_${bill.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete bill", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BillMiniMoney(
    label: String,
    paise: Long,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(text = MoneyUtils.formatRupees(paise), color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ElectricityBillDialog(
    rooms: List<RoomEntity>,
    tenants: List<TenantEntity>,
    bills: List<ElectricityBillEntity>,
    defaultRatePaise: Long,
    initialRoomNumber: Int? = null,
    editingBill: ElectricityBillEntity? = null,
    onSave: (
        roomNumber: Int,
        tenantId: Long?,
        periodFrom: String,
        periodTo: String,
        prevReading: Double,
        currReading: Double,
        ratePaise: Long
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val occupiedRooms = remember(rooms) { rooms.filter { it.currentTenantId != null } }
    val defaultRoom = editingBill?.roomNumber
        ?: initialRoomNumber?.takeIf { rn -> rooms.any { it.roomNumber == rn && it.currentTenantId != null } }
        ?: occupiedRooms.firstOrNull()?.roomNumber
        ?: 1

    var selectedRoomNum by remember(editingBill, defaultRoom) { mutableStateOf(defaultRoom) }

    fun latestReadingForRoom(roomNum: Int): Double {
        val roomBills = bills.filter { it.roomNumber == roomNum && it.id != editingBill?.id }
        return roomBills.maxOfOrNull { it.currentReading }
            ?: rooms.firstOrNull { it.roomNumber == roomNum }?.lastMeterReading
            ?: 0.0
    }

    var prevReadingInput by remember(editingBill, selectedRoomNum) {
        mutableStateOf(
            if (editingBill != null) MoneyUtils.formatUnits(editingBill.previousReading)
            else MoneyUtils.formatUnits(latestReadingForRoom(selectedRoomNum))
        )
    }
    var currReadingInput by remember(editingBill) {
        mutableStateOf(editingBill?.let { MoneyUtils.formatUnits(it.currentReading) } ?: "")
    }
    var rateInput by remember(editingBill, defaultRatePaise) {
        mutableStateOf(
            MoneyUtils.formatRupeesInput(editingBill?.ratePerUnitPaise ?: defaultRatePaise)
        )
    }
    var periodFrom by remember(editingBill) {
        mutableStateOf(editingBill?.periodFrom ?: DateUtils.todayIso())
    }
    var periodTo by remember(editingBill) {
        mutableStateOf(editingBill?.periodTo ?: DateUtils.todayIso())
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val selectedRoom = rooms.firstOrNull { it.roomNumber == selectedRoomNum }
    val selectedTenant = if (editingBill != null) {
        tenants.firstOrNull { it.id == editingBill.tenantId }
    } else {
        selectedRoom?.currentTenantId?.let { tid -> tenants.firstOrNull { it.id == tid } }
    }

    val prevVal = prevReadingInput.toDoubleOrNull()
    val currVal = currReadingInput.toDoubleOrNull()
    val ratePaise = MoneyUtils.parseRupeesToPaise(rateInput)
    val previewUnits = if (prevVal != null && currVal != null && currVal >= prevVal) currVal - prevVal else null
    val previewAmount = if (previewUnits != null && ratePaise != null) {
        MoneyUtils.calculateBillPaise(previewUnits, ratePaise)
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (editingBill == null) "New Electricity Bill" else "Edit Electricity Bill",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = StatusRedText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }

                if (editingBill == null) {
                    val selectableRooms = if (occupiedRooms.isNotEmpty()) occupiedRooms else rooms
                    DropdownField(
                        label = "Room *",
                        options = selectableRooms.map { it.roomNumber },
                        selectedOption = selectedRoomNum,
                        optionLabel = { rn ->
                            val r = rooms.firstOrNull { it.roomNumber == rn }
                            val tName = r?.currentTenantId?.let { tid -> tenants.firstOrNull { it.id == tid }?.name }
                            if (tName != null) "Room $rn — $tName" else "Room $rn (Vacant)"
                        },
                        onOptionSelected = { rn ->
                            selectedRoomNum = rn
                            prevReadingInput = MoneyUtils.formatUnits(latestReadingForRoom(rn))
                            errorMsg = null
                        },
                        testTag = "elec_room_dropdown"
                    )
                } else {
                    OutlinedTextField(
                        value = "Room ${editingBill.roomNumber} — ${selectedTenant?.name ?: ""}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Room & Tenant") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedTenant == null) {
                    Text(
                        "Room $selectedRoomNum is currently vacant. Allot a tenant before billing.",
                        color = StatusRedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                DatePickerField(
                    label = "Billing Period From *",
                    isoDate = periodFrom,
                    onDateSelected = { periodFrom = it; errorMsg = null },
                    testTag = "elec_period_from"
                )

                DatePickerField(
                    label = "Billing Period To *",
                    isoDate = periodTo,
                    onDateSelected = { periodTo = it; errorMsg = null },
                    testTag = "elec_period_to"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prevReadingInput,
                        onValueChange = { prevReadingInput = it; errorMsg = null },
                        label = { Text("Prev Reading *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("elec_prev_reading")
                    )
                    OutlinedTextField(
                        value = currReadingInput,
                        onValueChange = { currReadingInput = it; errorMsg = null },
                        label = { Text("Curr Reading *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("elec_curr_reading")
                    )
                }

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it; errorMsg = null },
                    label = { Text("Rate Per Unit (Rs.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("elec_rate_input")
                )

                if (previewUnits != null && previewAmount != null) {
                    Surface(
                        color = GoldLight,
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
                                Text("Units Consumed:", style = MaterialTheme.typography.bodySmall)
                                Text("${MoneyUtils.formatUnits(previewUnits)} units", fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Calculated Bill:", style = MaterialTheme.typography.bodySmall)
                                MoneyText(
                                    text = MoneyUtils.formatRupees(previewAmount),
                                    color = NavyPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedTenant != null,
                onClick = {
                    val p = prevReadingInput.toDoubleOrNull()
                    val c = currReadingInput.toDoubleOrNull()
                    val r = MoneyUtils.parseRupeesToPaise(rateInput)
                    val dFrom = DateUtils.parseIsoDate(periodFrom)
                    val dTo = DateUtils.parseIsoDate(periodTo)
                    when {
                        selectedTenant == null -> errorMsg = "Select an occupied room."
                        dFrom == null || dTo == null -> errorMsg = "Select valid billing dates."
                        dTo < dFrom -> errorMsg = "Billing Period To cannot be before From date."
                        p == null || p < 0.0 -> errorMsg = "Enter a valid previous reading."
                        c == null || c < p -> errorMsg = "Current reading cannot be less than previous reading."
                        r == null || r < 0L -> errorMsg = "Enter a valid rate per unit."
                        else -> onSave(selectedRoomNum, selectedTenant.id, periodFrom, periodTo, p, c, r)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("elec_save_button")
            ) {
                Text("Save Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
