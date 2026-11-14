package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.PaymentReceiptEntity
import com.example.data.db.TenantEntity
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.MoneyText
import com.example.ui.components.StatusPill
import com.example.ui.theme.CardSurface
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.StatusGreenText
import com.example.ui.theme.StatusRedBorder
import com.example.util.DateUtils
import com.example.util.MoneyUtils
import com.example.util.PdfAndPrintHelper
import com.example.util.ReceiptPrintData

@Composable
fun PaymentsAndReceiptsScreen(
    receipts: List<PaymentReceiptEntity>,
    tenants: List<TenantEntity>,
    onOpenReceivePayment: () -> Unit,
    onDeleteReceipt: (Long) -> Unit,
    onPdfSaved: (String) -> Unit,
    onPdfError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tenantMap = remember(tenants) { tenants.associateBy { it.id } }
    var searchQuery by remember { mutableStateOf("") }
    var viewingReceipt by remember { mutableStateOf<PaymentReceiptEntity?>(null) }
    var deletingReceipt by remember { mutableStateOf<PaymentReceiptEntity?>(null) }
    var pendingPdfReceipt by remember { mutableStateOf<PaymentReceiptEntity?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val rec = pendingPdfReceipt
        pendingPdfReceipt = null
        if (uri != null && rec != null) {
            val tenantName = tenantMap[rec.tenantId]?.name ?: "Tenant"
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfAndPrintHelper.writeReceiptPdf(context, out, rec.toPrintData(tenantName))
                } ?: error("Cannot open output file.")
            }.onSuccess {
                onPdfSaved("Receipt PDF saved (${rec.receiptNumber}).")
            }.onFailure {
                onPdfError("Failed to save PDF: ${it.message}")
            }
        }
    }

    val filtered = remember(receipts, tenantMap, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) receipts else receipts.filter { r ->
            val tName = tenantMap[r.tenantId]?.name.orEmpty().lowercase()
            r.receiptNumber.lowercase().contains(q) ||
                tName.contains(q) ||
                "room ${r.roomNumber}".contains(q) ||
                r.paymentMode.lowercase().contains(q)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Primary Payment Received CTA + Search
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenReceivePayment,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("main_receive_payment_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Payment Received",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search receipt #, tenant, room, mode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("receipt_search_input")
            )
        }

        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = NavyPrimary.copy(alpha = 0.45f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (receipts.isEmpty()) "No payment receipts yet" else "No matching receipts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap Payment Received above to record a payment and automatically generate an official RRR receipt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { receipt ->
                    val tenantName = tenantMap[receipt.tenantId]?.name ?: "Unknown Tenant"
                    ReceiptCard(
                        receipt = receipt,
                        tenantName = tenantName,
                        onView = { viewingReceipt = receipt },
                        onPrint = {
                            PdfAndPrintHelper.printReceipt(context, receipt.toPrintData(tenantName))
                        },
                        onSavePdf = {
                            pendingPdfReceipt = receipt
                            savePdfLauncher.launch("Receipt_${receipt.receiptNumber}.pdf")
                        },
                        onDelete = { deletingReceipt = receipt }
                    )
                }
            }
        }
    }

    viewingReceipt?.let { rec ->
        val tenantName = tenantMap[rec.tenantId]?.name ?: "Tenant"
        ReceiptViewerDialog(
            receipt = rec,
            tenantName = tenantName,
            onPrint = { PdfAndPrintHelper.printReceipt(context, rec.toPrintData(tenantName)) },
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
            message = "Deleting this receipt (${MoneyUtils.formatRupees(rec.totalAmountPaise)}) will reverse its Rent (${MoneyUtils.formatRupees(rec.rentAdjustedPaise)}), Electricity (${MoneyUtils.formatRupees(rec.electricityAdjustedPaise)}), and Advance (${MoneyUtils.formatRupees(rec.advanceAddedPaise)}) allocations and restore the exact pending balances.",
            confirmText = "Delete Receipt",
            isDestructive = true,
            onConfirm = {
                deletingReceipt = null
                onDeleteReceipt(rec.id)
            },
            onDismiss = { deletingReceipt = null }
        )
    }
}

