package com.example.util

object CsvExportHelper {
    fun buildCsv(headers: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.appendLine(headers.joinToString(",") { escapeCsv(it) })
        for (row in rows) {
            sb.appendLine(row.joinToString(",") { escapeCsv(it) })
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') ||
            value.contains('\n') || value.contains('\r')
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }
}
