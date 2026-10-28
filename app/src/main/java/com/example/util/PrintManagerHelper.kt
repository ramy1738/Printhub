package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import com.example.model.ColorMode
import com.example.model.FitMode
import com.example.model.PageOrientation
import com.example.model.PaperSize
import com.example.model.PrintOptions
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object PrintManagerHelper {
    private const val TAG = "PrintManagerHelper"

    /**
     * Print a PDF from a content Uri using native Android PrintManager
     */
    fun printPdfUri(
        context: Context,
        pdfUri: Uri,
        jobName: String,
        printOptions: PrintOptions = PrintOptions(),
        onResult: (Boolean, String?) -> Unit
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            onResult(false, "Service d'impression indisponible")
            return
        }

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }

                if (destination == null) {
                    callback?.onWriteFailed("Fichier destination nul")
                    return
                }

                var inputStream: FileInputStream? = null
                var outputStream: FileOutputStream? = null
                try {
                    val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                    if (pfd != null) {
                        inputStream = FileInputStream(pfd.fileDescriptor)
                        outputStream = FileOutputStream(destination.fileDescriptor)

                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } >= 0) {
                            if (cancellationSignal?.isCanceled == true) {
                                callback?.onWriteCancelled()
                                return
                            }
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } else {
                        callback?.onWriteFailed("Impossible d'ouvrir le fichier source")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur écriture PDF", e)
                    callback?.onWriteFailed(e.localizedMessage ?: "Erreur inconnue")
                } finally {
                    try {
                        inputStream?.close()
                        outputStream?.close()
                    } catch (_: IOException) {}
                }
            }
        }

        val attributesBuilder = PrintAttributes.Builder()
        // Paper size mapping
        when (printOptions.paperSize) {
            PaperSize.A4 -> attributesBuilder.setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            PaperSize.A3 -> attributesBuilder.setMediaSize(PrintAttributes.MediaSize.ISO_A3)
            PaperSize.LETTER -> attributesBuilder.setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
            PaperSize.PHOTO_4X6 -> attributesBuilder.setMediaSize(PrintAttributes.MediaSize.JPN_HAGAKI)
            PaperSize.ROLL_80MM, PaperSize.ROLL_58MM -> attributesBuilder.setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        }

        // Color mode mapping
        if (printOptions.colorMode == ColorMode.MONOCHROME) {
            attributesBuilder.setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
        } else {
            attributesBuilder.setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        }

        printManager.print(jobName, printAdapter, attributesBuilder.build())
        onResult(true, null)
    }

    /**
     * Print a photo Bitmap with scaling and color mode options using native Android PrintManager
     */
    fun printBitmap(
        context: Context,
        bitmap: Bitmap,
        jobName: String,
        printOptions: PrintOptions = PrintOptions(),
        onResult: (Boolean, String?) -> Unit
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            onResult(false, "Service d'impression indisponible")
            return
        }

        val printAdapter = object : PrintDocumentAdapter() {
            private var printAttributes: PrintAttributes? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                printAttributes = newAttributes
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("Fichier destination nul")
                    return
                }

                val pdfDocument = PdfDocument()
                try {
                    val mediaSize = printAttributes?.mediaSize ?: PrintAttributes.MediaSize.ISO_A4
                    val pageWidth = (mediaSize.widthMils * 72 / 1000)
                    val pageHeight = (mediaSize.heightMils * 72 / 1000)

                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    if (printOptions.colorMode == ColorMode.MONOCHROME) {
                        val matrix = ColorMatrix().apply { setSaturation(0f) }
                        paint.colorFilter = ColorMatrixColorFilter(matrix)
                    }

                    // Compute transform matrix
                    val srcRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
                    val dstRect = RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
                    val matrix = Matrix()

                    if (printOptions.fitMode == FitMode.FILL_PAGE) {
                        val scale = maxOf(
                            pageWidth.toFloat() / bitmap.width.toFloat(),
                            pageHeight.toFloat() / bitmap.height.toFloat()
                        )
                        val dx = (pageWidth - bitmap.width * scale) / 2f
                        val dy = (pageHeight - bitmap.height * scale) / 2f
                        matrix.postScale(scale, scale)
                        matrix.postTranslate(dx, dy)
                    } else {
                        matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.CENTER)
                    }

                    canvas.drawBitmap(bitmap, matrix, paint)
                    pdfDocument.finishPage(page)

                    val outputStream = FileOutputStream(destination.fileDescriptor)
                    pdfDocument.writeTo(outputStream)
                    outputStream.close()

                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur écriture photo", e)
                    callback?.onWriteFailed(e.localizedMessage ?: "Erreur d'impression photo")
                } finally {
                    pdfDocument.close()
                }
            }
        }

        val attributesBuilder = PrintAttributes.Builder()
        when (printOptions.paperSize) {
            PaperSize.PHOTO_4X6 -> attributesBuilder.setMediaSize(PrintAttributes.MediaSize.JPN_HAGAKI)
            PaperSize.A3 -> attributesBuilder.setMediaSize(PrintAttributes.MediaSize.ISO_A3)
            PaperSize.LETTER -> attributesBuilder.setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
            else -> attributesBuilder.setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        }

        if (printOptions.orientation == PageOrientation.LANDSCAPE) {
            attributesBuilder.setMediaSize(
                attributesBuilder.build().mediaSize?.asLandscape() ?: PrintAttributes.MediaSize.ISO_A4.asLandscape()
            )
        }

        if (printOptions.colorMode == ColorMode.MONOCHROME) {
            attributesBuilder.setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
        } else {
            attributesBuilder.setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        }

        printManager.print(jobName, printAdapter, attributesBuilder.build())
        onResult(true, null)
    }

    /**
     * Helper to decode a bitmap safely from a Uri
     */
    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            var inSampleSize = 1
            if (boundsOptions.outWidth > maxDimension || boundsOptions.outHeight > maxDimension) {
                val halfWidth = boundsOptions.outWidth / 2
                val halfHeight = boundsOptions.outHeight / 2
                while (halfWidth / inSampleSize >= maxDimension && halfHeight / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur décodage bitmap", e)
            null
        }
    }
}
