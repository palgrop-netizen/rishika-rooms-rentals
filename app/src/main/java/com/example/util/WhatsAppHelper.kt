package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

object WhatsAppHelper {
    fun buildElectricityReminderMessage(
        roomNumber: Int,
        tenantName: String,
        periodFrom: String,
        periodTo: String,
        unitsConsumed: Double,
        ratePerUnitPaise: Long,
        billAmountPaise: Long,
        pendingAmountPaise: Long
    ): String {
        return buildString {
            appendLine("*Rishika Rooms & Rentals (RRR)*")
            appendLine("Electricity Bill Reminder")
            appendLine("------------------------------")
            appendLine("Room Number: Room $roomNumber")
            appendLine("Tenant Name: $tenantName")
            appendLine("Billing Period: ${DateUtils.formatDisplayDate(periodFrom)} to ${DateUtils.formatDisplayDate(periodTo)}")
            appendLine("Units Consumed: ${MoneyUtils.formatUnits(unitsConsumed)} units")
            appendLine("Rate Per Unit: ${MoneyUtils.formatRupees(ratePerUnitPaise)}")
            appendLine("Total Bill Amount: ${MoneyUtils.formatRupees(billAmountPaise)}")
            appendLine("Pending Amount: ${MoneyUtils.formatRupees(pendingAmountPaise)}")
            appendLine("------------------------------")
            append("Please clear the pending amount at your earliest convenience. Thank you!")
        }
    }

    fun buildFollowUpReminderMessage(
        roomNumber: Int,
        tenantName: String,
        promisedDate: String,
        pendingAmountPaise: Long,
        remarks: String
    ): String {
        return buildString {
            appendLine("*Rishika Rooms & Rentals (RRR)*")
            appendLine("Payment Follow-Up Reminder")
            appendLine("Room Number: Room $roomNumber")
            appendLine("Tenant Name: $tenantName")
            appendLine("Promised Date: ${DateUtils.formatDisplayDate(promisedDate)}")
            appendLine("Pending Amount: ${MoneyUtils.formatRupees(pendingAmountPaise)}")
            if (remarks.isNotBlank()) {
                appendLine("Note: $remarks")
            }
            append("Thank you!")
        }
    }

    fun sendMessage(context: Context, message: String) {
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(whatsappIntent)
        } catch (_: ActivityNotFoundException) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            val chooser = Intent.createChooser(fallback, "Share Reminder via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }
}
