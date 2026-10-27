package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PrintJobEntity
import com.example.data.PrintShopEntity
import com.example.model.AppLanguage
import com.example.model.JobActionType
import com.example.model.PrintOptions
import com.example.model.PrintType
import com.example.util.PdfGeneratorHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SelectedDocumentInfo(
    val uri: Uri,
    val name: String,
    val sizeText: String,
    val pageCount: Int,
    val thumbnail: Bitmap?
)

enum class AppNavTab {
    PHOTOS,
    DOCUMENTS,
    CREATE,
    HISTORY,
    SHOPS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val printJobDao = db.printJobDao()
    private val printShopDao = db.printShopDao()

    private val prefs = application.getSharedPreferences("imprime_doc_prefs", Context.MODE_PRIVATE)

    // Language state - FRENCH by default!
    private val _currentLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("pref_language", "fr") ?: "fr")
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Navigation Tab
    private val _currentTab = MutableStateFlow(AppNavTab.PHOTOS)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    // Photos state
    private val _selectedPhotos = MutableStateFlow<List<Uri>>(emptyList())
    val selectedPhotos: StateFlow<List<Uri>> = _selectedPhotos.asStateFlow()

    private val _activePhotoIndex = MutableStateFlow(0)
    val activePhotoIndex: StateFlow<Int> = _activePhotoIndex.asStateFlow()

    // Document state
    private val _selectedDocument = MutableStateFlow<SelectedDocumentInfo?>(null)
    val selectedDocument: StateFlow<SelectedDocumentInfo?> = _selectedDocument.asStateFlow()

    // Composed document state
    private val _composedTitle = MutableStateFlow("")
    val composedTitle: StateFlow<String> = _composedTitle.asStateFlow()

    private val _composedAuthor = MutableStateFlow("")
    val composedAuthor: StateFlow<String> = _composedAuthor.asStateFlow()

    private val _composedRecipient = MutableStateFlow("")
    val composedRecipient: StateFlow<String> = _composedRecipient.asStateFlow()

    private val _composedContent = MutableStateFlow("")
    val composedContent: StateFlow<String> = _composedContent.asStateFlow()

    // Print Options
    private val _printOptions = MutableStateFlow(PrintOptions())
    val printOptions: StateFlow<PrintOptions> = _printOptions.asStateFlow()

    // Room DB Flows
    val historyJobs = printJobDao.getAllJobs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val printShops = printShopDao.getAllShops().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // User notification events
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("pref_language", language.code).apply()
    }

    fun selectTab(tab: AppNavTab) {
        _currentTab.value = tab
    }

    fun updatePrintOptions(newOptions: PrintOptions) {
        _printOptions.value = newOptions
    }

    // Photo Management
    fun addPhotos(uris: List<Uri>) {
        val current = _selectedPhotos.value.toMutableList()
        for (uri in uris) {
            if (!current.contains(uri)) {
                current.add(uri)
            }
        }
        _selectedPhotos.value = current
        if (_activePhotoIndex.value >= current.size) {
            _activePhotoIndex.value = 0
        }
    }

    fun setActivePhotoIndex(index: Int) {
        if (index in 0 until _selectedPhotos.value.size) {
            _activePhotoIndex.value = index
        }
    }

    fun removePhoto(uri: Uri) {
        val current = _selectedPhotos.value.toMutableList()
        current.remove(uri)
        _selectedPhotos.value = current
        if (_activePhotoIndex.value >= current.size) {
            _activePhotoIndex.value = maxOf(0, current.size - 1)
        }
    }

    fun clearPhotos() {
        _selectedPhotos.value = emptyList()
        _activePhotoIndex.value = 0
    }

    // Document Management
    fun setDocument(uri: Uri, context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var name = "document.pdf"
                var sizeText = "Inconnu"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            name = cursor.getString(nameIndex) ?: name
                        }
                        if (sizeIndex != -1) {
                            val sizeBytes = cursor.getLong(sizeIndex)
                            sizeText = when {
                                sizeBytes > 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes / (1024f * 1024f))
                                sizeBytes > 1024 -> "${sizeBytes / 1024} KB"
                                else -> "$sizeBytes B"
                            }
                        }
                    }
                }

                val (pageCount, thumbnail) = PdfGeneratorHelper.inspectPdf(context, uri)
                _selectedDocument.value = SelectedDocumentInfo(
                    uri = uri,
                    name = name,
                    sizeText = sizeText,
                    pageCount = pageCount,
                    thumbnail = thumbnail
                )
            }
        }
    }

    fun clearDocument() {
        _selectedDocument.value = null
    }

    // Composed document setters
    fun setComposedTitle(title: String) { _composedTitle.value = title }
    fun setComposedAuthor(author: String) { _composedAuthor.value = author }
    fun setComposedRecipient(recipient: String) { _composedRecipient.value = recipient }
    fun setComposedContent(content: String) { _composedContent.value = content }

    fun clearComposedDoc() {
        _composedTitle.value = ""
        _composedAuthor.value = ""
        _composedRecipient.value = ""
        _composedContent.value = ""
    }

    // Database Actions
    fun recordPrintJob(
        title: String,
        uri: Uri,
        type: PrintType,
        action: JobActionType,
        recipient: String? = null,
        instructions: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val opts = _printOptions.value
            val entity = PrintJobEntity(
                title = title,
                fileUriString = uri.toString(),
                printType = type.name,
                actionType = action.name,
                copies = opts.copies,
                colorMode = opts.colorMode.name,
                paperSize = opts.paperSize.name,
                duplexMode = opts.duplexMode.name,
                bindingOption = opts.bindingOption.name,
                recipient = recipient,
                instructions = instructions ?: opts.customNotes
            )
            printJobDao.insertJob(entity)
        }
    }

    fun deleteHistoryJob(job: PrintJobEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            printJobDao.deleteJob(job)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            printJobDao.clearAll()
        }
    }

    fun addShop(name: String, email: String, phone: String, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            printShopDao.insertShop(
                PrintShopEntity(
                    name = name,
                    email = email,
                    phone = phone,
                    notes = notes
                )
            )
        }
    }

    fun deleteShop(shop: PrintShopEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            printShopDao.deleteShop(shop)
        }
    }

    fun emitToast(message: String) {
        viewModelScope.launch {
            _toastEvent.emit(message)
        }
    }
}
