package com.example.printer.connection

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import com.example.model.DiscoveredPrinter
import com.example.model.PrinterConnectionType
import com.example.printer.driver.DriverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class NetworkPrinterScanner(
    private val context: Context,
    private val driverRepository: DriverRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("noko_network_printers_prefs", Context.MODE_PRIVATE)

    /**
     * Retrieves saved manual network printers
     */
    fun getSavedNetworkPrinters(): List<DiscoveredPrinter> {
        val jsonStr = prefs.getString("saved_network_printers", null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<DiscoveredPrinter>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val mfr = obj.optString("manufacturer", "Réseau")
                val model = obj.optString("model", "Imprimante Wi-Fi")
                val ip = obj.getString("ipAddress")
                val port = obj.optInt("port", 9100)
                val driver = driverRepository.autoDetectDriver(mfr, model)

                list.add(
                    DiscoveredPrinter(
                        id = "net_${ip}_$port",
                        name = "$mfr $model ($ip)",
                        manufacturer = mfr,
                        model = model,
                        connectionType = PrinterConnectionType.WIFI,
                        address = "$ip:$port",
                        ipAddress = ip,
                        port = port,
                        matchedDriver = driver,
                        isDriverInstalled = true,
                        statusText = "Configuré"
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Saves a manually configured printer
     */
    fun saveNetworkPrinter(mfr: String, model: String, ip: String, port: Int = 9100) {
        val current = getSavedNetworkPrinters().toMutableList()
        current.removeAll { it.ipAddress == ip && it.port == port }
        val array = JSONArray()
        for (p in current) {
            val obj = JSONObject()
            obj.put("manufacturer", p.manufacturer)
            obj.put("model", p.model)
            obj.put("ipAddress", p.ipAddress)
            obj.put("port", p.port)
            array.put(obj)
        }
        val newObj = JSONObject()
        newObj.put("manufacturer", mfr)
        newObj.put("model", model)
        newObj.put("ipAddress", ip)
        newObj.put("port", port)
        array.put(newObj)
        prefs.edit().putString("saved_network_printers", array.toString()).apply()
    }

    fun removeNetworkPrinter(ip: String, port: Int) {
        val current = getSavedNetworkPrinters().filterNot { it.ipAddress == ip && it.port == port }
        val array = JSONArray()
        for (p in current) {
            val obj = JSONObject()
            obj.put("manufacturer", p.manufacturer)
            obj.put("model", p.model)
            obj.put("ipAddress", p.ipAddress)
            obj.put("port", p.port)
            array.put(obj)
        }
        prefs.edit().putString("saved_network_printers", array.toString()).apply()
    }

    /**
     * Scans the local network subnet for printers responding on raw port 9100, 631 (IPP) or 515 (LPR)
     */
    suspend fun scanLocalSubnet(onFound: (DiscoveredPrinter) -> Unit): List<DiscoveredPrinter> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<DiscoveredPrinter>()
            val subnetPrefix = getSubnetPrefix() ?: "192.168.1."

            // Scan hosts in parallel with batches
            val targetPorts = listOf(9100, 631)
            val batchSize = 25

            coroutineScope {
                for (chunkStart in 1..254 step batchSize) {
                    val chunkEnd = minOf(chunkStart + batchSize - 1, 254)
                    val deferredList = (chunkStart..chunkEnd).map { hostNum ->
                        async {
                            val ip = "$subnetPrefix$hostNum"
                            for (port in targetPorts) {
                                if (testPortOpen(ip, port, timeoutMs = 250)) {
                                    val hostname = try {
                                        InetAddress.getByName(ip).canonicalHostName
                                    } catch (_: Exception) {
                                        ip
                                    }
                                    val mfr = detectManufacturerFromHostname(hostname)
                                    val model = if (hostname != ip) hostname else "Imprimante Wi-Fi ($ip)"
                                    val driver = driverRepository.autoDetectDriver(mfr, model)

                                    val printer = DiscoveredPrinter(
                                        id = "discovered_${ip}_$port",
                                        name = "$mfr $model",
                                        manufacturer = mfr,
                                        model = model,
                                        connectionType = PrinterConnectionType.WIFI,
                                        address = "$ip:$port",
                                        ipAddress = ip,
                                        port = port,
                                        matchedDriver = driver,
                                        isDriverInstalled = true,
                                        statusText = "En ligne"
                                    )
                                    synchronized(results) {
                                        results.add(printer)
                                    }
                                    withContext(Dispatchers.Main) {
                                        onFound(printer)
                                    }
                                    break
                                }
                            }
                        }
                    }
                    deferredList.awaitAll()
                }
            }
            results
        }

    /**
     * Tests if a specific IP & port is responding
     */
    fun testPortOpen(ip: String, port: Int, timeoutMs: Int = 1000): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Streams raw print data over TCP socket to network printer
     */
    suspend fun sendDataViaSocket(
        ipAddress: String,
        port: Int,
        data: ByteArray,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.soTimeout = 15000
                socket.connect(InetSocketAddress(ipAddress, port), 6000)

                val os: OutputStream = socket.getOutputStream()
                val chunkSize = 4096
                var offset = 0
                val total = data.size

                while (offset < total) {
                    val len = minOf(chunkSize, total - offset)
                    os.write(data, offset, len)
                    offset += len
                    onProgress(offset.toFloat() / total.toFloat())
                }
                os.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Échec de connexion à $ipAddress:$port - ${e.localizedMessage ?: "Délai d'attente dépassé"}"))
        }
    }

    private fun getSubnetPrefix(): String? {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        val ipInt = wm.connectionInfo?.ipAddress ?: 0
        if (ipInt == 0) return null
        val byte0 = ipInt and 0xFF
        val byte1 = (ipInt shr 8) and 0xFF
        val byte2 = (ipInt shr 16) and 0xFF
        return "$byte0.$byte1.$byte2."
    }

    private fun detectManufacturerFromHostname(hostname: String): String {
        val lower = hostname.lowercase()
        return when {
            lower.contains("epson") -> "Epson"
            lower.contains("hp") || lower.contains("laserjet") || lower.contains("deskjet") -> "HP"
            lower.contains("canon") -> "Canon"
            lower.contains("brother") -> "Brother"
            lower.contains("xprinter") -> "Xprinter"
            lower.contains("star") -> "Star"
            lower.contains("zebra") -> "Zebra"
            lower.contains("samsung") -> "Samsung"
            lower.contains("ricoh") -> "Ricoh"
            lower.contains("kyocera") -> "Kyocera"
            else -> "Imprimante Réseau"
        }
    }
}
