package com.example.printer.connection

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import com.example.model.DiscoveredPrinter
import com.example.model.PrinterConnectionType
import com.example.printer.driver.DriverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsbPrinterManager(
    private val context: Context,
    private val driverRepository: DriverRepository
) {
    companion object {
        const val ACTION_USB_PERMISSION = "com.example.imprimedoc.USB_PERMISSION"
    }

    private val usbManager: UsbManager? =
        context.getSystemService(Context.USB_SERVICE) as? UsbManager

    /**
     * Checks if a UsbDevice is a printer based on USB Class 7 (PRINTER)
     * or known printer vendor IDs.
     */
    fun isPrinterDevice(device: UsbDevice): Boolean {
        // USB Class 7 is Printer
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                return true
            }
        }
        // Known printer vendor IDs
        val knownPrinterVendors = setOf(
            0x04b8, // Epson
            0x03f0, // HP
            0x04a9, // Canon
            0x04f9, // Brother
            0x0416, // Winbond / Xprinter
            0x1fc9, // NXP / Xprinter
            0x0fe6, // Corechip / POS
            0x0dd4, // Custom / POS
            0x0519, // Star Micronics
            0x0a5f, // Zebra
            0x1504, // Rongta
            0x28e9, // Gprinter
            0x1a86  // QinHeng USB-Serial / CH340 printers
        )
        return knownPrinterVendors.contains(device.vendorId)
    }

    /**
     * Enumerate all connected USB printers
     */
    fun getConnectedUsbPrinters(): List<DiscoveredPrinter> {
        val manager = usbManager ?: return emptyList()
        val deviceList = manager.deviceList
        val discovered = mutableListOf<DiscoveredPrinter>()

        for ((_, device) in deviceList) {
            if (isPrinterDevice(device)) {
                val hasPerm = manager.hasPermission(device)
                val mfr = device.manufacturerName ?: detectManufacturerFromVendorId(device.vendorId)
                val model = device.productName ?: "Imprimante USB #${device.deviceId}"
                val driver = driverRepository.autoDetectDriver(
                    manufacturer = mfr,
                    model = model,
                    vendorId = device.vendorId,
                    productId = device.productId
                )

                discovered.add(
                    DiscoveredPrinter(
                        id = "usb_${device.vendorId}_${device.productId}_${device.deviceId}",
                        name = "$mfr $model",
                        manufacturer = mfr,
                        model = model,
                        connectionType = PrinterConnectionType.USB,
                        address = "USB://${Integer.toHexString(device.vendorId)}:${Integer.toHexString(device.productId)}",
                        usbDeviceName = device.deviceName,
                        vendorId = device.vendorId,
                        productId = device.productId,
                        hasUsbPermission = hasPerm,
                        matchedDriver = driver,
                        isDriverInstalled = true,
                        statusText = if (hasPerm) "Prêt (Autorisé)" else "Autorisation requise"
                    )
                )
            }
        }
        return discovered
    }

    /**
     * Request runtime permission to access the USB printer
     */
    fun requestUsbPermission(device: UsbDevice, onResult: (Boolean) -> Unit) {
        val manager = usbManager ?: run {
            onResult(false)
            return
        }

        if (manager.hasPermission(device)) {
            onResult(true)
            return
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val intent = Intent(ACTION_USB_PERMISSION)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == ACTION_USB_PERMISSION) {
                    context.unregisterReceiver(this)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    onResult(granted)
                }
            }
        }

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        manager.requestPermission(device, pendingIntent)
    }

    /**
     * Sends raw print data to the USB device over Bulk OUT endpoint
     */
    suspend fun sendDataViaUsb(
        vendorId: Int,
        productId: Int,
        data: ByteArray,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val manager = usbManager ?: return@withContext Result.failure(Exception("Service USB non disponible sur cet appareil"))
        val device = manager.deviceList.values.find { it.vendorId == vendorId && it.productId == productId }
            ?: return@withContext Result.failure(Exception("Imprimante USB déconnectée"))

        if (!manager.hasPermission(device)) {
            return@withContext Result.failure(Exception("Permission USB non accordée pour cette imprimante"))
        }

        var targetInterface: UsbInterface? = null
        var targetEndpoint: UsbEndpoint? = null

        // Find bulk OUT endpoint
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            for (j in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(j)
                if (ep.direction == UsbConstants.USB_DIR_OUT && ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    targetInterface = intf
                    targetEndpoint = ep
                    break
                }
            }
            if (targetEndpoint != null) break
        }

        if (targetInterface == null || targetEndpoint == null) {
            return@withContext Result.failure(Exception("Point de terminaison (Endpoint Bulk OUT) introuvable sur l'imprimante"))
        }

        val connection = manager.openDevice(device)
            ?: return@withContext Result.failure(Exception("Impossible d'ouvrir la connexion avec l'imprimante USB"))

        try {
            if (!connection.claimInterface(targetInterface, true)) {
                return@withContext Result.failure(Exception("Impossible d'allouer l'interface de l'imprimante USB"))
            }

            val chunkSize = 4096
            var offset = 0
            val total = data.size

            while (offset < total) {
                val length = minOf(chunkSize, total - offset)
                val buffer = ByteArray(length)
                System.arraycopy(data, offset, buffer, 0, length)

                val transferred = connection.bulkTransfer(targetEndpoint, buffer, length, 5000)
                if (transferred < 0) {
                    return@withContext Result.failure(Exception("Échec du transfert USB lors de l'impression"))
                }
                offset += length
                onProgress(offset.toFloat() / total.toFloat())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                connection.releaseInterface(targetInterface)
                connection.close()
            } catch (_: Exception) {}
        }
    }

    private fun detectManufacturerFromVendorId(vendorId: Int): String {
        return when (vendorId) {
            0x04b8 -> "Epson"
            0x03f0 -> "HP"
            0x04a9 -> "Canon"
            0x04f9 -> "Brother"
            0x0416, 0x1fc9 -> "Xprinter"
            0x0519 -> "Star Micronics"
            0x0a5f -> "Zebra"
            0x1504 -> "Rongta"
            0x28e9 -> "Gprinter"
            else -> "Imprimante USB"
        }
    }
}
