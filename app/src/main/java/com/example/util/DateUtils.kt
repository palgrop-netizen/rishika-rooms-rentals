package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SimpleDate(val year: Int, val month: Int, val day: Int) : Comparable<SimpleDate> {
    override fun compareTo(other: SimpleDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun toIso(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    fun toYearMonth(): String = String.format(Locale.US, "%04d-%02d", year, month)
}

object DateUtils {
    private val monthShortNames = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun todayIso(): String {
        val cal = Calendar.getInstance()
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun todayDisplay(): String = formatDisplayDate(todayIso())

    fun parseIsoDate(iso: String): SimpleDate? {
        val trimmed = iso.trim()
        val parts = trimmed.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (y < 1970 || y > 2100 || m < 1 || m > 12) return null
        val maxD = daysInMonth(y, m)
        if (d < 1 || d > maxD) return null
        return SimpleDate(y, m, d)
    }

    fun isValidIsoDate(iso: String): Boolean = parseIsoDate(iso) != null

    fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }

    /**
     * Preserves the original entry anniversary day across month-end boundaries.
     * Example:
     *   2026-01-31 + 0 -> 2026-01-31
     *   2026-01-31 + 1 -> 2026-02-28
     *   2026-01-31 + 2 -> 2026-03-31
     *   2024-01-31 + 1 -> 2024-02-29
     */
    fun addMonthsPreservingDay(entryDateIso: String, monthsToAdd: Int): SimpleDate? {
        val base = parseIsoDate(entryDateIso) ?: return null
        val totalMonths = (base.year * 12) + (base.month - 1) + monthsToAdd
        val targetYear = Math.floorDiv(totalMonths, 12)
        val targetMonth = Math.floorMod(totalMonths, 12) + 1
        val maxDay = daysInMonth(targetYear, targetMonth)
        val targetDay = minOf(base.day, maxDay)
        return SimpleDate(targetYear, targetMonth, targetDay)
    }

    /**
     * Returns all due cycle indices (0, 1, 2, ...) whose anniversary dueDate <= asOfIso.
     */
    fun dueCycleIndices(entryDateIso: String, asOfIso: String): List<Int> {
        val entry = parseIsoDate(entryDateIso) ?: return emptyList()
        val asOf = parseIsoDate(asOfIso) ?: return emptyList()
        if (entry > asOf) return emptyList()
        val result = mutableListOf<Int>()
        var cycle = 0
        while (cycle < 600) {
            val due = addMonthsPreservingDay(entryDateIso, cycle) ?: break
            if (due > asOf) break
            result.add(cycle)
            cycle++
        }
        return result
    }

    fun formatDisplayDate(iso: String): String {
        val d = parseIsoDate(iso) ?: return iso
        return String.format(Locale.US, "%02d %s %04d", d.day, monthShortNames[d.month - 1], d.year)
    }

    fun formatMonthYear(yyyyMm: String): String {
        val parts = yyyyMm.split("-")
        if (parts.size != 2) return yyyyMm
        val y = parts[0].toIntOrNull() ?: return yyyyMm
        val m = parts[1].toIntOrNull() ?: return yyyyMm
        if (m < 1 || m > 12) return yyyyMm
        return "${monthShortNames[m - 1]} $y"
    }

    fun formatTimestamp(epochMs: Long): String {
        if (epochMs <= 0L) return "Never"
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        return sdf.format(Date(epochMs))
    }

    fun epochDaysSince(epochMs: Long, nowMs: Long = System.currentTimeMillis()): Long {
        if (epochMs <= 0L) return Long.MAX_VALUE
        val diff = nowMs - epochMs
        return diff / (1000L * 60L * 60L * 24L)
    }
}