@Composable
fun ReceiptCard(
    receipt: PaymentReceiptEntity,
    tenantName: String,
    onView: () -> Unit,
    onPrint: () -> Unit,
    onSavePdf: () -> Unit,
    onDelete: () -> Unit
) {
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
                        text = receipt.receiptNumber,
                        style = MaterialTheme.typography.labelLarge,
                        color = NavyPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$tenantName · Room ${receipt.roomNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    MoneyText(
                        text = MoneyUtils.formatRupees(receipt.totalAmountPaise),
                        color = StatusGreenText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${DateUtils.formatDisplayDate(receipt.paymentDate)} · ${receipt.paymentMode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = NavySurfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AllocationMiniStat("Rent", receipt.rentAdjustedPaise)
                    AllocationMiniStat("Electricity", receipt.electricityAdjustedPaise)
                    AllocationMiniStat("Advance", receipt.advanceAddedPaise)
                }
            }

            if (receipt.remarks.isNotBlank()) {
                Text(
                    text = "Remarks: ${receipt.remarks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onView,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("receipt_view_${receipt.id}")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("View", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onPrint,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("receipt_print_${receipt.id}")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Print", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onSavePdf,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("receipt_pdf_${receipt.id}")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRedBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("receipt_delete_${receipt.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete receipt", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AllocationMiniStat(label: String, paise: Long) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(text = MoneyUtils.formatRupees(paise), fontSize = 12.sp)
    }
}

@Composable
fun ReceiptViewerDialog(
    receipt: PaymentReceiptEntity,
    tenantName: String,
    onPrint: () -> Unit,
    onSavePdf: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Branded RRR Receipt Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavyPrimary)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_rrr_logo),
                            contentDescription = "RRR Logo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RISHIKA ROOMS & RENTALS",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "OFFICIAL PAYMENT RECEIPT",
                                color = GoldAccent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = receipt.receiptNumber,
                        bgColor = GoldLight,
                        borderColor = GoldDark,
                        textColor = GoldDark
                    )
                    Text(
                        text = DateUtils.formatDisplayDate(receipt.paymentDate),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider()
                ReceiptLine("Tenant Name", tenantName)
                ReceiptLine("Room Number", "Room ${receipt.roomNumber}")
                ReceiptLine("Payment Mode", receipt.paymentMode)
                HorizontalDivider()
                ReceiptLine("Rent Adjusted", MoneyUtils.formatRupees(receipt.rentAdjustedPaise))
                ReceiptLine("Electricity Adjusted", MoneyUtils.formatRupees(receipt.electricityAdjustedPaise))
                ReceiptLine("Advance/Credit Added", MoneyUtils.formatRupees(receipt.advanceAddedPaise))
                HorizontalDivider()

                Surface(
                    color = NavyPrimary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL RECEIVED",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        MoneyText(
                            text = MoneyUtils.formatRupees(receipt.totalAmountPaise),
                            color = GoldAccent,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (receipt.remarks.isNotBlank()) {
                    Text(
                        text = "Remarks: ${receipt.remarks}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPrint,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.weight(1f).testTag("viewer_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Print")
                    }
                    OutlinedButton(
                        onClick = onSavePdf,
                        modifier = Modifier.weight(1f).testTag("viewer_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save PDF")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = StatusRedBorder)
            ) {
                Text("Delete Receipt")
            }
        }
    )
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

fun PaymentReceiptEntity.toPrintData(tenantName: String) = ReceiptPrintData(
    receiptNumber = receiptNumber,
    tenantName = tenantName,
    roomNumber = roomNumber,
    paymentDate = paymentDate,
    totalAmountPaise = totalAmountPaise,
    paymentMode = paymentMode,
    rentAdjustedPaise = rentAdjustedPaise,
    electricityAdjustedPaise = electricityAdjustedPaise,
    advanceAddedPaise = advanceAddedPaise,
    remarks = remarks
)
