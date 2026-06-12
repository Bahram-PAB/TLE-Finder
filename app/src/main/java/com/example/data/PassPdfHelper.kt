package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PassPdfHelper {

    fun generateAndSharePassesPdf(
        context: Context,
        satelliteName: String,
        passes: List<SatellitePass>,
        activeStationName: String?,
        activeStationCoords: String?
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 Width in points
            val pageHeight = 842 // A4 Height in points
            val margin = 40
            
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            
            val paint = Paint().apply {
                isAntiAlias = true
            }
            
            // Helper to start a new page
            fun startNextPage(): Canvas {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                return page.canvas
            }
            
            var y = 50f
            
            // Drawer tools
            fun drawHeader(canvas: Canvas) {
                // Background top accent bar
                paint.color = 0xFF1976D2.toInt() // Primary blue
                canvas.drawRect(RectF(0f, 0f, pageWidth.toFloat(), 15f), paint)
                
                y = 45f
                paint.color = Color.BLACK
                paint.textSize = 18f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("گزارش پیش‌بینی گذرهای ماهواره", (pageWidth - margin).toFloat(), y, paint)
                
                y += 24f
                paint.textSize = 12f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.DKGRAY
                canvas.drawText("ماهواره: $satelliteName", (pageWidth - margin).toFloat(), y, paint)
                
                y += 18f
                val stationStr = if (activeStationName != null && activeStationCoords != null) {
                    "ایستگاه محاسباتی: $activeStationName ($activeStationCoords)"
                } else {
                    "ایستگاه محاسباتی: نامشخص"
                }
                canvas.drawText(stationStr, (pageWidth - margin).toFloat(), y, paint)
                
                y += 18f
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val generationTime = sdf.format(Date())
                canvas.drawText("زمان تولید گزارش: $generationTime", (pageWidth - margin).toFloat(), y, paint)
                
                y += 15f
                paint.color = Color.LTGRAY
                canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, paint)
                y += 25f
            }
            
            drawHeader(canvas)
            
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.US)
            
            for ((index, pass) in passes.withIndex()) {
                // Check height before starting a new pass card
                if (y + 140f > pageHeight - margin) {
                    canvas = startNextPage()
                    drawHeader(canvas)
                }
                
                // Draw Card Background
                paint.color = 0xFFF5F5F5.toInt() // Light Grey
                paint.style = Paint.Style.FILL
                val cardRect = RectF(margin.toFloat(), y, (pageWidth - margin).toFloat(), y + 125f)
                canvas.drawRoundRect(cardRect, 8f, 8f, paint)
                
                // Card Border
                paint.color = Color.LTGRAY
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(cardRect, 8f, 8f, paint)
                
                // Write Pass Title
                paint.style = Paint.Style.FILL
                paint.color = 0xFF1976D2.toInt()
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("گذر شماره ${index + 1}", (pageWidth - margin - 15).toFloat(), y + 22f, paint)
                
                // Add an elegant small colored dot next to title
                paint.color = 0xFF1976D2.toInt()
                canvas.drawCircle((pageWidth - margin - 8).toFloat(), y + 18f, 3f, paint)
                
                // Content of the card
                val colRightX = (pageWidth - margin - 20).toFloat()
                
                paint.color = Color.BLACK
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                
                // Line 1: Date & Duration
                val dateStr = sdfDate.format(Date(pass.startEpochMs))
                val durationSeconds = (pass.endEpochMs - pass.startEpochMs) / 1000L
                val durationMin = durationSeconds / 60
                val durationSec = durationSeconds % 60
                val durationStr = "$durationMin دقیقه و " + String.format(Locale.US, "%02d", durationSec) + " ثانیه"
                
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("تاریخ گذر: $dateStr", colRightX, y + 42f, paint)
                canvas.drawText("مدت زمان: $durationStr", colRightX, y + 60f, paint)
                
                // Line 2: Details: طلوع (Start), اوج (Apex), غروب (End)
                val startStr = sdfTime.format(Date(pass.startEpochMs))
                val maxStr = sdfTime.format(Date(pass.maxElevationTimeMs))
                val endStr = sdfTime.format(Date(pass.endEpochMs))
                val elevStr = String.format(Locale.US, "%.1f°", pass.maxElevation)
                
                canvas.drawText("طلوع (شروع): $startStr", colRightX, y + 80f, paint)
                canvas.drawText("غروب (پایان): $endStr", colRightX, y + 98f, paint)
                canvas.drawText("اوج (بیشترین ارتفاع): $maxStr (الوشن: $elevStr)", colRightX, y + 114f, paint)
                
                y += 140f
            }
            
            pdfDocument.finishPage(page)
            
            // Save the document to cache
            val cleanSatName = satelliteName.replace(" ", "_").replace("/", "_")
            val fileName = "Passes_${cleanSatName}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()
            
            // Sharing the file via Intent
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "پیش‌بینی گذرهای ماهواره $satelliteName")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری گزارش PDF"))
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطا در ساخت گزارش PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
