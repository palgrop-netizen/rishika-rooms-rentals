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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.db.FollowUpEntity
import com.example.data.db.RentEntryEntity
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
fun FollowUpsScreen(
    tenants: List<TenantEntity>,
    rentEntries: List<RentEntryEntity>,
    electricityBills: List<ElectricityBillEntity>,
    followUps: List<FollowUpEntity>,
    onAddFollowUp: (
        tenantId: Long,
        followUpDate: String,
        promisedDate: String,
        pendingAmountPaise: Long,
        remarks: String,
        onDone: () -> Unit
    ) -> Unit,
    onEditFollowUp: (
        followUpId: Long,
        followUpDate: String,
        promisedDate: String,
        pendingAmountPaise: Long,
        remarks: String,
        onDone: () -> Unit
    ) -> Unit,
    onMarkCompleted: (Long) -> Unit,
    onReopen: (Long) -> Unit,
    onDeleteFollowUp: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tenantMap = remember(tenants) { tenants.associateBy { it.id } }
    val pendingByTenant = remember(tenants, rentEntries, electricityBills) {
        tenants.associate { t ->
            val r = rentEntries.filter { it.tenantId == t.id }.sumOf { it.pendingPaise }
            val e = electricityBills.filter { it.tenantId == t.id }.sumOf { it.pendingPaise }
            t.id to (r + e)
        }
    }

    var selectedStatus by remember { mutableStateOf("PENDING") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingFollowUp by remember { mutableStateOf<FollowUpEntity?>(null) }
    var deletingFollowUp by remember { mutableStateOf<FollowUpEntity?>(null) }

    val today = DateUtils.todayIso()
    val pendingList = remember(followUps) { followUps.filter { it.status == "PENDING" } }
    val completedList = remember(followUps) { followUps.filter { it.status == "COMPLETED" } }
    val displayed = if (selectedStatus == "PENDING") pendingList else completedList
    val overdueCount = remember(pendingList, today) {
        pendingList.count { it.promisedPaymentDate <= today }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_followup_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add Follow-Up")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedStatus == "PENDING",
                    onClick = { selectedStatus = "PENDING" },
                    label = {
                        val badge = if (overdueCount > 0) " ($overdueCount overdue)" else ""
                        Text("Pending (${pendingList.size})$badge")
                    },
                    modifier = Modifier.testTag("followup_tab_pending")
                )
                FilterChip(
                    selected = selectedStatus == "COMPLETED",
                    onClick = { selectedStatus = "COMPLETED" },
                    label = { Text("Completed (${completedList.size})") },
                    modifier = Modifier.testTag("followup_tab_completed")
                )
            }
        }

        if (displayed.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = null,
                    tint = NavyPrimary.copy(alpha = 0.45f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (selectedStatus == "PENDING") "No pending follow-ups" else "No completed follow-ups",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Track payment promises and overdue follow-ups separately from rent reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayed, key = { it.id }) { fu ->
                    val tenantName = tenantMap[fu.tenantId]?.name ?: "Tenant #${fu.tenantId}"
                    val isOverdue = fu.status == "PENDING" && fu.promisedPaymentDate <= today
                    FollowUpCard(
                        followUp = fu,
                        tenantName = tenantName,
                        isOverdue = isOverdue,
                        onToggleStatus = {
                            if (fu.status == "PENDING") onMarkCompleted(fu.id) else onReopen(fu.id)
                        },
                        onEdit = { editingFollowUp = fu },
                        onDelete = { deletingFollowUp = fu },
                        onWhatsApp = {
                            val msg = WhatsAppHelper.buildFollowUpReminderMessage(
                                roomNumber = fu.roomNumber,
                                tenantName = tenantName,
                                promisedDate = fu.promisedPaymentDate,
                                pendingAmountPaise = fu.pendingAmountPaise,
                                remarks = fu.remarks
                            )
                            WhatsAppHelper.sendMessage(context, msg)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        FollowUpFormDialog(
            tenants = tenants,
            pendingByTenant = pendingByTenant,
            initialTenantId = null,
            editingFollowUp = null,
            onSave = { tenantId, fDate, pDate, pendingPaise, remarks ->
                onAddFollowUp(tenantId, fDate, pDate, pendingPaise, remarks) {
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingFollowUp?.let { fu ->
        FollowUpFormDialog(
            tenants = tenants,
            pendingByTenant = pendingByTenant,
            initialTenantId = fu.tenantId,
            editingFollowUp = fu,
            onSave = { _, fDate, pDate, pendingPaise, remarks ->
                onEditFollowUp(fu.id, fDate, pDate, pendingPaise, remarks) {
                    editingFollowUp = null
                }
            },
            onDismiss = { editingFollowUp = null }
        )
    }

    deletingFollowUp?.let { fu ->
        ConfirmActionDialog(
            title = "Delete Follow-Up?",
            message = "Delete follow-up record for Room ${fu.roomNumber} (${DateUtils.formatDisplayDate(fu.promisedPaymentDate)})?",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                val id = fu.id
                deletingFollowUp = null
                onDeleteFollowUp(id)
            },
            onDismiss = { deletingFollowUp = null }
        )
    }
}

@Composable
fun FollowUpCard(
    followUp: FollowUpEntity,
    tenantName: String,
    isOverdue: Boolean,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val isCompleted = followUp.status == "COMPLETED"
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, if (isOverdue) StatusRedBorder.copy(alpha = 0.6f) else OutlineSubtle),
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
                        text = "$tenantName · Room ${followUp.roomNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Follow-Up: ${DateUtils.formatDisplayDate(followUp.followUpDate)} · Promised: ${DateUtils.formatDisplayDate(followUp.promisedPaymentDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue) StatusRedText else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusPill(
                    text = when {
                        isCompleted -> "COMPLETED"
                        isOverdue -> "OVERDUE"
                        else -> "PENDING"
                    },
                    bgColor = when {
                        isCompleted -> StatusGreenBg
                        isOverdue -> StatusRedBg
                        else -> GoldLight
                    },
                    borderColor = when {
                        isCompleted -> StatusGreenBorder
                        isOverdue -> StatusRedBorder
                        else -> GoldDark
                    },
                    textColor = when {
                        isCompleted -> StatusGreenText
                        isOverdue -> StatusRedText
                        else -> GoldDark
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pending at Follow-Up", style = MaterialTheme.typography.bodySmall)
                MoneyText(
                    text = MoneyUtils.formatRupees(followUp.pendingAmountPaise),
                    color = NavyPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (followUp.remarks.isNotBlank()) {
                Text(
                    text = "Remarks: ${followUp.remarks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleStatus,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("followup_toggle_${followUp.id}")
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.Replay else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isCompleted) "Reopen" else "Complete", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("followup_edit_${followUp.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit follow-up", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(
                    onClick = onWhatsApp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("followup_whatsapp_${followUp.id}")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Send reminder", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRedBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("followup_delete_${followUp.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete follow-up", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun FollowUpFormDialog(
    tenants: List<TenantEntity>,
    pendingByTenant: Map<Long, Long>,
    initialTenantId: Long? = null,
    editingFollowUp: FollowUpEntity? = null,
    onSave: (
        tenantId: Long,
        followUpDate: String,
        promisedDate: String,
        pendingAmountPaise: Long,
        remarks: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sortedTenants = remember(tenants) {
        tenants.sortedWith(compareByDescending<TenantEntity> { it.isActive }.thenBy { it.currentRoomNumber })
    }
    var selectedTenant by remember(sortedTenants, initialTenantId, editingFollowUp) {
        mutableStateOf(
            sortedTenants.firstOrNull { it.id == (editingFollowUp?.tenantId ?: initialTenantId) }
                ?: sortedTenants.firstOrNull()
        )
    }
    var followUpDate by remember(editingFollowUp) {
        mutableStateOf(editingFollowUp?.followUpDate ?: DateUtils.todayIso())
    }
    var promisedDate by remember(editingFollowUp) {
        mutableStateOf(editingFollowUp?.promisedPaymentDate ?: DateUtils.todayIso())
    }
    var pendingInput by remember(editingFollowUp, selectedTenant?.id) {
        val paise = editingFollowUp?.pendingAmountPaise
            ?: selectedTenant?.let { pendingByTenant[it.id] ?: 0L }
            ?: 0L
        mutableStateOf(MoneyUtils.formatRupeesInput(paise))
    }
    var remarks by remember(editingFollowUp) {
        mutableStateOf(editingFollowUp?.remarks ?: "")
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (editingFollowUp == null) "Add Follow-Up" else "Edit Follow-Up",
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
                    Text(errorMsg!!, color = StatusRedText, style = MaterialTheme.typography.bodySmall)
                }
                if (sortedTenants.isEmpty()) {
                    Text("No tenants available. Add a tenant first.")
                } else {
                    if (editingFollowUp == null) {
                        DropdownField(
                            label = "Tenant *",
                            options = sortedTenants,
                            selectedOption = selectedTenant,
                            optionLabel = { "${it.name} — Room ${it.currentRoomNumber}" },
                            onOptionSelected = { t ->
                                selectedTenant = t
                                pendingInput = MoneyUtils.formatRupeesInput(pendingByTenant[t.id] ?: 0L)
                            },
                            testTag = "followup_tenant_dropdown"
                        )
                    } else {
                        OutlinedTextField(
                            value = "${selectedTenant?.name ?: ""} — Room ${editingFollowUp.roomNumber}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tenant & Room") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    DatePickerField(
                        label = "Follow-Up Date *",
                        isoDate = followUpDate,
                        onDateSelected = { followUpDate = it },
                        testTag = "followup_date_field"
                    )

                    DatePickerField(
                        label = "Promised Payment Date *",
                        isoDate = promisedDate,
                        onDateSelected = { promisedDate = it },
                        testTag = "followup_promised_date"
                    )

                    OutlinedTextField(
                        value = pendingInput,
                        onValueChange = { pendingInput = it; errorMsg = null },
                        label = { Text("Pending Amount at Follow-Up (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("followup_pending_input")
                    )

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("followup_remarks_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = sortedTenants.isNotEmpty() && selectedTenant != null,
                onClick = {
                    val t = selectedTenant
                    val amt = MoneyUtils.parseRupeesToPaise(pendingInput)
                    when {
                        t == null -> errorMsg = "Select a tenant."
                        amt == null -> errorMsg = "Enter a valid pending amount."
                        else -> onSave(t.id, followUpDate, promisedDate, amt, remarks)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("followup_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
