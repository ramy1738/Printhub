package com.example.printer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.model.DiscoveredPrinter
import com.example.model.DriverProfile
import com.example.model.PaperSize
import com.example.model.PrintOptions
import com.example.model.PrintProgress
import com.example.model.PrintProgressStatus
import com.example.model.PrinterConnectionType
import com.example.model.PrinterProtocol
import com.example.printer.connection.NetworkPrinterScanner
import com.example.printer.connection.UsbPrinterManager
import com.example.printer.driver.DriverRepository
import com.example.printer.engine.StandardDocumentPrintingEngine
import com.example.printer.engine.ThermalReceiptPrintingEngine
import com.example.util.PdfGeneratorHelper
import com.example.util.PrintManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class PrintController(private val context: Context) {

    val driverRepository = DriverRepository(context)
    val usbManager = UsbPrinterManager(context, driverRepository)
    val networkScanner = NetworkPrinterScanner(context, driverRepository)

    // Active printer and driver
    private val _selectedPrinter = MutableStateFlow<DiscoveredPrinter?>(null)
    val selectedPrinter: StateFlow<DiscoveredPrinter?> = _selectedPrinter.asStateFlow()

    private val _selectedDriver = MutableStateFlow<DriverProfile>(
        driverRepository.getInstalledDrivers().first { it.id == "drv_android_system" }
    )
    val selectedDriver: StateFlow<DriverProfile> = _selectedDriver.asStateFlow()

    // Discovery lists
    private val _discoveredPrinters = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val discoveredPrinters: StateFlow<List<DiscoveredPrinter>> = _discoveredPrinters.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Print Job Progress
    private val _printProgress = MutableStateFlow(PrintProgress())
    val printProgress: StateFlow<PrintProgress> = _printProgress.asStateFlow()

    init {
        refreshPrinters()
    }

    /**
     * Refresh both USB connected devices and saved network printers
     */
    fun refreshPrinters() {
        val usbList = usbManager.getConnectedUsbPrinters()
        val netList = networkScanner.getSavedNetworkPrinters()
        val systemPrinter = DiscoveredPrinter(
            id = "sys_android_print",
            name = "Service Système Android (Tous modèles)",
            manufacturer = "Android",
            model = "Print Framework",
            connectionType = PrinterConnectionType.SYSTEM,
            address = "Système",
            matchedDriver = driverRepository.getInstalledDrivers().first { it.id == "drv_android_system" },
            isDriverInstalled = true,
            statusText = "Disponible"
        )

        val combined = mutableListOf<DiscoveredPrinter>()
        // Prioritize USB if connected
        combined.addAll(usbList)
        combined.addAll(netList)
        combined.add(systemPrinter)

        _discoveredPrinters.value = combined

        // If no printer selected yet, auto-select first USB or first network or system
        if (_selectedPrinter.value == null) {
            val first = usbList.firstOrNull() ?: netList.firstOrNull() ?: systemPrinter
            selectPrinter(first)
        }
    }

    /**
     * Start active Wi-Fi subnet scanning
     */
    suspend fun startNetworkScan() {
        _isScanning.value = true
        try {
            networkScanner.scanLocalSubnet { foundPrinter ->
                val current = _discoveredPrinters.value.toMutableList()
                if (current.none { it.id == foundPrinter.id }) {
                    current.add(0, foundPrinter)
                    _discoveredPrinters.value = current
                }
            }
        } finally {
            _isScanning.value = false
        }
    }

    fun selectPrinter(printer: DiscoveredPrinter) {
        _selectedPrinter.value = printer
        val driver = printer.matchedDriver
            ?: driverRepository.autoDetectDriver(printer.manufacturer, printer.model, printer.vendorId, printer.productId)
        _selectedDriver.value = driver
    }

    fun selectDriver(driver: DriverProfile) {
        _selectedDriver.value = driver
        val printer = _selectedPrinter.value
        if (printer != null) {
            _selectedPrinter.value = printer.copy(matchedDriver = driver)
        }
    }

    fun addManualPrinter(mfr: String, model: String, ip: String, port: Int = 9100) {
        networkScanner.saveNetworkPrinter(mfr, model, ip, port)
        refreshPrinters()
        val saved = _discoveredPrinters.value.find { it.ipAddress == ip && it.port == port }
        if (saved != null) {
            selectPrinter(saved)
        }
    }

    fun removeSavedPrinter(printer: DiscoveredPrinter) {
        if (printer.ipAddress != null) {
            networkScanner.removeNetworkPrinter(printer.ipAddress, printer.port)
            refreshPrinters()
        }
    }

    /**
     * Executes the direct print job using the active printer and driver
     */
    suspend fun executePrintJob(
        title: String,
        pages: List<Bitmap>,
        pdfUri: Uri?,
        options: PrintOptions,
        onComplete: (Boolean, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        val printer = _selectedPrinter.value ?: run {
            onComplete(false, "Aucune imprimante sélectionnée")
            return@withContext
        }
        val driver = _selectedDriver.value

        // 1. Android System Service fallback
        if (driver.protocol == PrinterProtocol.ANDROID_PRINT_SERVICE || printer.connectionType == PrinterConnectionType.SYSTEM) {
            withContext(Dispatchers.Main) {
                if (pdfUri != null) {
                    PrintManagerHelper.printPdfUri(context, pdfUri, title, options, onComplete)
                } else if (pages.isNotEmpty()) {
                    PrintManagerHelper.printBitmap(context, pages.first(), title, options, onComplete)
                } else {
                    onComplete(false, "Aucune donnée à imprimer")
                }
            }
            return@withContext
        }

        try {
            _printProgress.value = PrintProgress(
                status = PrintProgressStatus.CONNECTING,
                progress = 0.1f,
                message = "Connexion à ${printer.name}..."
            )

            // 2. Generate Protocol Data Stream
            _printProgress.value = PrintProgress(
                status = PrintProgressStatus.PREPARING_DATA,
                progress = 0.35f,
                message = "Préparation des données (${driver.protocol.displayName})..."
            )

            val printBytes: ByteArray = when {
                driver.protocol.isThermal -> {
                    // Thermal Engine: render pages as high contrast raster with Arabic RTL shaping
                    val paperSize = if (driver.protocol == PrinterProtocol.ESC_POS_58) PaperSize.ROLL_58MM else PaperSize.ROLL_80MM
                    val widthDots = ThermalReceiptPrintingEngine.getDotsWidth(paperSize)

                    val combinedBytes = java.io.ByteArrayOutputStream()
                    for (page in pages) {
                        val raster = ThermalReceiptPrintingEngine.convertBitmapToEscPosRaster(
                            sourceBitmap = page,
                            targetWidthDots = widthDots,
                            densityMultiplier = 1.0f,
                            autoCut = true
                        )
                        combinedBytes.write(raster)
                    }
                    combinedBytes.toByteArray()
                }

                driver.protocol == PrinterProtocol.PDF_DIRECT && pdfUri != null -> {
                    // Direct PDF pass-through for enterprise laser printers
                    context.contentResolver.openInputStream(pdfUri)?.use { it.readBytes() }
                        ?: StandardDocumentPrintingEngine.generatePclStream(pages, options, driver.protocol)
                }

                else -> {
                    // Standard Document Engine: PCL 3/5/6 / PWG
                    StandardDocumentPrintingEngine.generatePclStream(pages, options, driver.protocol)
                }
            }

            // 3. Transmit to Printer (USB or Wi-Fi Socket)
            _printProgress.value = PrintProgress(
                status = PrintProgressStatus.TRANSMITTING,
                progress = 0.6f,
                message = "Transmission des données d'impression..."
            )

            val transmitResult = when (printer.connectionType) {
                PrinterConnectionType.USB -> {
                    val vid = printer.vendorId ?: return@withContext onComplete(false, "VendorID USB manquant")
                    val pid = printer.productId ?: return@withContext onComplete(false, "ProductID USB manquant")
                    usbManager.sendDataViaUsb(vid, pid, printBytes) { ratio ->
                        _printProgress.value = PrintProgress(
                            status = PrintProgressStatus.TRANSMITTING,
                            progress = 0.6f + (ratio * 0.35f),
                            message = "Envoi : ${(ratio * 100).toInt()}%"
                        )
                    }
                }
                PrinterConnectionType.WIFI -> {
                    val ip = printer.ipAddress ?: return@withContext onComplete(false, "Adresse IP manquante")
                    networkScanner.sendDataViaSocket(ip, printer.port, printBytes) { ratio ->
                        _printProgress.value = PrintProgress(
                            status = PrintProgressStatus.TRANSMITTING,
                            progress = 0.6f + (ratio * 0.35f),
                            message = "Envoi : ${(ratio * 100).toInt()}%"
                        )
                    }
                }
                PrinterConnectionType.SYSTEM -> Result.success(Unit)
            }

            if (transmitResult.isSuccess) {
                _printProgress.value = PrintProgress(
                    status = PrintProgressStatus.COMPLETED,
                    progress = 1.0f,
                    message = "Impression envoyée avec succès !"
                )
                withContext(Dispatchers.Main) {
                    onComplete(true, null)
                }
            } else {
                val err = transmitResult.exceptionOrNull()?.localizedMessage ?: "Erreur de transmission"
                _printProgress.value = PrintProgress(
                    status = PrintProgressStatus.ERROR,
                    progress = 0f,
                    message = "Échec de l'impression",
                    errorMessage = err
                )
                withContext(Dispatchers.Main) {
                    onComplete(false, err)
                }
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Erreur inattendue"
            _printProgress.value = PrintProgress(
                status = PrintProgressStatus.ERROR,
                progress = 0f,
                message = "Erreur",
                errorMessage = msg
            )
            withContext(Dispatchers.Main) {
                onComplete(false, msg)
            }
        }
    }

    fun dismissProgress() {
        _printProgress.value = PrintProgress(status = PrintProgressStatus.IDLE)
    }
}
