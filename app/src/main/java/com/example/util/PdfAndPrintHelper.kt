package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.example.R
import java.io.FileOutputStream
import java.io.OutputStream

data class ReceiptPrintData(
    val receiptNumber: String,
    val tenantName: String,
    val roomNumber: Int,
    val paymentDate: String,
    val totalAmountPaise: Long,
    val paymentMode: String,
    val rentAdjustedPaise: Long,
    val electricityAdjustedPaise: Long,
    val advanceAddedPaise: Long,
    val remarks: String
)

data class ReportPrintData(
    val title: String,
    val subtitle: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val summaryLines: List<String> = emptyList()
)

object PdfAndPrintHelper {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private val navyColor = Color.rgb(11, 33, 58)
    private val goldColor = Color.rgb(212, 175, 55)
    private val slateColor = Color.rgb(71, 85, 105)
    private val lightFill = Color.rgb(244, 246, 249)
    private val borderGray = Color.rgb(203, 213, 225)
    private val darkText = Color.rgb(15, 23, 42)

    fun renderReceiptBitmap(context: Context, receipt: ReceiptPrintData): Bitmap {
        val bmp = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        drawReceiptCanvas(context, Canvas(bmp), receipt)
        return bmp
    }

    fun renderReportBitmap(context: Context, report: ReportPrintData): Bitmap {
        val bmp = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val pages = paginateReport(report)
        drawReportPageCanvas(
            context = context,
            canvas = Canvas(bmp),
            report = report,
            rows = pages.firstOrNull().orEmpty(),
            pageNumber = 1,
            totalPages = pages.size.coerceAtLeast(1),
            isLastPage = pages.size <= 1
        )
        return bmp
    }

