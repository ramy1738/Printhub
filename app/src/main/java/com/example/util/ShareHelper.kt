package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.AppLanguage
import com.example.model.BindingOption
import com.example.model.ColorMode
import com.example.model.DuplexMode
import com.example.model.PrintOptions

object ShareHelper {

    /**
     * Builds a localized text summary of the print job specifications
     */
    fun buildOrderMessage(
        title: String,
        options: PrintOptions,
        language: AppLanguage
    ): String {
        return when (language) {
            AppLanguage.ARABIC -> {
                """
                📄 *طلب طباعة جديد عبر ImprimeDoc*
                -----------------------------------
                • المستند: $title
                • عدد النسخ: ${options.copies}
                • نوع الألوان: ${if (options.colorMode == ColorMode.COLOR) "ألوان" else "أبيض وأسود"}
                • حجم الورق: ${options.paperSize.displayName}
                • الوجهين: ${if (options.duplexMode == DuplexMode.DOUBLE_SIDED) "طباعة على الوجهين" else "وجه واحد فقط"}
                • التجليد: ${when (options.bindingOption) {
                    BindingOption.TOP_LEFT_STAPLE -> "تدبيس الزاوية"
                    BindingOption.SPIRAL -> "تجليد لولبي"
                    BindingOption.NONE -> "بدون تجليد"
                }}
                ${if (options.customNotes.isNotBlank()) "• ملاحظات إضافية: ${options.customNotes}" else ""}
                -----------------------------------
                يرجى تأكيد الاستلام والتجهيز. شكراً!
                """.trimIndent()
            }
            AppLanguage.ENGLISH -> {
                """
                📄 *New Print Order via ImprimeDoc*
                -----------------------------------
                • Document: $title
                • Copies: ${options.copies}
                • Color Mode: ${if (options.colorMode == ColorMode.COLOR) "Color" else "Black & White"}
                • Paper Size: ${options.paperSize.displayName}
                • Duplex: ${if (options.duplexMode == DuplexMode.DOUBLE_SIDED) "Double-sided (Duplex)" else "Single-sided"}
                • Binding: ${when (options.bindingOption) {
                    BindingOption.TOP_LEFT_STAPLE -> "Top-left staple"
                    BindingOption.SPIRAL -> "Spiral binding"
                    BindingOption.NONE -> "None"
                }}
                ${if (options.customNotes.isNotBlank()) "• Notes: ${options.customNotes}" else ""}
                -----------------------------------
                Please confirm receipt and printing. Thank you!
                """.trimIndent()
            }
            else -> {
                """
                📄 *Nouvelle commande d'impression via ImprimeDoc*
                -----------------------------------
                • Document : $title
                • Nombre d'exemplaires : ${options.copies}
                • Mode couleur : ${if (options.colorMode == ColorMode.COLOR) "Couleur" else "Noir & Blanc"}
                • Format de papier : ${options.paperSize.displayName}
                • Recto / Verso : ${if (options.duplexMode == DuplexMode.DOUBLE_SIDED) "Recto-Verso" else "Recto simple"}
                • Reliure : ${when (options.bindingOption) {
                    BindingOption.TOP_LEFT_STAPLE -> "Agrafe en haut à gauche"
                    BindingOption.SPIRAL -> "Reliure spirale"
                    BindingOption.NONE -> "Aucune"
                }}
                ${if (options.customNotes.isNotBlank()) "• Instructions : ${options.customNotes}" else ""}
                -----------------------------------
                Merci de confirmer la réception et le tirage.
                """.trimIndent()
            }
        }
    }

    /**
     * Sends file + order message directly via Email (great for HP ePrint, Epson Connect, or print shop email)
     */
    fun sendViaEmail(
        context: Context,
        recipientEmail: String,
        subjectTitle: String,
        orderMessage: String,
        fileUri: Uri,
        mimeType: String = "application/pdf"
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, "[Impression] $subjectTitle")
            putExtra(Intent.EXTRA_TEXT, orderMessage)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Envoyer par e-mail à l'imprimeur")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Sends file + message to WhatsApp or messaging app
     */
    fun sendViaMessaging(
        context: Context,
        orderMessage: String,
        fileUri: Uri,
        mimeType: String = "application/pdf",
        phoneNumber: String? = null
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TEXT, orderMessage)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (!phoneNumber.isNullOrBlank()) {
                val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
                // If specific whatsapp destination
                setPackage("com.whatsapp")
                putExtra("jid", "$cleanPhone@s.whatsapp.net")
            }
        }

        try {
            val chooser = Intent.createChooser(intent, "Partager avec l'imprimeur")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Fallback without package lock
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_TEXT, orderMessage)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallback, "Partager le fichier"))
        }
    }

    /**
     * Generic share sheet with file and instructions
     */
    fun shareDocument(
        context: Context,
        orderMessage: String,
        fileUri: Uri,
        mimeType: String = "*/*"
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TEXT, orderMessage)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Partager pour impression")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
