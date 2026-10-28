package com.example.printer.engine

import android.graphics.Bitmap
import com.example.model.DuplexMode
import com.example.model.PageOrientation
import com.example.model.PaperSize
import com.example.model.PrintOptions
import com.example.model.PrinterProtocol
import java.io.ByteArrayOutputStream

object StandardDocumentPrintingEngine {

    /**
     * Converts a list of page Bitmaps into a PCL 3 / PCL 5 / PCL 6 byte stream for laser/inkjet printers.
     */
    fun generatePclStream(
        pages: List<Bitmap>,
        options: PrintOptions,
        protocol: PrinterProtocol = PrinterProtocol.PCL_5
    ): ByteArray {
        val out = ByteArrayOutputStream()

        // 1. PCL Job Initialization & Universal Exit Language (UEL)
        // \x1B%-12345X@PJL\r\n@PJL JOB\r\n@PJL ENTER LANGUAGE=PCL\r\n
        out.write(byteArrayOf(0x1B))
        out.write("%-12345X@PJL\r\n".toByteArray(Charsets.US_ASCII))
        out.write("@PJL JOB\r\n".toByteArray(Charsets.US_ASCII))
        out.write("@PJL ENTER LANGUAGE=PCL\r\n".toByteArray(Charsets.US_ASCII))

        // Reset PCL: ESC E
        out.write(byteArrayOf(0x1B, 'E'.code.toByte()))

        // Set copies: ESC & l # X
        val copiesCmd = "\u001B&l${options.copies}X".toByteArray(Charsets.US_ASCII)
        out.write(copiesCmd)

        // Set paper size: ESC & l # A (2 = Letter, 26 = A4, 27 = A3)
        val paperCode = when (options.paperSize) {
            PaperSize.A4 -> 26
            PaperSize.LETTER -> 2
            PaperSize.A3 -> 27
            else -> 26
        }
        val paperCmd = "\u001B&l${paperCode}A".toByteArray(Charsets.US_ASCII)
        out.write(paperCmd)

        // Set orientation: ESC & l # O (0 = Portrait, 1 = Landscape)
        val orientCode = if (options.orientation == PageOrientation.LANDSCAPE) 1 else 0
        out.write("\u001B&l${orientCode}O".toByteArray(Charsets.US_ASCII))

        // Duplex: ESC & l # S (0 = simplex, 1 = duplex long-edge)
        val duplexCode = if (options.duplexMode == DuplexMode.DOUBLE_SIDED) 1 else 0
        out.write("\u001B&l${duplexCode}S".toByteArray(Charsets.US_ASCII))

        // 2. Render each page as PCL Raster
        for ((pageIndex, pageBitmap) in pages.withIndex()) {
            renderPclPage(out, pageBitmap, options, protocol)
        }

        // 3. PCL Job Termination: ESC E and UEL
        out.write(byteArrayOf(0x1B, 'E'.code.toByte()))
        out.write("\u001B%-12345X@PJL EOJ\r\n\u001B%-12345X".toByteArray(Charsets.US_ASCII))

        return out.toByteArray()
    }

    private fun renderPclPage(
        out: ByteArrayOutputStream,
        bitmap: Bitmap,
        options: PrintOptions,
        protocol: PrinterProtocol
    ) {
        // Set resolution: ESC * t 300 R (300 DPI)
        out.write("\u001B*t300R".toByteArray(Charsets.US_ASCII))

        // Start raster graphics at current cursor: ESC * r 1 A
        out.write("\u001B*r1A".toByteArray(Charsets.US_ASCII))

        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val rowBuffer = ByteArray(widthBytes)

        for (y in 0 until height) {
            rowBuffer.fill(0)
            var hasData = false

            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                // In 1-bit PCL raster: 1 = black, 0 = white
                if (luminance < 140) {
                    val byteIndex = x / 8
                    val bitIndex = 7 - (x % 8)
                    rowBuffer[byteIndex] = (rowBuffer[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                    hasData = true
                }
            }

            // Write PCL raster row: ESC * b # W followed by row data
            if (hasData) {
                val header = "\u001B*b${widthBytes}W".toByteArray(Charsets.US_ASCII)
                out.write(header)
                out.write(rowBuffer)
            } else {
                // Transfer 0-byte row to advance cursor
                val emptyHeader = "\u001B*b0W".toByteArray(Charsets.US_ASCII)
                out.write(emptyHeader)
            }
        }

        // End raster graphics: ESC * r B
        out.write("\u001B*rB".toByteArray(Charsets.US_ASCII))

        // Page Eject: ESC & l 0 H
        out.write("\u001B&l0H".toByteArray(Charsets.US_ASCII))
    }
}
