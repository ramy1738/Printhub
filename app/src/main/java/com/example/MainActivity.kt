package com.example

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppLanguage
import com.example.ui.AppNavTab
import com.example.ui.MainViewModel
import com.example.ui.components.LanguageSelectionDialog
import com.example.ui.components.TopNavBar
import com.example.ui.screens.ComposeDocScreen
import com.example.ui.screens.DocumentPrintScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PhotoPrintScreen
import com.example.ui.screens.PrintShopsScreen
import com.example.ui.screens.PreviewItem
import com.example.ui.screens.PrintPreviewScreen
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import com.example.ui.theme.ImprimeDocTheme
import java.util.Locale

class LocalizedActivityContext(
    private val activity: ComponentActivity,
    private val localizedContext: Context
) : ContextWrapper(activity), ActivityResultRegistryOwner {
    override fun getResources(): Resources = localizedContext.resources
    override val activityResultRegistry: ActivityResultRegistry
        get() = activity.activityResultRegistry
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
            val activity = this@MainActivity

            // Dynamically provide localized context preserving ComponentActivity & ActivityResultRegistryOwner
            val (localizedContext, config) = remember(currentLanguage) {
                val locale = Locale(currentLanguage.code)
                Locale.setDefault(locale)
                val conf = Configuration(activity.resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
                val confContext = activity.createConfigurationContext(conf)
                Pair(LocalizedActivityContext(activity, confContext), conf)
            }

            val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides activity,
                LocalConfiguration provides config,
                LocalLayoutDirection provides layoutDirection
            ) {
                ImprimeDocTheme {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    val selectedPhotos by viewModel.selectedPhotos.collectAsStateWithLifecycle()
    val activePhotoIndex by viewModel.activePhotoIndex.collectAsStateWithLifecycle()
    val selectedDocument by viewModel.selectedDocument.collectAsStateWithLifecycle()
    val printOptions by viewModel.printOptions.collectAsStateWithLifecycle()

    val composedTitle by viewModel.composedTitle.collectAsStateWithLifecycle()
    val composedAuthor by viewModel.composedAuthor.collectAsStateWithLifecycle()
    val composedRecipient by viewModel.composedRecipient.collectAsStateWithLifecycle()
    val composedContent by viewModel.composedContent.collectAsStateWithLifecycle()

    val historyJobs by viewModel.historyJobs.collectAsStateWithLifecycle()
    val printShops by viewModel.printShops.collectAsStateWithLifecycle()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var activePreviewItem by remember { mutableStateOf<PreviewItem?>(null) }

    // Toast listener
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Predictive / Back handler: if preview is open, close it; otherwise return to Photos tab
    BackHandler(enabled = activePreviewItem != null || currentTab != AppNavTab.PHOTOS) {
        if (activePreviewItem != null) {
            activePreviewItem = null
        } else {
            viewModel.selectTab(AppNavTab.PHOTOS)
        }
    }

    if (activePreviewItem != null) {
        PrintPreviewScreen(
            previewItem = activePreviewItem!!,
            options = printOptions,
            savedShops = printShops,
            language = currentLanguage,
            onBack = { activePreviewItem = null },
            onOptionsChanged = { viewModel.updatePrintOptions(it) },
            onRecordJob = { title, uri, type, action, recipient, instructions ->
                viewModel.recordPrintJob(title, uri, type, action, recipient, instructions)
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopNavBar(
                    currentLanguage = currentLanguage,
                    onOpenLanguageDialog = { showLanguageDialog = true }
                )
            },
            bottomBar = {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppNavTab.PHOTOS,
                        onClick = { viewModel.selectTab(AppNavTab.PHOTOS) },
                        icon = { Icon(Icons.Default.Image, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_photos), fontSize = 11.sp) },
                        modifier = Modifier.testTag("tab_photos")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppNavTab.DOCUMENTS,
                        onClick = { viewModel.selectTab(AppNavTab.DOCUMENTS) },
                        icon = { Icon(Icons.Default.Description, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_documents), fontSize = 11.sp) },
                        modifier = Modifier.testTag("tab_documents")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppNavTab.CREATE,
                        onClick = { viewModel.selectTab(AppNavTab.CREATE) },
                        icon = { Icon(Icons.Default.Create, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_create), fontSize = 11.sp) },
                        modifier = Modifier.testTag("tab_create")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppNavTab.HISTORY,
                        onClick = { viewModel.selectTab(AppNavTab.HISTORY) },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_history), fontSize = 11.sp) },
                        modifier = Modifier.testTag("tab_history")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppNavTab.SHOPS,
                        onClick = { viewModel.selectTab(AppNavTab.SHOPS) },
                        icon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_shops), fontSize = 10.sp) },
                        modifier = Modifier.testTag("tab_shops")
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    AppNavTab.PHOTOS -> {
                        PhotoPrintScreen(
                            photos = selectedPhotos,
                            activePhotoIndex = activePhotoIndex,
                            options = printOptions,
                            savedShops = printShops,
                            language = currentLanguage,
                            onAddPhotos = { viewModel.addPhotos(it) },
                            onSelectActivePhoto = { viewModel.setActivePhotoIndex(it) },
                            onRemovePhoto = { viewModel.removePhoto(it) },
                            onClearPhotos = { viewModel.clearPhotos() },
                            onOptionsChanged = { viewModel.updatePrintOptions(it) },
                            onRecordJob = { title, uri, type, action, recipient, instructions ->
                                viewModel.recordPrintJob(title, uri, type, action, recipient, instructions)
                            },
                            onOpenPreview = { activePreviewItem = it }
                        )
                    }

                    AppNavTab.DOCUMENTS -> {
                        DocumentPrintScreen(
                            selectedDoc = selectedDocument,
                            options = printOptions,
                            savedShops = printShops,
                            language = currentLanguage,
                            onSelectDocument = { viewModel.setDocument(it, context) },
                            onClearDocument = { viewModel.clearDocument() },
                            onOptionsChanged = { viewModel.updatePrintOptions(it) },
                            onRecordJob = { title, uri, type, action, recipient, instructions ->
                                viewModel.recordPrintJob(title, uri, type, action, recipient, instructions)
                            },
                            onOpenPreview = { activePreviewItem = it }
                        )
                    }

                    AppNavTab.CREATE -> {
                        ComposeDocScreen(
                            title = composedTitle,
                            author = composedAuthor,
                            recipient = composedRecipient,
                            content = composedContent,
                            options = printOptions,
                            savedShops = printShops,
                            language = currentLanguage,
                            onTitleChange = { viewModel.setComposedTitle(it) },
                            onAuthorChange = { viewModel.setComposedAuthor(it) },
                            onRecipientChange = { viewModel.setComposedRecipient(it) },
                            onContentChange = { viewModel.setComposedContent(it) },
                            onClearDoc = { viewModel.clearComposedDoc() },
                            onOptionsChanged = { viewModel.updatePrintOptions(it) },
                            onRecordJob = { title, uri, type, action, recipient, instructions ->
                                viewModel.recordPrintJob(title, uri, type, action, recipient, instructions)
                            },
                            onOpenPreview = { activePreviewItem = it }
                        )
                    }

                    AppNavTab.HISTORY -> {
                        HistoryScreen(
                            jobs = historyJobs,
                            language = currentLanguage,
                            onDeleteJob = { viewModel.deleteHistoryJob(it) },
                            onClearAll = { viewModel.clearAllHistory() }
                        )
                    }

                    AppNavTab.SHOPS -> {
                        PrintShopsScreen(
                            shops = printShops,
                            language = currentLanguage,
                            onAddShop = { name, email, phone, notes ->
                                viewModel.addShop(name, email, phone, notes)
                            },
                            onDeleteShop = { viewModel.deleteShop(it) }
                        )
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onSelectLanguage = { viewModel.setLanguage(it) },
            onDismiss = { showLanguageDialog = false }
        )
    }
}
