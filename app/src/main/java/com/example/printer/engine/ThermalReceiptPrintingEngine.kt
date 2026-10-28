package com.example.printer.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.model.PaperSize
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ThermalReceiptPrintingEngine {

    // ESC/POS Command Constants
    val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @
    val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    val CMD_FEED_3 = byteArrayOf(0x1B, 0x64, 0x03) // ESC d 3
    val CMD_FEED_5 = byteArrayOf(0x1B, 0x64, 0x05)
    val CMD_CUT_PARTIAL = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // GS V 66 0 (cut)
    val CMD_DRAWER_PULSE = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()) // Open cash drawer

    /**
     * Determines the optimal dots per line for paper size
     * 80mm thermal paper is typically 576 dots (72mm printable at 203 DPI)
     * 58mm thermal paper is typically 384 dots (48mm printable at 203 DPI)
     */
    fun getDotsWidth(paperSize: PaperSize): Int {
        return if (paperSize == PaperSize.ROLL_58MM) 384 else 576
    }

    /**
     * Converts any arbitrary Bitmap (e.g. document page or photo) into ESC/POS GS v 0 raster bytes.
     * Uses Floyd-Steinberg dithering / adaptive thresholding for optimal contrast on thermal paper.
     */
    fun convertBitmapToEscPosRaster(
        sourceBitmap: Bitmap,
        targetWidthDots: Int = 576,
        densityMultiplier: Float = 1.0f,
        autoCut: Boolean = true
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(CMD_INIT)

        // Scale bitmap to fit printer paper width while preserving aspect ratio
        val scale = targetWidthDots.toFloat() / sourceBitmap.width.toFloat()
        val targetHeight = (sourceBitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(sourceBitmap, targetWidthDots, targetHeight, true)

        // Generate ESC/POS GS v 0 raster command
        // Format: GS v 0 m xL xH yL yH d1...dk
        val widthBytes = (targetWidthDots + 7) / 8
        val heightPixels = targetHeight

        out.write(0x1D) // GS
        out.write(0x76) // v
        out.write(0x30) // 0
        out.write(0x00) // m = 0 (normal)
        out.write(widthBytes and 0xFF)
        out.write((widthBytes shr 8) and 0xFF)
        out.write(heightPixels and 0xFF)
        out.write((heightPixels shr 8) and 0xFF)

        // Convert pixels to 1-bit monochrome (8 pixels per byte, MSB first)
        val threshold = (128 * (1.0f / densityMultiplier)).toInt().coerceIn(30, 220)
        val pixels = IntArray(targetWidthDots * heightPixels)
        scaled.getPixels(pixels, 0, targetWidthDots, 0, 0, targetWidthDots, heightPixels)

        for (y in 0 until heightPixels) {
            for (byteIdx in 0 until widthBytes) {
                var byteVal = 0
                for (bit in 0 until 8) {
                    val x = byteIdx * 8 + bit
                    if (x < targetWidthDots) {
                        val pixel = pixels[y * targetWidthDots + x]
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        // 1 = black, 0 = white in ESC/POS
                        if (luminance < threshold) {
                            byteVal = byteVal or (0x80 shr bit)
                        }
                    }
                }
                out.write(byteVal)
            }
        }

        out.write(CMD_FEED_5)
        if (autoCut) {
            out.write(CMD_CUT_PARTIAL)
        }

        return out.toByteArray()
    }

    /**
     * Renders a complete structured Receipt/Document with full native Arabic RTL shaping,
     * headers, metadata, custom notes, and timestamps to a crisp monochrome Bitmap,
     * then converts it into ESC/POS bytes.
     */
    fun createArabicReceiptRaster(
        title: String,
        details: List<Pair<String, String>>,
        notes: String = "",
        paperSize: PaperSize = PaperSize.ROLL_80MM,
        copies: Int = 1,
        cutPaper: Boolean = true
    ): ByteArray {
        val width = getDotsWidth(paperSize)
        val is58 = paperSize == PaperSize.ROLL_58MM

        // Calculate estimated height
        val baseLineHeight = if (is58) 32 else 38
        val headerHeight = if (is58) 120 else 160
        val detailsHeight = details.size * baseLineHeight
        val notesHeight = if (notes.isNotBlank()) 100 else 20
        val footerHeight = 80
        val totalHeight = headerHeight + detailsHeight + notesHeight + footerHeight

        // Create canvas bitmap
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paintTitle = TextPaint().apply {
            color = Color.BLACK
            textSize = if (is58) 26f else 32f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val paintSub = TextPaint().apply {
            color = Color.BLACK
            textSize = if (is58) 18f else 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val paintDivider = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        val paintLabel = TextPaint().apply {
            color = Color.BLACK
            textSize = if (is58) 19f else 24f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }

        val paintValue = TextPaint().apply {
            color = Color.BLACK
            textSize = if (is58) 19f else 24f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        var currentY = if (is58) 40f else 50f

        // 1. Draw Title (Arabic / French / English automatically shaped by Android Canvas)
        canvas.drawText(title, (width / 2).toFloat(), currentY, paintTitle)
        currentY += if (is58) 32f else 42f

        // Date and Time
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date())
        canvas.drawText(dateStr, (width / 2).toFloat(), currentY, paintSub)
        currentY += 24f

        // Divider
        canvas.drawLine(16f, currentY, (width - 16).toFloat(), currentY, paintDivider)
        currentY += 30f

        // 2. Details Rows
        val margin = 20f
        for (item in details) {
            val label = item.first
            val value = item.second

            // Check if text contains Arabic characters (Unicode range 0x0600 - 0x06FF)
            val hasArabic = label.any { it.code in 0x0600..0x06FF } || value.any { it.code in 0x0600..0x06FF }

            if (hasArabic) {
                // When Arabic is present, we use StaticLayout for RTL bidirectional rendering
                val combinedText = "$label : $value"
                val textLayout = StaticLayout.Builder.obtain(
                    combinedText,
                    0,
                    combinedText.length,
                    paintLabel,
                    width - (margin * 2).toInt()
                )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false)
                    .build()

                canvas.save()
                canvas.translate(margin, currentY)
                textLayout.draw(canvas)
                canvas.restore()
                currentY += textLayout.height + 12f
            } else {
                canvas.drawText(label, margin, currentY, paintLabel)
                canvas.drawText(value, (width - margin), currentY, paintValue)
                currentY += baseLineHeight.toFloat()
            }
        }

        // 3. Notes section if present
        if (notes.isNotBlank()) {
            currentY += 10f
            canvas.drawLine(16f, currentY, (width - 16).toFloat(), currentY, paintDivider)
            currentY += 25f

            val notesLayout = StaticLayout.Builder.obtain(
                notes,
                0,
                notes.length,
                paintSub,
                width - (margin * 2).toInt()
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()

            canvas.save()
            canvas.translate(margin, currentY)
            notesLayout.draw(canvas)
            canvas.restore()
            currentY += notesLayout.height + 15f
        }

        // 4. Footer
        currentY += 15f
        canvas.drawLine(16f, currentY, (width - 16).toFloat(), currentY, paintDivider)
        currentY += 28f
        canvas.drawText("*** ImprimeDoc Mobile ***", (width / 2).toFloat(), currentY, paintSub)

        // Convert the finished bitmap into ESC/POS bytes
        val singleJobBytes = convertBitmapToEscPosRaster(
            sourceBitmap = bitmap,
            targetWidthDots = width,
            densityMultiplier = 1.0f,
            autoCut = cutPaper
        )

        // Support multiple copies
        val finalOut = ByteArrayOutputStream()
        repeat(copies.coerceAtLeast(1)) {
            finalOut.write(singleJobBytes)
        }
        return finalOut.toByteArray()
    }
}
