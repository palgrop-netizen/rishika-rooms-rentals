package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.db.RoomEntity
import com.example.data.db.TenantEntity
import com.example.data.repository.PaymentAllocationPreview
import com.example.ui.components.DatePickerField
import com.example.ui.components.DropdownField
import com.example.ui.components.MoneyText
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.StatusGreenText
import com.example.ui.theme.StatusRedText
import com.example.util.DateUtils
import com.example.util.KycFileHelper
import com.example.util.MoneyUtils

val KYC_TYPES = listOf("Aadhaar Card", "PAN Card", "Voter ID", "Driving Licence", "Other")
val PAYMENT_MODES = listOf("Cash", "UPI", "Bank Transfer", "Other")

@Composable
fun TenantFormDialog(
    vacantRooms: List<RoomEntity>,
    initialRoomNumber: Int? = null,
    editingTenant: TenantEntity? = null,
    onSave: (
        name: String,
        mobile: String,
        roomNumber: Int,
        monthlyRentPaise: Long,
        entryDate: String,
        kycType: String,
        kycNumber: String,
        kycDocPath: String?,
        kycDocName: String?,
        notes: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(editingTenant) { mutableStateOf(editingTenant?.name ?: "") }
    var mobile by remember(editingTenant) { mutableStateOf(editingTenant?.mobileNumber ?: "") }
    var selectedRoomNum by remember(editingTenant, initialRoomNumber, vacantRooms) {
        mutableStateOf(
            editingTenant?.currentRoomNumber
                ?: initialRoomNumber
                ?: vacantRooms.firstOrNull()?.roomNumber
                ?: 1
        )
    }
    var monthlyRentInput by remember(editingTenant) {
        mutableStateOf(editingTenant?.let { MoneyUtils.formatRupeesInput(it.monthlyRentPaise) } ?: "")
    }
    var entryDate by remember(editingTenant) {
        mutableStateOf(editingTenant?.entryDate ?: DateUtils.todayIso())
    }
    var kycType by remember(editingTenant) {
        mutableStateOf(editingTenant?.kycType ?: KYC_TYPES.first())
    }
    var kycNumber by remember(editingTenant) {
        mutableStateOf(editingTenant?.kycNumber ?: "")
    }
    var kycDocPath by remember(editingTenant) {
        mutableStateOf(editingTenant?.kycDocumentPath)
    }
    var kycDocName by remember(editingTenant) {
        mutableStateOf(editingTenant?.kycDocumentName)
    }
    var notes by remember(editingTenant) {
        mutableStateOf(editingTenant?.notes ?: "")
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun handlePickedUri(uri: Uri?) {
        if (uri == null) return
        val saved = KycFileHelper.copyIntoPrivateStorage(context, uri)
        if (saved != null) {
            kycDocPath = saved.internalPath
            kycDocName = saved.displayName
        } else {
            errorMsg = "Could not copy the selected KYC file."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = ::handlePickedUri
    )

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = ::handlePickedUri
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingTenant == null) "Allot Room / New Tenant" else "Edit Tenant Details",
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
                    Text(
                        text = errorMsg!!,
                        color = StatusRedText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Tenant Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tenant_name_input")
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it; errorMsg = null },
                    label = { Text("Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tenant_mobile_input")
                )

                if (editingTenant == null) {
                    val roomNums = vacantRooms.map { it.roomNumber }
                    DropdownField(
                        label = "Room Number *",
                        options = roomNums,
                        selectedOption = selectedRoomNum.takeIf { it in roomNums },
                        optionLabel = { "Room $it (Vacant)" },
                        onOptionSelected = { selectedRoomNum = it },
                        testTag = "tenant_room_dropdown"
                    )
                } else {
                    OutlinedTextField(
                        value = "Room ${editingTenant.currentRoomNumber}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Current Room") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = monthlyRentInput,
                    onValueChange = { monthlyRentInput = it; errorMsg = null },
                    label = { Text("Monthly Rent (Rs.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tenant_rent_input")
                )

                DatePickerField(
                    label = "Entry Date (Rent Anniversary) *",
                    isoDate = entryDate,
                    onDateSelected = { entryDate = it },
                    testTag = "tenant_entry_date"
                )

                DropdownField(
                    label = "KYC Document Type *",
                    options = KYC_TYPES,
                    selectedOption = kycType,
                    optionLabel = { it },
                    onOptionSelected = { kycType = it },
                    testTag = "tenant_kyc_type_dropdown"
                )

                OutlinedTextField(
                    value = kycNumber,
                    onValueChange = { kycNumber = it; errorMsg = null },
                    label = { Text("Exact KYC Document Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tenant_kyc_number_input")
                )

                // Zero-permission Photo Picker & SAF Document Picker for KYC upload
                Surface(
                    color = NavySurfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "KYC Document Upload (Private Storage)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NavyPrimary
                        )
                        if (!kycDocName.isNullOrBlank()) {
                            Text(
                                text = "Attached: $kycDocName",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusGreenText
                            )
                        } else {
                            Text(
                                text = "No file attached yet (optional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("kyc_pick_photo_button")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text("Photo")
                            }
                            OutlinedButton(
                                onClick = {
                                    docPickerLauncher.launch(arrayOf("application/pdf", "image/*"))
                                },
                                modifier = Modifier.weight(1f).testTag("kyc_pick_doc_button")
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text("PDF/Doc")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("tenant_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rentPaise = MoneyUtils.parseRupeesToPaise(monthlyRentInput)
                    when {
                        name.isBlank() -> errorMsg = "Please enter tenant name."
                        mobile.isBlank() -> errorMsg = "Please enter mobile number."
                        editingTenant == null && vacantRooms.none { it.roomNumber == selectedRoomNum } ->
                            errorMsg = "No vacant room available."
                        rentPaise == null || rentPaise <= 0L ->
                            errorMsg = "Enter a valid monthly rent greater than 0."
                        !DateUtils.isValidIsoDate(entryDate) ->
                            errorMsg = "Please select a valid entry date."
                        kycNumber.isBlank() ->
                            errorMsg = "Please enter the exact KYC document number."
                        else -> onSave(
                            name.trim(),
                            mobile.trim(),
                            selectedRoomNum,
                            rentPaise,
                            entryDate,
                            kycType,
                            kycNumber.trim(),
                            kycDocPath,
                            kycDocName,
                            notes.trim()
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("tenant_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("tenant_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PaymentReceivedDialog(
    tenants: List<TenantEntity>,
    initialTenantId: Long? = null,
    onPreviewAllocation: suspend (tenantId: Long, amountPaise: Long) -> PaymentAllocationPreview,
    onConfirmPayment: (
        tenantId: Long,
        amountPaise: Long,
        paymentDate: String,
        paymentMode: String,
        remarks: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val activeFirst = remember(tenants) {
        tenants.sortedWith(compareByDescending<TenantEntity> { it.isActive }.thenBy { it.currentRoomNumber })
    }
    var selectedTenant by remember(activeFirst, initialTenantId) {
        mutableStateOf(
            activeFirst.firstOrNull { it.id == initialTenantId } ?: activeFirst.firstOrNull()
        )
    }
    var amountInput by remember { mutableStateOf("") }
    var paymentDate by remember { mutableStateOf(DateUtils.todayIso()) }
    var paymentMode by remember { mutableStateOf(PAYMENT_MODES.first()) }
    var remarks by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<PaymentAllocationPreview?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val parsedAmountPaise = remember(amountInput) { MoneyUtils.parseRupeesToPaise(amountInput) ?: 0L }

    LaunchedEffect(selectedTenant?.id, parsedAmountPaise) {
        val t = selectedTenant
        if (t != null) {
            preview = onPreviewAllocation(t.id, parsedAmountPaise)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Payment Received", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = StatusRedText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (activeFirst.isEmpty()) {
                    Text("No tenants available. Add a tenant first.")
                } else {
                    DropdownField(
                        label = "Select Tenant *",
                        options = activeFirst,
                        selectedOption = selectedTenant,
                        optionLabel = { t ->
                            val statusTag = if (t.isActive) "Room ${t.currentRoomNumber}" else "Vacated (Room ${t.currentRoomNumber})"
                            "${t.name} — $statusTag"
                        },
                        onOptionSelected = { selectedTenant = it },
                        testTag = "payment_tenant_dropdown"
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it; errorMsg = null },
                        label = { Text("Amount Received (Rs.) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                    )

                    DatePickerField(
                        label = "Payment Date *",
                        isoDate = paymentDate,
                        onDateSelected = { paymentDate = it },
                        testTag = "payment_date_field"
                    )

                    DropdownField(
                        label = "Payment Mode *",
                        options = PAYMENT_MODES,
                        selectedOption = paymentMode,
                        optionLabel = { it },
                        onOptionSelected = { paymentMode = it },
                        testTag = "payment_mode_dropdown"
                    )

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks (Optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("payment_remarks_input")
                    )

                    preview?.let { p ->
                        Surface(
                            color = GoldLight,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldDark.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Auto-Allocation Preview (Rent → Electricity → Advance)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                                PreviewRow("Current Rent Pending", MoneyUtils.formatRupees(p.totalPendingRentPaise))
                                PreviewRow("Current Electricity Pending", MoneyUtils.formatRupees(p.totalPendingElectricityPaise))
                                PreviewRow("Existing Advance Balance", MoneyUtils.formatRupees(p.currentAdvancePaise))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                PreviewRow("1. Rent Settled", MoneyUtils.formatRupees(p.rentToSettlePaise), StatusGreenText)
                                PreviewRow("2. Electricity Settled", MoneyUtils.formatRupees(p.electricityToSettlePaise), StatusGreenText)
                                PreviewRow("3. Advance Added", MoneyUtils.formatRupees(p.advanceToAddPaise), NavyPrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = activeFirst.isNotEmpty(),
                onClick = {
                    val tenant = selectedTenant
                    val amt = MoneyUtils.parseRupeesToPaise(amountInput)
                    when {
                        tenant == null -> errorMsg = "Please select a tenant."
                        amt == null || amt <= 0L -> errorMsg = "Enter a valid amount greater than 0."
                        !DateUtils.isValidIsoDate(paymentDate) -> errorMsg = "Select a valid payment date."
                        else -> onConfirmPayment(tenant.id, amt, paymentDate, paymentMode, remarks.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("payment_save_button")
            ) {
                Text("Save Payment")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("payment_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PreviewRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        MoneyText(text = value, color = valueColor)
    }
}

@Composable
fun TransferRoomDialog(
    tenant: TenantEntity,
    vacantRooms: List<RoomEntity>,
    onConfirmTransfer: (toRoomNumber: Int, transferDate: String, remarks: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRoom by remember(vacantRooms) { mutableStateOf(vacantRooms.firstOrNull()?.roomNumber) }
    var transferDate by remember { mutableStateOf(DateUtils.todayIso()) }
    var remarks by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Tenant Room", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Transferring ${tenant.name} from Room ${tenant.currentRoomNumber}. All rent, electricity, payment, KYC, and follow-up records remain preserved with this tenant.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = StatusRedText, style = MaterialTheme.typography.bodySmall)
                }
                if (vacantRooms.isEmpty()) {
                    Text("No vacant rooms available for transfer.", color = StatusRedText)
                } else {
                    DropdownField(
                        label = "Destination Room *",
                        options = vacantRooms.map { it.roomNumber },
                        selectedOption = selectedRoom,
                        optionLabel = { "Room $it (Vacant)" },
                        onOptionSelected = { selectedRoom = it },
                        testTag = "transfer_room_dropdown"
                    )
                    DatePickerField(
                        label = "Transfer Date *",
                        isoDate = transferDate,
                        onDateSelected = { transferDate = it },
                        testTag = "transfer_date_field"
                    )
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = vacantRooms.isNotEmpty() && selectedRoom != null,
                onClick = {
                    val dest = selectedRoom
                    if (dest == null) {
                        errorMsg = "Select destination room."
                    } else {
                        onConfirmTransfer(dest, transferDate, remarks)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("transfer_confirm_button")
            ) {
                Text("Confirm Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun VacateTenantDialog(
    tenant: TenantEntity,
    onConfirmVacate: (vacatedDate: String) -> Unit,
    onDismiss: () -> Unit
) {
    var vacateDate by remember { mutableStateOf(DateUtils.todayIso()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vacate Tenant", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Mark ${tenant.name} as vacated from Room ${tenant.currentRoomNumber}? Room ${tenant.currentRoomNumber} will become Vacant, and all historical financial & KYC records will remain preserved.",
                    style = MaterialTheme.typography.bodyMedium
                )
                DatePickerField(
                    label = "Vacate Date",
                    isoDate = vacateDate,
                    onDateSelected = { vacateDate = it },
                    testTag = "vacate_date_field"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmVacate(vacateDate) },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("vacate_confirm_button")
            ) {
                Text("Confirm Vacate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
