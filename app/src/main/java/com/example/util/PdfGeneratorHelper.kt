package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.model.AppLanguage
import com.example.model.BindingOption
import com.example.model.ColorMode
import com.example.model.DuplexMode
import com.example.model.PrintOptions
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGeneratorHelper {

    /**
     * Generates a printable PDF from a user-created document
     */
    fun createComposedPdf(
        context: Context,
        title: String,
        author: String,
        recipient: String,
        bodyContent: String,
        language: AppLanguage = AppLanguage.FRENCH
    ): File {
        val dir = File(context.cacheDir, "print_docs").apply { mkdirs() }
        val fileName = "doc_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(dir, fileName)

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard width at 72dpi
        val pageHeight = 842 // A4 standard height at 72dpi

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Top decorative header banner
        paint.color = Color.parseColor("#1E40AF")
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 18f, paint)

        // Accent stripe
        paint.color = Color.parseColor("#0D9488")
        canvas.drawRect(0f, 18f, pageWidth.toFloat(), 22f, paint)

        // Title
        val titlePaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#0F172A")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val safeTitle = if (title.isBlank()) {
            when (language) {
                AppLanguage.ARABIC -> "مستند رسمي"
                AppLanguage.ENGLISH -> "Official Document"
                else -> "Document Officiel"
            }
        } else title

        canvas.drawText(safeTitle, 40f, 65f, titlePaint)

        // Meta separator line
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1.5f
        canvas.drawLine(40f, 80f, (pageWidth - 40).toFloat(), 80f, paint)

        // Metadata box (Date, Author, Recipient)
        val metaPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#475569")
            textSize = 10f
        }
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val dateLabel = when (language) {
            AppLanguage.ARABIC -> "التاريخ: $dateStr"
            AppLanguage.ENGLISH -> "Date: $dateStr"
            else -> "Date : $dateStr"
        }
        canvas.drawText(dateLabel, 40f, 100f, metaPaint)

        if (author.isNotBlank()) {
            val authorLabel = when (language) {
                AppLanguage.ARABIC -> "المرسل: $author"
                AppLanguage.ENGLISH -> "From: $author"
                else -> "Émetteur : $author"
            }
            canvas.drawText(authorLabel, 40f, 115f, metaPaint)
        }

        if (recipient.isNotBlank()) {
            val recipientLabel = when (language) {
                AppLanguage.ARABIC -> "المستلم: $recipient"
                AppLanguage.ENGLISH -> "To: $recipient"
                else -> "Destinataire : $recipient"
            }
            canvas.drawText(recipientLabel, (pageWidth / 2).toFloat(), 115f, metaPaint)
        }

        // Content body using StaticLayout for robust multiline wrapping
        val contentPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1E293B")
            textSize = 12f
        }

        val textWidth = pageWidth - 80
        val safeContent = if (bodyContent.isBlank()) "(Aucun texte / No content / لا يوجد محتوى)" else bodyContent

        canvas.save()
        canvas.translate(40f, 140f)

        val staticLayout = StaticLayout.Builder.obtain(
            safeContent,
            0,
            safeContent.length,
            contentPaint,
            textWidth
        )
            .setLineSpacing(4f, 1.2f)
            .setAlignment(if (language == AppLanguage.ARABIC) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
            .build()

        staticLayout.draw(canvas)
        canvas.restore()

        // Footer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 9f
        val footerText = when (language) {
            AppLanguage.ARABIC -> "تم الإنشاء بواسطة ImprimeDoc - جاهز للطباعة"
            AppLanguage.ENGLISH -> "Generated with ImprimeDoc - Ready for print"
            else -> "Généré avec ImprimeDoc - Prêt pour impression"
        }
        canvas.drawText(footerText, 40f, (pageHeight - 30).toFloat(), paint)

        pdfDocument.finishPage(page)

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Generates a Print Job Order Sheet PDF that can be attached when sending to print shops
     */
    fun createPrintSpecSheetPdf(
        context: Context,
        jobTitle: String,
        options: PrintOptions,
        recipient: String,
        language: AppLanguage = AppLanguage.FRENCH
    ): File {
        val dir = File(context.cacheDir, "print_docs").apply { mkdirs() }
        val file = File(dir, "print_order_${System.currentTimeMillis()}.pdf")

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Header
        paint.color = Color.parseColor("#1E40AF")
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val headerTitle = when (language) {
            AppLanguage.ARABIC -> "بطاقة طلب ومواصفات الطباعة"
            AppLanguage.ENGLISH -> "PRINT SPECIFICATION ORDER"
            else -> "FICHE DE SPÉCIFICATIONS D'IMPRESSION"
        }
        canvas.drawText(headerTitle, 35f, 45f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        val subTitle = when (language) {
            AppLanguage.ARABIC -> "تطبيق ImprimeDoc - تفاصيل المهمة للمركز"
            AppLanguage.ENGLISH -> "ImprimeDoc - Detailed instructions for print operator"
            else -> "ImprimeDoc - Instructions détaillées pour l'opérateur d'impression"
        }
        canvas.drawText(subTitle, 35f, 70f, paint)

        // Spec items
        var y = 130f
        fun drawSpecRow(label: String, value: String) {
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(35f, y - 18f, 560f, y + 14f, 6f, 6f, paint)

            paint.color = Color.parseColor("#334155")
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(label, 50f, y, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(value, 240f, y, paint)

            y += 40f
        }

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        drawSpecRow(
            if (language == AppLanguage.ARABIC) "المستند:" else if (language == AppLanguage.ENGLISH) "Document:" else "Document :",
            jobTitle
        )
        drawSpecRow(
            if (language == AppLanguage.ARABIC) "تاريخ الطلب:" else if (language == AppLanguage.ENGLISH) "Order Date:" else "Date de la commande :",
            dateStr
        )
        drawSpecRow(
            if (language == AppLanguage.ARABIC) "عدد النسخ المطلوبة:" else if (language == AppLanguage.ENGLISH) "Copies Needed:" else "Exemplaires demandés :",
            "${options.copies} x"
        )
        drawSpecRow(
            if (language == AppLanguage.ARABIC) "نوع الألوان:" else if (language == AppLanguage.ENGLISH) "Color Mode:" else "Mode couleur :",
            if (options.colorMode == ColorMode.COLOR) "Couleur / Color / ألوان" else "Noir & Blanc / B&W / أبيض وأسود"
        )
        drawSpecRow(
            if (language == AppLanguage.ARABIC) "حجم الورق:" else if (language == AppLanguage.ENGLISH) "Paper Size:" else "Format de papier :",
            options.paperSize.displayName
        )
        drawSpecRow(
            if (language == AppLanguage.ARABIC) "الطباعة وجه/وجهين:" else if (language == AppLanguage.ENGLISH) "Duplex:" else "Recto / Verso :",
            if (options.duplexMode == DuplexMode.DOUBLE_SIDED) "Recto-Verso (Double-sided)" else "Recto simple (Single-sided)"
        )

        val bindingText = when (options.bindingOption) {
            BindingOption.TOP_LEFT_STAPLE -> "Agrafe coin supérieur (Staple)"
            BindingOption.SPIRAL -> "Reliure Spirale (Spiral binding)"
            BindingOption.NONE -> "Aucune reliure (None)"
        }
        drawSpecRow(
            if (language == AppLanguage.ARABIC) "التشطيب والتجليد:" else if (language == AppLanguage.ENGLISH) "Finishing:" else "Finition / Reliure :",
            bindingText
        )

        if (recipient.isNotBlank()) {
            drawSpecRow(
                if (language == AppLanguage.ARABIC) "المستلم / المركز:" else if (language == AppLanguage.ENGLISH) "Shop / Recipient:" else "Destinataire :",
                recipient
            )
        }

        if (options.customNotes.isNotBlank()) {
            drawSpecRow(
                if (language == AppLanguage.ARABIC) "تعليمات إضافية:" else if (language == AppLanguage.ENGLISH) "Special Notes:" else "Instructions :",
                options.customNotes
            )
        }

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /**
     * Inspect PDF to get page count and first page thumbnail using PdfRenderer
     */
    fun inspectPdf(context: Context, pdfUri: Uri): Pair<Int, Bitmap?> {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                var thumbnail: Bitmap? = null
                if (pageCount > 0) {
                    val page = renderer.openPage(0)
                    // Render page thumbnail
                    val width = 400
                    val height = (width * page.height / page.width)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    thumbnail = bitmap
                }
                return Pair(pageCount, thumbnail)
            }
        } catch (_: Exception) {
            // Ignore failure, fallback to 1 page
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }
        return Pair(1, null)
    }

    /**
     * Get content Uri for a local File via FileProvider
     */
    fun getSharableUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
