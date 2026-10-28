package com.example.model

enum class PrinterProtocol(val displayName: String, val isThermal: Boolean) {
    ESC_POS_80("ESC/POS (Ticket 80mm)", true),
    ESC_POS_58("ESC/POS (Ticket 58mm)", true),
    PCL_3("HP PCL 3 GUI (Jet d'encre)", false),
    PCL_5("HP PCL 5 / 5e (Laser)", false),
    PCL_6("PCL 6 / PCL-XL (Laser rapide)", false),
    PCLM("PCLm Raster (AirPrint/Mopria)", false),
    PWG_RASTER("PWG Raster Direct", false),
    PDF_DIRECT("Pass-through PDF Direct", false),
    ANDROID_PRINT_SERVICE("Système d'impression Android", false)
}

enum class PrinterConnectionType(val label: String) {
    USB("USB-OTG"),
    WIFI("Wi-Fi / Réseau"),
    SYSTEM("Système")
}

data class DriverProfile(
    val id: String,
    val name: String,
    val manufacturer: String,
    val supportedFamily: String,
    val protocol: PrinterProtocol,
    val supportedPaperSizes: List<PaperSize> = listOf(PaperSize.A4, PaperSize.LETTER),
    val defaultPort: Int = 9100,
    val isDownloaded: Boolean = true,
    val isBuiltIn: Boolean = false,
    val version: String = "1.0",
    val description: String = ""
)

data class DiscoveredPrinter(
    val id: String,
    val name: String,
    val manufacturer: String,
    val model: String,
    val connectionType: PrinterConnectionType,
    val address: String, // e.g. "192.168.1.100:9100" or "USB://04b8:0e15"
    val ipAddress: String? = null,
    val port: Int = 9100,
    val usbDeviceName: String? = null,
    val vendorId: Int? = null,
    val productId: Int? = null,
    val hasUsbPermission: Boolean = false,
    val matchedDriver: DriverProfile? = null,
    val isDriverInstalled: Boolean = false,
    val statusText: String = "Prêt"
)

enum class PrintProgressStatus {
    IDLE,
    CONNECTING,
    PREPARING_DATA,
    TRANSMITTING,
    COMPLETED,
    ERROR
}

data class PrintProgress(
    val status: PrintProgressStatus = PrintProgressStatus.IDLE,
    val progress: Float = 0f,
    val message: String = "",
    val errorMessage: String? = null
)

enum class PrintDensity(val label: String, val multiplier: Float) {
    LIGHT("Clair", 0.8f),
    NORMAL("Normal", 1.0f),
    DARK("Foncé", 1.25f),
    VERY_DARK("Très foncé", 1.5f)
}
