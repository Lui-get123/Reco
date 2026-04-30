package com.example.repartocobro.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.repartocobro.model.Route
import com.example.repartocobro.model.RouteSummary
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExporter {

    companion object {
        private const val PAGE_WIDTH = 595   // A4 width in points
        private const val PAGE_HEIGHT = 842  // A4 height in points
        private const val MARGIN_LEFT = 40f
        private const val MARGIN_RIGHT = 40f
        private const val MARGIN_TOP = 50f
        private const val MARGIN_BOTTOM = 60f
        private const val USABLE_WIDTH = PAGE_WIDTH - 80f // left + right margins
        private const val MAX_Y = PAGE_HEIGHT - MARGIN_BOTTOM

        // Space needed for the totals block (title + 3 lines + spacing)
        private const val TOTALS_BLOCK_HEIGHT = 100f

        // Space each store row takes in the table
        private const val STORE_ROW_HEIGHT = 20f
        private const val TABLE_HEADER_HEIGHT = 24f
    }

    private val currencyFormat = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
        minimumFractionDigits = 0
    }

    fun exportRouteSummary(
        context: Context,
        route: Route,
        collectorName: String,
        summary: RouteSummary
    ): Result<String> {
        return runCatching {
            val document = PdfDocument()
            var pageNumber = 1
            var currentPage = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            var canvas = currentPage.canvas
            var y = MARGIN_TOP

            // --- Paints ---
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(33, 33, 33)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 11f
                color = Color.rgb(100, 100, 100)
            }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.WHITE
            }
            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f
                color = Color.rgb(50, 50, 50)
            }
            val cellPaintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(50, 50, 50)
            }
            val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(33, 33, 33)
            }
            val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 12f
                color = Color.rgb(50, 50, 50)
            }
            val linePaint = Paint().apply {
                color = Color.rgb(200, 200, 200)
                strokeWidth = 0.5f
            }
            val headerBgPaint = Paint().apply {
                color = Color.rgb(55, 71, 133)
                style = Paint.Style.FILL
            }
            val altRowPaint = Paint().apply {
                color = Color.rgb(240, 242, 247)
                style = Paint.Style.FILL
            }
            val pageNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 8f
                color = Color.rgb(150, 150, 150)
                textAlign = Paint.Align.CENTER
            }

            // Helper to start a new page
            fun newPage(): Unit {
                // Draw page number on current page
                canvas.drawText(
                    "Página $pageNumber",
                    PAGE_WIDTH / 2f,
                    (PAGE_HEIGHT - 20f),
                    pageNumPaint
                )
                document.finishPage(currentPage)
                pageNumber++
                currentPage = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = currentPage.canvas
                y = MARGIN_TOP
            }

            // Helper to check if we need a new page
            fun ensureSpace(needed: Float) {
                if (y + needed > MAX_Y) {
                    newPage()
                }
            }

            // === HEADER ===
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            canvas.drawText("📋 Resumen de Cobranza", MARGIN_LEFT, y, titlePaint)
            y += 18f
            canvas.drawText(
                "Ruta: ${route.name}  •  Cobrador: $collectorName  •  ${dateFormat.format(Date())}",
                MARGIN_LEFT,
                y,
                subtitlePaint
            )
            y += 8f

            // Separator line
            canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, linePaint)
            y += 16f

            // === TABLE: Column definitions ===
            // Columns: #, Tienda, Entr.Emp, Entr.Ded, Vend.Emp, Vend.Ded, Cobrado, Fecha
            val colX = floatArrayOf(
                MARGIN_LEFT,           // # (20pt)
                MARGIN_LEFT + 20f,     // Tienda (110pt)
                MARGIN_LEFT + 130f,    // Ent.E (50pt)
                MARGIN_LEFT + 180f,    // Ent.D (50pt)
                MARGIN_LEFT + 230f,    // Vnd.E (50pt)
                MARGIN_LEFT + 280f,    // Vnd.D (50pt)
                MARGIN_LEFT + 330f,    // Cobrado (80pt)
                MARGIN_LEFT + 410f     // Fecha (remaining)
            )
            val tableRight = PAGE_WIDTH - MARGIN_RIGHT

            // --- Table header ---
            ensureSpace(TABLE_HEADER_HEIGHT + STORE_ROW_HEIGHT)
            canvas.drawRect(MARGIN_LEFT, y - 12f, tableRight, y + 10f, headerBgPaint)
            canvas.drawText("#", colX[0] + 4f, y + 4f, headerPaint)
            canvas.drawText("Tienda", colX[1] + 4f, y + 4f, headerPaint)
            canvas.drawText("Ent.E", colX[2] + 4f, y + 4f, headerPaint)
            canvas.drawText("Ent.D", colX[3] + 4f, y + 4f, headerPaint)
            canvas.drawText("Vnd.E", colX[4] + 4f, y + 4f, headerPaint)
            canvas.drawText("Vnd.D", colX[5] + 4f, y + 4f, headerPaint)
            canvas.drawText("Cobrado", colX[6] + 4f, y + 4f, headerPaint)
            canvas.drawText("Fecha", colX[7] + 4f, y + 4f, headerPaint)
            y += 14f

            // Helper to redraw table header on new pages
            fun drawTableHeader() {
                canvas.drawRect(MARGIN_LEFT, y - 12f, tableRight, y + 10f, headerBgPaint)
                canvas.drawText("#", colX[0] + 4f, y + 4f, headerPaint)
                canvas.drawText("Tienda", colX[1] + 4f, y + 4f, headerPaint)
                canvas.drawText("Ent.E", colX[2] + 4f, y + 4f, headerPaint)
                canvas.drawText("Ent.D", colX[3] + 4f, y + 4f, headerPaint)
                canvas.drawText("Vnd.E", colX[4] + 4f, y + 4f, headerPaint)
                canvas.drawText("Vnd.D", colX[5] + 4f, y + 4f, headerPaint)
                canvas.drawText("Cobrado", colX[6] + 4f, y + 4f, headerPaint)
                canvas.drawText("Fecha", colX[7] + 4f, y + 4f, headerPaint)
                y += 14f
            }

            // --- Table rows ---
            summary.stores.forEachIndexed { index, store ->
                // Check if we need a new page (with room for at least one row)
                if (y + STORE_ROW_HEIGHT > MAX_Y) {
                    newPage()
                    drawTableHeader()
                }

                // Alternate row background
                if (index % 2 == 0) {
                    canvas.drawRect(MARGIN_LEFT, y - 10f, tableRight, y + 8f, altRowPaint)
                }

                val rowPaint = cellPaint
                canvas.drawText("${index + 1}", colX[0] + 4f, y + 2f, rowPaint)

                // Truncate store name if too long
                val maxNameWidth = colX[2] - colX[1] - 8f
                var displayName = store.name
                while (rowPaint.measureText(displayName) > maxNameWidth && displayName.length > 3) {
                    displayName = displayName.dropLast(1)
                }
                if (displayName != store.name) displayName += "…"
                canvas.drawText(displayName, colX[1] + 4f, y + 2f, rowPaint)

                canvas.drawText("${store.deliveredEmpanadas}", colX[2] + 4f, y + 2f, rowPaint)
                canvas.drawText("${store.deliveredDeditos}", colX[3] + 4f, y + 2f, rowPaint)
                canvas.drawText("${store.soldEmpanadas}", colX[4] + 4f, y + 2f, rowPaint)
                canvas.drawText("${store.soldDeditos}", colX[5] + 4f, y + 2f, rowPaint)
                canvas.drawText(
                    "\$${currencyFormat.format(store.collectedValue)}",
                    colX[6] + 4f,
                    y + 2f,
                    cellPaintBold
                )
                val dateText = store.collectionDate ?: "—"
                canvas.drawText(dateText, colX[7] + 4f, y + 2f, rowPaint)

                y += STORE_ROW_HEIGHT
            }

            // Bottom border of table
            canvas.drawLine(MARGIN_LEFT, y - 2f, tableRight, y - 2f, linePaint)
            y += 12f

            // === TOTALS SECTION ===
            ensureSpace(TOTALS_BLOCK_HEIGHT)

            // Totals box background
            val totalsBoxPaint = Paint().apply {
                color = Color.rgb(232, 245, 233)
                style = Paint.Style.FILL
            }
            val totalsBoxBorderPaint = Paint().apply {
                color = Color.rgb(76, 175, 80)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            val boxTop = y - 4f
            val boxBottom = y + 76f
            canvas.drawRect(MARGIN_LEFT, boxTop, tableRight, boxBottom, totalsBoxPaint)
            canvas.drawRect(MARGIN_LEFT, boxTop, tableRight, boxBottom, totalsBoxBorderPaint)

            y += 14f
            canvas.drawText("📊 Totales de Ruta", MARGIN_LEFT + 12f, y, totalLabelPaint)
            y += 20f
            canvas.drawText(
                "Empanadas vendidas: ${summary.totalSoldEmpanadas}",
                MARGIN_LEFT + 12f,
                y,
                totalValuePaint
            )
            canvas.drawText(
                "Deditos vendidos: ${summary.totalSoldDeditos}",
                MARGIN_LEFT + 240f,
                y,
                totalValuePaint
            )
            y += 20f
            val totalMoneyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(27, 94, 32)
            }
            canvas.drawText(
                "💰 Total cobrado: \$${currencyFormat.format(summary.totalCollectedMoney)}",
                MARGIN_LEFT + 12f,
                y,
                totalMoneyPaint
            )

            // Draw page number on last page
            canvas.drawText(
                "Página $pageNumber",
                PAGE_WIDTH / 2f,
                (PAGE_HEIGHT - 20f),
                pageNumPaint
            )
            document.finishPage(currentPage)

            // Save the document
            val fileFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName =
                "resumen_${route.name.replace(" ", "_")}_${fileFormat.format(Date())}.pdf"
            val destination = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToDownloadsWithMediaStore(context, fileName) { out -> document.writeTo(out) }
            } else {
                saveToDownloadsLegacy(context, fileName) { out -> document.writeTo(out) }
            }
            document.close()
            destination
        }
    }

    private fun saveToDownloadsWithMediaStore(
        context: Context,
        fileName: String,
        writeBlock: (OutputStream) -> Unit
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear archivo en Descargas")
        resolver.openOutputStream(uri)?.use { out ->
            writeBlock(out)
        } ?: error("No se pudo abrir stream de escritura")
        return uri.toString()
    }

    private fun saveToDownloadsLegacy(
        context: Context,
        fileName: String,
        writeBlock: (OutputStream) -> Unit
    ): String {
        val baseDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        val file = File(baseDir, fileName)
        FileOutputStream(file).use { out ->
            writeBlock(out)
        }
        return file.absolutePath
    }
}
