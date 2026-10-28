package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PrintShopEntity
import com.example.model.AppLanguage
import com.example.model.JobActionType
import com.example.model.PrintOptions
import com.example.model.PrintType
import com.example.ui.components.PrintOptionsCard
import com.example.ui.components.SendPrintBottomSheet
import com.example.util.PdfGeneratorHelper
import com.example.util.PrintManagerHelper
import com.example.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeDocScreen(
    title: String,
    author: String,
    recipient: String,
    content: String,
    options: PrintOptions,
    savedShops: List<PrintShopEntity>,
    language: AppLanguage,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onRecipientChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onClearDoc: () -> Unit,
    onOptionsChanged: (PrintOptions) -> Unit,
    onRecordJob: (title: String, uri: Uri, type: PrintType, action: JobActionType, recipient: String?, instructions: String?) -> Unit,
    onOpenPreview: (PreviewItem) -> Unit
) {
    val context = LocalContext.current
    var showSendSheet by remember { mutableStateOf(false) }
    var generatedFileUri by remember { mutableStateOf<Uri?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun generateCurrentPdf(): Uri {
        val file = PdfGeneratorHelper.createComposedPdf(
            context = context,
            title = title,
            author = author,
            recipient = recipient,
            bodyContent = content,
            language = language
        )
        val uri = PdfGeneratorHelper.getSharableUri(context, file)
        generatedFileUri = uri
        return uri
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 90.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.create_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(R.string.create_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (title.isNotBlank() || content.isNotBlank()) {
                    IconButton(onClick = onClearDoc) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Template Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        when (language) {
                            AppLanguage.ARABIC -> {
                                onTitleChange("إفادة رسمية")
                                onContentChange("يشهد الموقع أدناه بأن السيد/السيدة قد أتم متطلبات التدريب بنجاح، وسُلّمت له هذه الإفادة للإدلاء بها عند الحاجة.")
                            }
                            AppLanguage.ENGLISH -> {
                                onTitleChange("Official Certificate")
                                onContentChange("This is to certify that the undersigned has successfully completed the program. Issued for all legal and official purposes.")
                            }
                            else -> {
                                onTitleChange("Attestation Officielle")
                                onContentChange("Je soussigné certifie par la présente que le titulaire a complété l'ensemble des formalités avec succès. Fait pour valoir ce que de droit.")
                            }
                        }
                    },
                    label = { Text("Attestation", fontSize = 11.sp) }
                )

                FilterChip(
                    selected = false,
                    onClick = {
                        when (language) {
                            AppLanguage.ARABIC -> {
                                onTitleChange("فاتورة خدمات")
                                onContentChange("فاتورة رقم: 2026-001\nالخدمة: تصميم ومطبوعات مكتبية\nالمبلغ الإجمالي: 1500 درهم\nشروط الدفع: عند الاستلام")
                            }
                            AppLanguage.ENGLISH -> {
                                onTitleChange("Service Invoice")
                                onContentChange("Invoice #2026-001\nService: Professional printing & graphic design\nTotal Amount: $150.00\nPayment due upon delivery.")
                            }
                            else -> {
                                onTitleChange("Facture de Prestation")
                                onContentChange("Facture N° : 2026-001\nPrestation : Travaux d'impression et reprographie\nMontant total : 150,00 €\nRèglement à réception.")
                            }
                        }
                    },
                    label = { Text("Facture", fontSize = 11.sp) }
                )

                FilterChip(
                    selected = false,
                    onClick = {
                        when (language) {
                            AppLanguage.ARABIC -> {
                                onTitleChange("خطاب رسمي")
                                onContentChange("السلام عليكم ورحمة الله وبركاته،\n\nنود إحاطتكم علماً بالمستجدات التالية المتعلقة بملفكم...\n\nوتفضلوا بقبول فائق التقدير والاحترام.")
                            }
                            AppLanguage.ENGLISH -> {
                                onTitleChange("Formal Letter")
                                onContentChange("Dear Sir/Madam,\n\nI am writing to formally present our proposal regarding the project...\n\nSincerely,")
                            }
                            else -> {
                                onTitleChange("Courrier Officiel")
                                onContentChange("Madame, Monsieur,\n\nPar la présente, je vous fais part de ma demande concernant...\n\nVeuillez agréer mes salutations distinguées.")
                            }
                        }
                    },
                    label = { Text("Lettre", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Form inputs
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text(stringResource(R.string.field_doc_title)) },
                        placeholder = { Text(stringResource(R.string.field_doc_title_hint)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_doc_title")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = author,
                            onValueChange = onAuthorChange,
                            label = { Text(stringResource(R.string.field_doc_author)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_doc_author")
                        )

                        OutlinedTextField(
                            value = recipient,
                            onValueChange = onRecipientChange,
                            label = { Text(stringResource(R.string.field_doc_recipient)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_doc_recipient")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = content,
                        onValueChange = onContentChange,
                        label = { Text(stringResource(R.string.field_doc_content)) },
                        placeholder = { Text(stringResource(R.string.field_doc_content_hint)) },
                        minLines = 6,
                        maxLines = 12,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_doc_content")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val uri = generateCurrentPdf()
                            val safeTitle = if (title.isBlank()) "Document" else title
                            onOpenPreview(PreviewItem.DocumentItem(uri, safeTitle))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("btn_open_composed_preview"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_open_preview), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Print Options Card
            PrintOptionsCard(
                options = options,
                isPhotoMode = false,
                onOptionsChanged = onOptionsChanged
            )
        }

        // Bottom Action Bar
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val uri = generateCurrentPdf()
                        showSendSheet = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_composed_send_print"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_generate_send), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val uri = generateCurrentPdf()
                        val jobName = if (title.isBlank()) "Document" else title
                        PrintManagerHelper.printPdfUri(
                            context = context,
                            pdfUri = uri,
                            jobName = jobName,
                            printOptions = options
                        ) { success, err ->
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.alert_print_started), Toast.LENGTH_SHORT).show()
                                onRecordJob(
                                    jobName,
                                    uri,
                                    PrintType.COMPOSED,
                                    JobActionType.PRINTED,
                                    "Imprimante directe",
                                    options.customNotes
                                )
                            } else {
                                Toast.makeText(context, err ?: "Erreur d'impression", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_composed_print_now"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_generate_pdf), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Send Print Bottom Sheet
        if (showSendSheet && generatedFileUri != null) {
            val targetUri = generatedFileUri!!
            val safeTitle = if (title.isBlank()) "Document_PDF" else title
            SendPrintBottomSheet(
                sheetState = sheetState,
                jobTitle = safeTitle,
                options = options,
                savedShops = savedShops,
                language = language,
                onDismiss = { showSendSheet = false },
                onSendViaApp = { instructions ->
                    showSendSheet = false
                    val msg = ShareHelper.buildOrderMessage(safeTitle, options.copy(customNotes = instructions), language)
                    ShareHelper.shareDocument(context, msg, targetUri, "application/pdf")
                    onRecordJob(safeTitle, targetUri, PrintType.COMPOSED, JobActionType.SENT, "Partage d'application", instructions)
                },
                onSendViaEmail = { email, instructions ->
                    showSendSheet = false
                    val msg = ShareHelper.buildOrderMessage(safeTitle, options.copy(customNotes = instructions), language)
                    ShareHelper.sendViaEmail(context, email, safeTitle, msg, targetUri, "application/pdf")
                    onRecordJob(safeTitle, targetUri, PrintType.COMPOSED, JobActionType.SENT, email, instructions)
                }
            )
        }
    }
}
