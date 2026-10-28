package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.PrintShopEntity
import com.example.model.AppLanguage
import com.example.model.ColorMode
import com.example.model.DuplexMode
import com.example.model.FitMode
import com.example.model.JobActionType
import com.example.model.PageOrientation
import com.example.model.PaperSize
import com.example.model.PrintOptions
import com.example.model.PrintType
import com.example.ui.components.SendPrintBottomSheet
import com.example.util.PdfGeneratorHelper
import com.example.util.PrintManagerHelper
import com.example.util.ShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

sealed interface PreviewItem {
    data class PhotoItem(val uri: Uri, val title: String) : PreviewItem
    data class DocumentItem(val pdfUri: Uri, val title: String) : PreviewItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintPreviewScreen(
    previewItem: PreviewItem,
    options: PrintOptions,
    savedShops: List<PrintShopEntity>,
    language: AppLanguage,
    onBack: () -> Unit,
    onOptionsChanged: (PrintOptions) -> Unit,
    onRecordJob: (title: String, uri: Uri, type: PrintType, action: JobActionType, recipient: String?, instructions: String?) -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    var showSendSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // For PDF page rendering
    var renderedPages by remember { mutableStateOf<List<File>>(emptyList()) }
    var isRenderingPages by remember { mutableStateOf(false) }
    var currentPageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(previewItem) {
        if (previewItem is PreviewItem.DocumentItem) {
            isRenderingPages = true
            withContext(Dispatchers.IO) {
                val pages = PdfGeneratorHelper.renderPdfPagesToFiles(context, previewItem.pdfUri)
                renderedPages = pages
                currentPageIndex = 0
                isRenderingPages = false
            }
        }
    }

    // Interactive zoom & pan states for Coil viewer
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    val totalPages = when (previewItem) {
        is PreviewItem.PhotoItem -> 1
        is PreviewItem.DocumentItem -> maxOf(1, renderedPages.size)
    }

    val paperAspectRatio = remember(options.paperSize, options.orientation) {
        val baseRatio = when (options.paperSize) {
            PaperSize.PHOTO_4X6 -> 4f / 6f
            PaperSize.A4 -> 210f / 297f
            PaperSize.A3 -> 297f / 420f
            PaperSize.LETTER -> 8.5f / 11f
        }
        if (options.orientation == PageOrientation.LANDSCAPE) (1f / baseRatio) else baseRatio
    }

    // Color matrix for monochrome simulation
    val colorFilter = remember(options.colorMode) {
        if (options.colorMode == ColorMode.MONOCHROME) {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.preview_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val subtitleText = when (previewItem) {
                            is PreviewItem.PhotoItem -> previewItem.title
                            is PreviewItem.DocumentItem -> stringResource(
                                R.string.preview_page_indicator,
                                currentPageIndex + 1,
                                totalPages
                            )
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_preview_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    if (scale > 1.05f || offsetX != 0f || offsetY != 0f) {
                        IconButton(onClick = { resetZoom() }) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = stringResource(R.string.preview_reset_zoom),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Pre-order confirmation card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Quick specs & Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color toggle chip
                            FilterChip(
                                selected = options.colorMode == ColorMode.COLOR,
                                onClick = {
                                    val nextMode = if (options.colorMode == ColorMode.COLOR) ColorMode.MONOCHROME else ColorMode.COLOR
                                    onOptionsChanged(options.copy(colorMode = nextMode))
                                },
                                label = {
                                    Text(
                                        if (options.colorMode == ColorMode.COLOR) stringResource(R.string.color_full) else stringResource(R.string.color_bw),
                                        fontSize = 11.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (options.colorMode == ColorMode.COLOR) Icons.Default.ColorLens else Icons.Default.FormatAlignJustify,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )

                            // Fit/Fill toggle chip (for photos)
                            if (previewItem is PreviewItem.PhotoItem) {
                                FilterChip(
                                    selected = options.fitMode == FitMode.FILL_PAGE,
                                    onClick = {
                                        val nextFit = if (options.fitMode == FitMode.FILL_PAGE) FitMode.FIT_PAGE else FitMode.FILL_PAGE
                                        onOptionsChanged(options.copy(fitMode = nextFit))
                                    },
                                    label = {
                                        Text(
                                            if (options.fitMode == FitMode.FILL_PAGE) stringResource(R.string.fill_page) else stringResource(R.string.fit_page),
                                            fontSize = 11.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                )
                            }
                        }

                        // Copies stepper
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (options.copies > 1) {
                                        onOptionsChanged(options.copy(copies = options.copies - 1))
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = "${options.copies}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = { onOptionsChanged(options.copy(copies = options.copies + 1)) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dual Confirmation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { showSendSheet = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_preview_confirm_send"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.preview_confirm_send), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                when (previewItem) {
                                    is PreviewItem.PhotoItem -> {
                                        val bitmap = PrintManagerHelper.loadBitmapFromUri(context, previewItem.uri)
                                        if (bitmap != null) {
                                            PrintManagerHelper.printBitmap(
                                                context = context,
                                                bitmap = bitmap,
                                                jobName = previewItem.title,
                                                printOptions = options
                                            ) { success, err ->
                                                if (success) {
                                                    Toast.makeText(context, context.getString(R.string.alert_print_started), Toast.LENGTH_SHORT).show()
                                                    onRecordJob(previewItem.title, previewItem.uri, PrintType.PHOTO, JobActionType.PRINTED, "Imprimante directe", options.customNotes)
                                                    onBack()
                                                } else {
                                                    Toast.makeText(context, err ?: "Erreur d'impression", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                    is PreviewItem.DocumentItem -> {
                                        PrintManagerHelper.printPdfUri(
                                            context = context,
                                            pdfUri = previewItem.pdfUri,
                                            jobName = previewItem.title,
                                            printOptions = options
                                        ) { success, err ->
                                            if (success) {
                                                Toast.makeText(context, context.getString(R.string.alert_print_started), Toast.LENGTH_SHORT).show()
                                                onRecordJob(previewItem.title, previewItem.pdfUri, PrintType.PDF, JobActionType.PRINTED, "Imprimante directe", options.customNotes)
                                                onBack()
                                            } else {
                                                Toast.makeText(context, err ?: "Erreur d'impression", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_preview_confirm_print"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.preview_confirm_print), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom hint and controls bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.preview_zoom_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray.copy(alpha = 0.8f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { scale = maxOf(0.7f, scale - 0.25f) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom arrière", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "${(scale * 100).roundToInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { scale = minOf(4f, scale + 0.25f) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom avant", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Paper canvas container with Pinch-to-Zoom & Pan
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.7f, 4.0f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.1f) resetZoom() else scale = 2.2f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val maxPaperWidth = maxWidth * 0.86f
                val maxPaperHeight = maxHeight * 0.90f

                // Simulated Physical Paper Sheet with shadow
                Card(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .width(maxPaperWidth)
                        .aspectRatio(paperAspectRatio)
                        .shadow(16.dp, shape = RoundedCornerShape(4.dp))
                        .testTag("preview_paper_sheet"),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Simulated printable margin border guide
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFCBD5E1).copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )

                        // Coil-based Image Viewer
                        when (previewItem) {
                            is PreviewItem.PhotoItem -> {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(previewItem.uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = stringResource(R.string.preview_title),
                                    contentScale = if (options.fitMode == FitMode.FILL_PAGE) ContentScale.Crop else ContentScale.Fit,
                                    colorFilter = colorFilter,
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (options.fitMode == FitMode.FILL_PAGE) 0.dp else 12.dp)
                                        .testTag("coil_photo_preview")
                                )
                            }

                            is PreviewItem.DocumentItem -> {
                                if (isRenderingPages) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Rendu des pages...", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    val currentFile = renderedPages.getOrNull(currentPageIndex)
                                    if (currentFile != null && currentFile.exists()) {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(currentFile)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Page ${currentPageIndex + 1}",
                                            contentScale = ContentScale.Fit,
                                            colorFilter = colorFilter,
                                            loading = {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp)
                                                .testTag("coil_doc_page_preview")
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Document prêt pour impression", color = Color.Gray, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Watermark / Format Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${options.paperSize.name} • ${if (options.colorMode == ColorMode.COLOR) "Couleur" else "N&B"}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Multi-page stepper for documents
            if (previewItem is PreviewItem.DocumentItem && totalPages > 1) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (currentPageIndex > 0) {
                                currentPageIndex--
                                resetZoom()
                            }
                        },
                        enabled = currentPageIndex > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = "Page précédente",
                            tint = if (currentPageIndex > 0) Color.White else Color.Gray
                        )
                    }

                    Text(
                        text = stringResource(R.string.preview_page_indicator, currentPageIndex + 1, totalPages),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    IconButton(
                        onClick = {
                            if (currentPageIndex < totalPages - 1) {
                                currentPageIndex++
                                resetZoom()
                            }
                        },
                        enabled = currentPageIndex < totalPages - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = "Page suivante",
                            tint = if (currentPageIndex < totalPages - 1) Color.White else Color.Gray
                        )
                    }
                }
            }
        }
    }

    // Send Print Bottom Sheet
    if (showSendSheet) {
        val (targetTitle, targetUri, targetMime) = when (previewItem) {
            is PreviewItem.PhotoItem -> Triple(previewItem.title, previewItem.uri, "image/*")
            is PreviewItem.DocumentItem -> Triple(previewItem.title, previewItem.pdfUri, "application/pdf")
        }

        SendPrintBottomSheet(
            sheetState = sheetState,
            jobTitle = targetTitle,
            options = options,
            savedShops = savedShops,
            language = language,
            onDismiss = { showSendSheet = false },
            onSendViaApp = { instructions ->
                showSendSheet = false
                val msg = ShareHelper.buildOrderMessage(targetTitle, options.copy(customNotes = instructions), language)
                ShareHelper.shareDocument(context, msg, targetUri, targetMime)
                onRecordJob(targetTitle, targetUri, if (previewItem is PreviewItem.PhotoItem) PrintType.PHOTO else PrintType.PDF, JobActionType.SENT, "Partage d'application", instructions)
                onBack()
            },
            onSendViaEmail = { email, instructions ->
                showSendSheet = false
                val msg = ShareHelper.buildOrderMessage(targetTitle, options.copy(customNotes = instructions), language)
                ShareHelper.sendViaEmail(context, email, targetTitle, msg, targetUri, targetMime)
                onRecordJob(targetTitle, targetUri, if (previewItem is PreviewItem.PhotoItem) PrintType.PHOTO else PrintType.PDF, JobActionType.SENT, email, instructions)
                onBack()
            }
        )
    }
}
