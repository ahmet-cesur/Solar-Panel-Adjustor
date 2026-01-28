package com.acesur.solarpvtracker.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.acesur.solarpvtracker.solar.DailyTiltAngle
import com.acesur.solarpvtracker.solar.MonthlyTiltAngle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfExportManager(private val context: Context) {
    
    companion object {
        private const val PAGE_WIDTH = 595  // A4 width in points
        private const val PAGE_HEIGHT = 842 // A4 height in points
        private const val MARGIN = 40f
        private const val LINE_HEIGHT = 18f
        private const val HEADER_HEIGHT = 80f
    }
    
    private val titlePaint = Paint().apply {
        color = Color.parseColor("#FF8C00")  // Solar Orange
        textSize = 24f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    private val subtitlePaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 14f
        isAntiAlias = true
    }
    
    private val headerPaint = Paint().apply {
        color = Color.WHITE
        textSize = 12f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    private val cellPaint = Paint().apply {
        color = Color.BLACK
        textSize = 11f
        isAntiAlias = true
    }
    
    private val linePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
    }
    
    private val headerBgPaint = Paint().apply {
        color = Color.parseColor("#FF8C00")
    }
    
    /**
     * Generate PDF report for monthly tilt angles
     */
    fun generateMonthlyReport(
        monthlyAngles: List<MonthlyTiltAngle>,
        latitude: Double,
        longitude: Double
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        
        var yPosition = MARGIN
        
        // Draw header
        yPosition = drawHeader(canvas, yPosition, "Monthly Optimal Tilt Angles", latitude, longitude)
        yPosition += 20f
        
        // Draw table header
        val colWidths = floatArrayOf(80f, 150f, 100f, 185f)
        val headers = listOf("Month", "Month Name", "Angle (°)", "Notes")
        yPosition = drawTableHeader(canvas, yPosition, headers, colWidths)
        
        // Draw data rows
        for (angle in monthlyAngles) {
            if (yPosition > PAGE_HEIGHT - MARGIN - 30) break
            
            val rowData = listOf(
                angle.month.toString(),
                angle.monthName,
                String.format("%.1f", angle.optimalAngle),
                angle.notes
            )
            yPosition = drawTableRow(canvas, yPosition, rowData, colWidths)
        }
        
        // Footer
        drawFooter(canvas)
        
        document.finishPage(page)
        
        return saveDocument(document, "Monthly_Tilt_Angles")
    }
    
    /**
     * Generate PDF report for daily tilt angles (multi-page, 4 columns)
     */
    fun generateDailyReport(
        dailyAngles: List<DailyTiltAngle>,
        latitude: Double,
        longitude: Double
    ): File? {
        val document = PdfDocument()
        val rowsPerPage = 45 // Increased rows per page
        val colsPerRow = 4   // 4 sets of data per row
        val itemsPerPage = rowsPerPage * colsPerRow
        val totalPages = (dailyAngles.size + itemsPerPage - 1) / itemsPerPage
        
        // Define column widths for 4 columns: Date (70) + Angle (58) approx
        // Total width available is roughly 515. 515 / 4 = ~128.
        val colWidthOfPair = 129f
        val dateWidth = 75f
        val angleWidth = 54f
        
        // Create the flattened arrays for drawing
        // Date, Angle, Date, Angle, Date, Angle, Date, Angle
        val colWidths = FloatArray(colsPerRow * 2) { i ->
            if (i % 2 == 0) dateWidth else angleWidth
        }
        
        val headers = MutableList(colsPerRow * 2) { i ->
            if (i % 2 == 0) "Date" else "Angle"
        }
        
        for (pageNum in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            
            var yPosition = MARGIN
            
            // Draw header on first page
            if (pageNum == 0) {
                yPosition = drawHeader(canvas, yPosition, "Daily Optimal Tilt Angles (365 Days)", latitude, longitude)
                yPosition += 10f
            } else {
                yPosition += 20f
                canvas.drawText("Daily Optimal Tilt Angles - Page ${pageNum + 1}", MARGIN, yPosition, subtitlePaint)
                yPosition += 30f
            }
            
            // Draw table header
            yPosition = drawTableHeader(canvas, yPosition, headers, colWidths)
            
            // Draw data rows
            val pageStartIdx = pageNum * itemsPerPage
            
            for (row in 0 until rowsPerPage) {
                val rowData = mutableListOf<String>()
                var hasDataInRow = false
                
                for (col in 0 until colsPerRow) {
                    val idx = pageStartIdx + row + (col * rowsPerPage) 
                    // Note: This fills column by column (Vertical flow) which is usually better for reading?
                    // Or Row by row? "Row by row" means i, i+1, i+2, i+3.
                    // "Column by column" means i, i+45, i+90... 
                    // Let's do Row by Row (sequential in reading order: left to right) as it's easier to follow if you read horizontal.
                    // Actually for "Date" lists, often Columnar (Vertical) sorting is preferred so you read down 1 column then go to next.
                    // But to keep it simple and code safe, let's do sequential (Left-to-Right).
                    // Actually, let's stick to Index = pageStartIdx + (row * colsPerRow) + col. 
                    
                    val itemIdx = pageStartIdx + (row * colsPerRow) + col
                    
                    if (itemIdx < dailyAngles.size) {
                        val angle = dailyAngles[itemIdx]
                        // Format date to short MM/dd
                        val shortDate = angle.date.substringBeforeLast("/") // Assume MM/dd/yyyy -> MM/dd
                        rowData.add(shortDate)
                        rowData.add(String.format("%.1f°", angle.optimalAngle))
                        hasDataInRow = true
                    } else {
                        rowData.add("")
                        rowData.add("")
                    }
                }
                
                if (!hasDataInRow) break
                
                yPosition = drawTableRow(canvas, yPosition, rowData, colWidths)
                
                if (yPosition > PAGE_HEIGHT - MARGIN) break
            }
            
            // Page number
            canvas.drawText(
                "Page ${pageNum + 1} of $totalPages",
                PAGE_WIDTH / 2f - 30f,
                PAGE_HEIGHT - 20f,
                subtitlePaint
            )
            
            document.finishPage(page)
        }
        
        return saveDocument(document, "Daily_Tilt_Angles")
    }
    
    private fun drawHeader(
        canvas: Canvas,
        startY: Float,
        title: String,
        latitude: Double,
        longitude: Double
    ): Float {
        var y = startY
        
        // Title
        canvas.drawText("Solar Panel Adjustor", MARGIN, y + 25f, titlePaint)
        y += 35f
        
        // Report title
        canvas.drawText(title, MARGIN, y + 20f, subtitlePaint.apply { textSize = 16f })
        y += 30f
        
        // Location and date
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        
        canvas.drawText("Location: ${String.format("%.4f", latitude)}°, ${String.format("%.4f", longitude)}°", MARGIN, y + 15f, subtitlePaint.apply { textSize = 11f })
        y += 18f
        canvas.drawText("Generated: $dateStr", MARGIN, y + 15f, subtitlePaint)
        y += 25f
        
        // Separator line
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        
        return y
    }
    
    private fun drawTableHeader(
        canvas: Canvas,
        startY: Float,
        headers: List<String>,
        colWidths: FloatArray
    ): Float {
        val y = startY
        var x = MARGIN
        
        // Draw header background
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + LINE_HEIGHT + 8f, headerBgPaint)
        
        // Draw header text
        for (i in headers.indices) {
            canvas.drawText(headers[i], x + 5f, y + LINE_HEIGHT, headerPaint)
            x += colWidths[i]
        }
        
        return y + LINE_HEIGHT + 10f
    }
    
    private fun drawTableRow(
        canvas: Canvas,
        startY: Float,
        data: List<String>,
        colWidths: FloatArray
    ): Float {
        var x = MARGIN
        
        for (i in data.indices) {
            canvas.drawText(data[i], x + 5f, startY + LINE_HEIGHT - 3f, cellPaint)
            x += colWidths[i]
        }
        
        // Draw bottom line
        canvas.drawLine(MARGIN, startY + LINE_HEIGHT, PAGE_WIDTH - MARGIN, startY + LINE_HEIGHT, linePaint)
        
        return startY + LINE_HEIGHT + 2f
    }
    
    private fun drawFooter(canvas: Canvas) {
        val footerY = PAGE_HEIGHT - 30f
        canvas.drawText(
            "Generated by Solar Panel Adjustor App",
            MARGIN,
            footerY,
            subtitlePaint.apply { textSize = 10f; color = Color.GRAY }
        )
    }
    
    private fun saveDocument(document: PdfDocument, filePrefix: String): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${filePrefix}_$timestamp.pdf"
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }
    
    /**
     * Create share intent for the PDF file
     */
    fun createShareIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
