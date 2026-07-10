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
import com.example.repartocobro.model.DEDITO_PRICE
import com.example.repartocobro.model.EMPANADA_PRICE
import com.example.repartocobro.model.Route
import com.example.repartocobro.model.RouteSummary
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfExportResult(
    val localPath: String,
    val fileName: String,
    val pdfBytes: ByteArray
)

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
    ): Result<PdfExportResult> {
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
            canvas.drawText("Resumen de Cobranza", MARGIN_LEFT, y, titlePaint)
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
            // Columns: #, Tienda, Ent.E, Ent.D, Vnd.E, Vnd.D, T.Entreg, Cobrado, Deuda, Novedad
            val colStarts = floatArrayOf(
                MARGIN_LEFT,           // # (20pt)
                MARGIN_LEFT + 20f,     // Tienda (85pt)
                MARGIN_LEFT + 105f,    // Ent.E (38pt)
                MARGIN_LEFT + 143f,    // Ent.D (38pt)
                MARGIN_LEFT + 181f,    // Vnd.E (38pt)
                MARGIN_LEFT + 219f,    // Vnd.D (38pt)
                MARGIN_LEFT + 257f,    // T.Entreg (55pt)
                MARGIN_LEFT + 312f,    // Cobrado (55pt)
                MARGIN_LEFT + 367f,    // Deuda (55pt)
                MARGIN_LEFT + 422f     // Novedad (remaining ~100pt)
            )
            val tableRight = PAGE_WIDTH - MARGIN_RIGHT

            // Paint for centered header text
            val headerCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
            }
            // Paint for centered cell text
            val cellCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f
                color = Color.rgb(50, 50, 50)
                textAlign = Paint.Align.CENTER
            }
            val cellCenterBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(50, 50, 50)
                textAlign = Paint.Align.CENTER
            }

            // Column midpoints for centered text
            val colMids = floatArrayOf(
                (colStarts[0] + colStarts[1]) / 2f,    // # mid
                (colStarts[1] + colStarts[2]) / 2f,    // Tienda mid
                (colStarts[2] + colStarts[3]) / 2f,    // Ent.E mid
                (colStarts[3] + colStarts[4]) / 2f,    // Ent.D mid
                (colStarts[4] + colStarts[5]) / 2f,    // Vnd.E mid
                (colStarts[5] + colStarts[6]) / 2f,    // Vnd.D mid
                (colStarts[6] + colStarts[7]) / 2f,    // T.Entreg mid
                (colStarts[7] + colStarts[8]) / 2f,    // Cobrado mid
                (colStarts[8] + colStarts[9]) / 2f,    // Deuda mid
                (colStarts[9] + tableRight) / 2f       // Novedad mid
            )

            // --- Table header ---
            ensureSpace(TABLE_HEADER_HEIGHT + STORE_ROW_HEIGHT)
            canvas.drawRect(MARGIN_LEFT, y - 14f, tableRight, y + 14f, headerBgPaint)
            canvas.drawText("#", colMids[0], y + 3f, headerCenterPaint)
            canvas.drawText("Tienda", colMids[1], y + 3f, headerCenterPaint)
            canvas.drawText("Ent.E", colMids[2], y + 3f, headerCenterPaint)
            canvas.drawText("Ent.D", colMids[3], y + 3f, headerCenterPaint)
            canvas.drawText("Vnd.E", colMids[4], y + 3f, headerCenterPaint)
            canvas.drawText("Vnd.D", colMids[5], y + 3f, headerCenterPaint)
            canvas.drawText("T.Entreg", colMids[6], y + 3f, headerCenterPaint)
            canvas.drawText("Cobrado", colMids[7], y + 3f, headerCenterPaint)
            canvas.drawText("Deuda", colMids[8], y + 3f, headerCenterPaint)
            canvas.drawText("Novedad", colMids[9], y + 3f, headerCenterPaint)
            y += 18f

            // Helper to redraw table header on new pages
            fun drawTableHeader() {
                canvas.drawRect(MARGIN_LEFT, y - 14f, tableRight, y + 14f, headerBgPaint)
                canvas.drawText("#", colMids[0], y + 3f, headerCenterPaint)
                canvas.drawText("Tienda", colMids[1], y + 3f, headerCenterPaint)
                canvas.drawText("Ent.E", colMids[2], y + 3f, headerCenterPaint)
                canvas.drawText("Ent.D", colMids[3], y + 3f, headerCenterPaint)
                canvas.drawText("Vnd.E", colMids[4], y + 3f, headerCenterPaint)
                canvas.drawText("Vnd.D", colMids[5], y + 3f, headerCenterPaint)
                canvas.drawText("T.Entreg", colMids[6], y + 3f, headerCenterPaint)
                canvas.drawText("Cobrado", colMids[7], y + 3f, headerCenterPaint)
                canvas.drawText("Deuda", colMids[8], y + 3f, headerCenterPaint)
                canvas.drawText("Novedad", colMids[9], y + 3f, headerCenterPaint)
                y += 18f
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

                canvas.drawText("${index + 1}", colMids[0], y + 2f, cellCenterPaint)

                // Truncate store name if too long
                val maxNameWidth = colStarts[2] - colStarts[1] - 8f
                var displayName = store.name
                while (cellPaint.measureText(displayName) > maxNameWidth && displayName.length > 3) {
                    displayName = displayName.dropLast(1)
                }
                if (displayName != store.name) displayName += "…"
                canvas.drawText(displayName, colStarts[1] + 4f, y + 2f, cellPaint)

                canvas.drawText("${store.deliveredEmpanadas}", colMids[2], y + 2f, cellCenterPaint)
                canvas.drawText("${store.deliveredDeditos}", colMids[3], y + 2f, cellCenterPaint)
                canvas.drawText("${store.soldEmpanadas}", colMids[4], y + 2f, cellCenterPaint)
                canvas.drawText("${store.soldDeditos}", colMids[5], y + 2f, cellCenterPaint)
                canvas.drawText(
                    "\$${currencyFormat.format(store.deliveredValue)}",
                    colMids[6], y + 2f, cellCenterBoldPaint
                )
                canvas.drawText(
                    "\$${currencyFormat.format(store.collectedValue)}",
                    colMids[7], y + 2f, cellCenterBoldPaint
                )
                canvas.drawText(
                    if (store.pendingDebtTotal > 0) "\$${currencyFormat.format(store.pendingDebtTotal)}" else "",
                    colMids[8], y + 2f, cellCenterBoldPaint
                )

                // Novedad / Observaciones
                val observations = store.observations ?: ""
                val maxObsWidth = tableRight - colStarts[9] - 8f
                var displayObs = observations
                while (cellPaint.measureText(displayObs) > maxObsWidth && displayObs.length > 3) {
                    displayObs = displayObs.dropLast(1)
                }
                if (displayObs != observations) displayObs += ".."
                canvas.drawText(displayObs, colStarts[9] + 4f, y + 2f, cellPaint)

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
            val boxBottom = y + 96f
            canvas.drawRect(MARGIN_LEFT, boxTop, tableRight, boxBottom, totalsBoxPaint)
            canvas.drawRect(MARGIN_LEFT, boxTop, tableRight, boxBottom, totalsBoxBorderPaint)

            y += 14f
            canvas.drawText("Totales de Ruta", MARGIN_LEFT + 12f, y, totalLabelPaint)
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
            val totalDeliveredValueAll = summary.stores.sumOf { it.deliveredValue }
            canvas.drawText(
                "Total entregado: \$${currencyFormat.format(totalDeliveredValueAll)}",
                MARGIN_LEFT + 12f,
                y,
                totalValuePaint
            )
            y += 20f
            canvas.drawText(
                "Total cobrado: \$${currencyFormat.format(summary.totalCollectedMoney)}",
                MARGIN_LEFT + 12f,
                y,
                totalMoneyPaint
            )

            // Draw page number on totals page
            canvas.drawText(
                "Página $pageNumber",
                PAGE_WIDTH / 2f,
                (PAGE_HEIGHT - 20f),
                pageNumPaint
            )

            // === VISUAL STATISTICS PAGE ===
            newPage()

            val chartTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(58, 58, 58) // Graphite
            }
            val chartSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 10f
                color = Color.rgb(90, 90, 90)
            }
            val chartLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 8f
                color = Color.rgb(90, 90, 90)
            }
            val chartValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(58, 58, 58)
            }

            // ── Bar chart title ──
            canvas.drawText("Estadísticas Visuales", MARGIN_LEFT, y, chartTitlePaint)
            y += 10f
            canvas.drawLine(MARGIN_LEFT, y, tableRight, y, linePaint)
            y += 20f

            canvas.drawText("Cobrado por tienda", MARGIN_LEFT, y, chartTitlePaint.apply { textSize = 13f })
            y += 16f

            // ── Horizontal bar chart ──
            val maxCollected = summary.stores.maxOfOrNull { it.collectedValue } ?: 1
            val barMaxWidth = USABLE_WIDTH - 140f  // space for labels and values
            val barHeight = 14f
            val barSpacing = 22f
            val barFillPaint = Paint().apply {
                color = Color.rgb(168, 201, 165) // Sage
                style = Paint.Style.FILL
            }
            val barBgPaint = Paint().apply {
                color = Color.rgb(224, 224, 224) // SoftBorder
                style = Paint.Style.FILL
            }

            summary.stores.forEach { store ->
                ensureSpace(barSpacing + 10f)
                // Store name (truncated)
                var displayName = store.name
                while (chartLabelPaint.measureText(displayName) > 80f && displayName.length > 3) {
                    displayName = displayName.dropLast(1)
                }
                if (displayName != store.name) displayName += "…"
                canvas.drawText(displayName, MARGIN_LEFT, y + barHeight / 2 + 3f, chartLabelPaint)

                // Background bar
                val barLeft = MARGIN_LEFT + 90f
                canvas.drawRect(barLeft, y, barLeft + barMaxWidth, y + barHeight, barBgPaint)

                // Value bar
                val fraction = if (maxCollected > 0) store.collectedValue.toFloat() / maxCollected else 0f
                val barWidth = barMaxWidth * fraction
                if (barWidth > 0) {
                    canvas.drawRect(barLeft, y, barLeft + barWidth, y + barHeight, barFillPaint)
                }

                // Value text
                canvas.drawText(
                    "\$${currencyFormat.format(store.collectedValue)}",
                    barLeft + barMaxWidth + 6f, y + barHeight / 2 + 3f, chartValuePaint
                )
                y += barSpacing
            }
            y += 10f

            // ── Progress donuts section ──
            ensureSpace(140f)
            canvas.drawText("Progreso del Día", MARGIN_LEFT, y, chartTitlePaint.apply { textSize = 13f })
            y += 20f

            val donutRadius = 40f
            val donutStroke = 10f
            val donutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = donutStroke
                strokeCap = Paint.Cap.ROUND
            }

            // Donut 1 — Cobro
            val totalCount = summary.stores.size
            val collectedCount = summary.stores.count { it.isCollected }
            val collFraction = if (totalCount > 0) collectedCount.toFloat() / totalCount else 0f
            val donut1CenterX = MARGIN_LEFT + 150f
            val donut1CenterY = y + donutRadius

            donutPaint.color = Color.rgb(224, 224, 224)
            canvas.drawArc(
                donut1CenterX - donutRadius, donut1CenterY - donutRadius,
                donut1CenterX + donutRadius, donut1CenterY + donutRadius,
                0f, 360f, false, donutPaint
            )
            donutPaint.color = Color.rgb(157, 188, 198) // SkyBlue
            canvas.drawArc(
                donut1CenterX - donutRadius, donut1CenterY - donutRadius,
                donut1CenterX + donutRadius, donut1CenterY + donutRadius,
                -90f, 360f * collFraction, false, donutPaint
            )
            val donutCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(58, 58, 58)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("${(collFraction * 100).toInt()}%", donut1CenterX, donut1CenterY + 5f, donutCenterPaint)
            canvas.drawText("Cobro", donut1CenterX, donut1CenterY + donutRadius + 24f, chartSubPaint.apply { textAlign = Paint.Align.CENTER })
            canvas.drawText("$collectedCount/$totalCount tiendas", donut1CenterX, donut1CenterY + donutRadius + 36f, chartLabelPaint.apply { textAlign = Paint.Align.CENTER })

            // Donut 2 — Dinero cobrado vs entregado
            val totalDeliveredValue = summary.stores.sumOf {
                it.deliveredEmpanadas * EMPANADA_PRICE + it.deliveredDeditos * DEDITO_PRICE
            }
            val moneyFraction = if (totalDeliveredValue > 0) summary.totalCollectedMoney.toFloat() / totalDeliveredValue else 0f
            val donut2CenterX = MARGIN_LEFT + 350f
            val donut2CenterY = y + donutRadius

            donutPaint.color = Color.rgb(224, 224, 224)
            canvas.drawArc(
                donut2CenterX - donutRadius, donut2CenterY - donutRadius,
                donut2CenterX + donutRadius, donut2CenterY + donutRadius,
                0f, 360f, false, donutPaint
            )
            donutPaint.color = Color.rgb(168, 201, 165) // Sage
            canvas.drawArc(
                donut2CenterX - donutRadius, donut2CenterY - donutRadius,
                donut2CenterX + donutRadius, donut2CenterY + donutRadius,
                -90f, 360f * moneyFraction.coerceIn(0f, 1f), false, donutPaint
            )
            canvas.drawText("${(moneyFraction * 100).toInt()}%", donut2CenterX, donut2CenterY + 5f, donutCenterPaint)
            canvas.drawText("Recaudo", donut2CenterX, donut2CenterY + donutRadius + 24f, chartSubPaint)
            canvas.drawText("\$${currencyFormat.format(summary.totalCollectedMoney)}", donut2CenterX, donut2CenterY + donutRadius + 36f, chartLabelPaint)

            // Draw page number on chart page
            canvas.drawText(
                "Página $pageNumber",
                PAGE_WIDTH / 2f,
                (PAGE_HEIGHT - 20f),
                pageNumPaint
            )
            document.finishPage(currentPage)

            // Save the document
            val fileFormat = SimpleDateFormat("MMMM-dd-yyyy_HH-mm", Locale.getDefault())
            val fileName =
                "resumen_${route.name.replace(" ", "_")}_${fileFormat.format(Date()).replaceFirstChar { it.uppercase() }}.pdf"

            // Capturar bytes del PDF para subir a Drive
            val byteStream = ByteArrayOutputStream()
            document.writeTo(byteStream)
            val pdfBytes = byteStream.toByteArray()

            val destination = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToDownloadsWithMediaStore(context, fileName) { out -> out.write(pdfBytes) }
            } else {
                saveToDownloadsLegacy(context, fileName) { out -> out.write(pdfBytes) }
            }
            document.close()
            PdfExportResult(
                localPath = destination,
                fileName = fileName,
                pdfBytes = pdfBytes
            )
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