    fun writeReceiptPdf(context: Context, outputStream: OutputStream, receipt: ReceiptPrintData) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        drawReceiptCanvas(context, page.canvas, receipt)
        doc.finishPage(page)
        doc.writeTo(outputStream)
        doc.close()
    }

    fun printReceipt(context: Context, receipt: ReceiptPrintData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "RRR_Receipt_${receipt.receiptNumber}"
        printManager.print(jobName, object : PrintDocumentAdapter() {
            private var pdfDoc: PrintedPdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                pdfDoc = PrintedPdfDocument(context, newAttributes)
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val doc = pdfDoc ?: run {
                    callback.onWriteFailed("PDF initialization failed")
                    return
                }
                try {
                    val page = doc.startPage(0)
                    val canvas = page.canvas
                    val scaleX = canvas.width.toFloat() / PAGE_WIDTH.toFloat()
                    val scaleY = canvas.height.toFloat() / PAGE_HEIGHT.toFloat()
                    val scale = minOf(scaleX, scaleY)
                    canvas.save()
                    canvas.scale(scale, scale)
                    drawReceiptCanvas(context, canvas, receipt)
                    canvas.restore()
                    doc.finishPage(page)
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    doc.close()
                    pdfDoc = null
                }
            }
        }, null)
    }

    fun writeReportPdf(context: Context, outputStream: OutputStream, report: ReportPrintData) {
        val doc = PdfDocument()
        val pages = paginateReport(report)
        pages.forEachIndexed { idx, pageRows ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, idx + 1).create()
            val page = doc.startPage(pageInfo)
            drawReportPageCanvas(
                context = context,
                canvas = page.canvas,
                report = report,
                rows = pageRows,
                pageNumber = idx + 1,
                totalPages = pages.size,
                isLastPage = idx == pages.lastIndex
            )
            doc.finishPage(page)
        }
        doc.writeTo(outputStream)
        doc.close()
    }

    fun printReport(context: Context, report: ReportPrintData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "RRR_${report.title.replace(" ", "_")}"
        val pagesData = paginateReport(report)
        printManager.print(jobName, object : PrintDocumentAdapter() {
            private var pdfDoc: PrintedPdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                pdfDoc = PrintedPdfDocument(context, newAttributes)
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(pagesData.size)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val doc = pdfDoc ?: run {
                    callback.onWriteFailed("PDF initialization failed")
                    return
                }
                try {
                    pagesData.forEachIndexed { idx, pageRows ->
                        val page = doc.startPage(idx)
                        val canvas = page.canvas
                        val scale = minOf(
                            canvas.width.toFloat() / PAGE_WIDTH.toFloat(),
                            canvas.height.toFloat() / PAGE_HEIGHT.toFloat()
                        )
                        canvas.save()
                        canvas.scale(scale, scale)
                        drawReportPageCanvas(
                            context = context,
                            canvas = canvas,
                            report = report,
                            rows = pageRows,
                            pageNumber = idx + 1,
                            totalPages = pagesData.size,
                            isLastPage = idx == pagesData.lastIndex
                        )
                        canvas.restore()
                        doc.finishPage(page)
                    }
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    doc.close()
                    pdfDoc = null
                }
            }
        }, null)
    }

    private fun drawReceiptCanvas(context: Context, canvas: Canvas, r: ReceiptPrintData) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)

        // Outer border
        paint.style = Paint.Style.STROKE
        paint.color = borderGray
        paint.strokeWidth = 1.5f
        canvas.drawRect(28f, 28f, PAGE_WIDTH - 28f, PAGE_HEIGHT - 28f, paint)

        // Navy header banner
        paint.style = Paint.Style.FILL
        paint.color = navyColor
        canvas.drawRect(28f, 28f, PAGE_WIDTH - 28f, 128f, paint)

        // Gold accent stripe
        paint.color = goldColor
        canvas.drawRect(28f, 124f, PAGE_WIDTH - 28f, 130f, paint)

        // Logo
        val logo: Bitmap? = BitmapFactory.decodeResource(context.resources, R.drawable.img_rrr_logo)
        if (logo != null) {
            canvas.drawBitmap(logo, null, Rect(44, 44, 112, 112), null)
        }

        // Brand title
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        canvas.drawText("RISHIKA ROOMS & RENTALS", 128f, 70f, paint)

        paint.color = goldColor
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OFFICIAL PAYMENT RECEIPT  |  RRR", 128f, 94f, paint)

        paint.color = Color.rgb(210, 220, 235)
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Generated on ${DateUtils.todayDisplay()}", 128f, 112f, paint)

        var y = 166f
        // Receipt Number & Date bar
        paint.color = lightFill
        canvas.drawRect(44f, y - 22f, PAGE_WIDTH - 44f, y + 18f, paint)
        paint.color = navyColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("Receipt No: ${r.receiptNumber}", 56f, y + 3f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Date: ${DateUtils.formatDisplayDate(r.paymentDate)}", PAGE_WIDTH - 56f, y + 3f, paint)
        paint.textAlign = Paint.Align.LEFT

        y += 52f
        drawKeyValueRow(canvas, paint, "Tenant Name", r.tenantName, y)
        y += 28f
        drawKeyValueRow(canvas, paint, "Room Number", "Room ${r.roomNumber}", y)
        y += 28f
        drawKeyValueRow(canvas, paint, "Payment Mode", r.paymentMode, y)

        y += 34f
        paint.color = navyColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("Payment Allocation Breakdown", 48f, y, paint)
        y += 12f
        paint.color = goldColor
        canvas.drawRect(48f, y, PAGE_WIDTH - 48f, y + 2f, paint)

        y += 28f
        drawKeyValueRow(canvas, paint, "Rent Amount Adjusted", MoneyUtils.formatRupees(r.rentAdjustedPaise), y)
        y += 28f
        drawKeyValueRow(canvas, paint, "Electricity Amount Adjusted", MoneyUtils.formatRupees(r.electricityAdjustedPaise), y)
        y += 28f
        drawKeyValueRow(canvas, paint, "Advance / Credit Added", MoneyUtils.formatRupees(r.advanceAddedPaise), y)

        y += 22f
        paint.color = navyColor
        canvas.drawRect(44f, y, PAGE_WIDTH - 44f, y + 44f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 15f
        canvas.drawText("TOTAL AMOUNT RECEIVED", 58f, y + 27f, paint)
        paint.color = goldColor
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 17f
        canvas.drawText(MoneyUtils.formatRupees(r.totalAmountPaise), PAGE_WIDTH - 58f, y + 27f, paint)
        paint.textAlign = Paint.Align.LEFT

        y += 74f
        if (r.remarks.isNotBlank()) {
            paint.color = slateColor
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Remarks:", 48f, y, paint)
            paint.typeface = Typeface.DEFAULT
            paint.color = darkText
            canvas.drawText(r.remarks.take(85), 115f, y, paint)
            y += 36f
        }

        // Footer signature block
        val footerY = PAGE_HEIGHT - 110f
        paint.color = borderGray
        paint.strokeWidth = 1f
        canvas.drawLine(48f, footerY, PAGE_WIDTH - 48f, footerY, paint)

        paint.color = slateColor
        paint.textSize = 10.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("This is a computer-generated receipt from Rishika Rooms & Rentals.", 48f, footerY + 24f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.color = navyColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Authorized Signatory", PAGE_WIDTH - 48f, footerY + 24f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawKeyValueRow(canvas: Canvas, paint: Paint, label: String, value: String, y: Float) {
        paint.color = slateColor
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 12.5f
        canvas.drawText(label, 52f, y, paint)

        paint.color = darkText
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(value, PAGE_WIDTH - 52f, y, paint)
        paint.textAlign = Paint.Align.LEFT

        paint.color = Color.rgb(230, 236, 243)
        paint.strokeWidth = 0.8f
        canvas.drawLine(52f, y + 9f, PAGE_WIDTH - 52f, y + 9f, paint)
    }

    private fun paginateReport(report: ReportPrintData): List<List<List<String>>> {
        val rowsPerPage = 22
        if (report.rows.isEmpty()) return listOf(emptyList())
        return report.rows.chunked(rowsPerPage)
    }

    private fun drawReportPageCanvas(
        context: Context,
        canvas: Canvas,
        report: ReportPrintData,
        rows: List<List<String>>,
        pageNumber: Int,
        totalPages: Int,
        isLastPage: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)

        // Header
        paint.color = navyColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 84f, paint)
        paint.color = goldColor
        canvas.drawRect(0f, 80f, PAGE_WIDTH.toFloat(), 84f, paint)

        val logo: Bitmap? = BitmapFactory.decodeResource(context.resources, R.drawable.img_rrr_logo)
        if (logo != null) {
            canvas.drawBitmap(logo, null, Rect(24, 16, 72, 64), null)
        }

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 15f
        canvas.drawText("Rishika Rooms & Rentals — ${report.title}", 84f, 38f, paint)
        paint.color = goldColor
        paint.textSize = 10.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(report.subtitle, 84f, 58f, paint)

        val margin = 24f
        val tableWidth = PAGE_WIDTH - (margin * 2)
        val colCount = report.headers.size.coerceAtLeast(1)
        val colWidth = tableWidth / colCount

        var y = 106f
        // Table Header
        paint.color = navyColor
        canvas.drawRect(margin, y, PAGE_WIDTH - margin, y + 24f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        report.headers.forEachIndexed { colIdx, header ->
            val x = margin + (colIdx * colWidth) + 4f
            canvas.drawText(fitText(header, paint, colWidth - 8f), x, y + 16f, paint)
        }

        y += 24f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.2f
        if (rows.isEmpty()) {
            paint.color = slateColor
            canvas.drawText("No records match the selected filters.", margin + 8f, y + 22f, paint)
            y += 32f
        } else {
            rows.forEachIndexed { rowIdx, row ->
                if (rowIdx % 2 == 1) {
                    paint.color = lightFill
                    canvas.drawRect(margin, y, PAGE_WIDTH - margin, y + 22f, paint)
                }
                paint.color = darkText
                row.take(colCount).forEachIndexed { colIdx, cell ->
                    val x = margin + (colIdx * colWidth) + 4f
                    canvas.drawText(fitText(cell, paint, colWidth - 8f), x, y + 15f, paint)
                }
                paint.color = borderGray
                canvas.drawLine(margin, y + 22f, PAGE_WIDTH - margin, y + 22f, paint)
                y += 22f
            }
        }

        if (isLastPage && report.summaryLines.isNotEmpty()) {
            y += 16f
            paint.color = lightFill
            val boxH = (report.summaryLines.size * 16f) + 18f
            canvas.drawRect(margin, y, PAGE_WIDTH - margin, y + boxH, paint)
            paint.color = navyColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            report.summaryLines.forEachIndexed { i, line ->
                canvas.drawText(line, margin + 10f, y + 18f + (i * 16f), paint)
            }
        }

        // Footer
        paint.color = slateColor
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f
        canvas.drawText("Generated ${DateUtils.todayDisplay()}", margin, PAGE_HEIGHT - 18f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page $pageNumber of $totalPages", PAGE_WIDTH - margin, PAGE_HEIGHT - 18f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.length > 1 && paint.measureText("$truncated…") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return if (truncated.isEmpty()) "" else "$truncated…"
    }
}
