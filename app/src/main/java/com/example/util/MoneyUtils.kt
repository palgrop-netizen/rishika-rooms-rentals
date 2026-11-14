package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object MoneyUtils {
    private val indianFormat: NumberFormat
        get() = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }

    fun formatRupees(paise: Long, includeSymbol: Boolean = true): String {
        val isNegative = paise < 0
        val absPaise = kotlin.math.abs(paise)
        val rupees = absPaise / 100
        val remPaise = (absPaise % 100).toInt()
        val formattedWhole = indianFormat.format(rupees)
        val amountStr = if (remPaise == 0) {
            formattedWhole
        } else {
            "$formattedWhole.${String.format(Locale.US, "%02d", remPaise)}"
        }
        val prefix = if (isNegative) "-" else ""
        return if (includeSymbol) "${prefix}Rs. $amountStr" else "$prefix$amountStr"
    }

    fun formatRupeesInput(paise: Long): String {
        val rupees = paise / 100
        val rem = (paise % 100).toInt()
        return if (rem == 0) rupees.toString() else String.format(Locale.US, "%d.%02d", rupees, rem)
    }

    fun parseRupeesToPaise(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace("Rs.", "", ignoreCase = true).trim()
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned)
            if (bd < BigDecimal.ZERO) return null
            bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    fun calculateBillPaise(units: Double, ratePerUnitPaise: Long): Long {
        if (units <= 0.0 || ratePerUnitPaise <= 0L) return 0L
        return BigDecimal.valueOf(units)
            .multiply(BigDecimal.valueOf(ratePerUnitPaise))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun formatUnits(units: Double): String {
        val rounded = BigDecimal.valueOf(units).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
        return rounded.toPlainString()
    }
}
