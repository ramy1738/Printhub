package com.example.printer.driver

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DriverProfile
import com.example.model.PaperSize
import com.example.model.PrinterProtocol
import org.json.JSONArray
import org.json.JSONObject

class DriverRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("noko_printer_drivers_prefs", Context.MODE_PRIVATE)

    companion object {
        // Built-in standard generic and brand drivers
        val BUILT_IN_DRIVERS = listOf(
            DriverProfile(
                id = "drv_generic_escpos_80",
                name = "Générique ESC/POS 80mm (Thermique)",
                manufacturer = "Générique",
                supportedFamily = "Imprimantes tickets de caisse 80mm (Epson, Xprinter, MUNBYN, POS-80)",
                protocol = PrinterProtocol.ESC_POS_80,
                supportedPaperSizes = listOf(PaperSize.ROLL_80MM),
                defaultPort = 9100,
                isDownloaded = true,
                isBuiltIn = true,
                version = "2.4.0",
                description = "Pilote haute compatibilité pour reçus et tickets 80mm avec moteur de tramage graphique et arabe RTL."
            ),
            DriverProfile(
                id = "drv_generic_escpos_58",
                name = "Générique ESC/POS 58mm (Thermique)",
                manufacturer = "Générique",
                supportedFamily = "Imprimantes tickets compactes 58mm (POS-58, mini imprimantes portables)",
                protocol = PrinterProtocol.ESC_POS_58,
                supportedPaperSizes = listOf(PaperSize.ROLL_58MM),
                defaultPort = 9100,
                isDownloaded = true,
                isBuiltIn = true,
                version = "2.4.0",
                description = "Pilote optimisé pour imprimantes thermiques 58mm compactes, reçus et textes arabes."
            ),
            DriverProfile(
                id = "drv_generic_pcl5",
                name = "Générique PCL 5 / PCL 5e (Laser)",
                manufacturer = "Générique",
                supportedFamily = "Imprimantes Laser noir et blanc (HP, Brother, Canon, Ricoh, Lexmark)",
                protocol = PrinterProtocol.PCL_5,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER, PaperSize.A3),
                defaultPort = 9100,
                isDownloaded = true,
                isBuiltIn = true,
                version = "3.1.2",
                description = "Pilote PCL 5 standard pour la majorité des imprimantes laser réseau ou USB."
            ),
            DriverProfile(
                id = "drv_generic_pcl6",
                name = "Générique PCL 6 / PCL-XL (Laser rapide)",
                manufacturer = "Générique",
                supportedFamily = "Imprimantes Laser modernes et multifonctions couleur/N&B",
                protocol = PrinterProtocol.PCL_6,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER, PaperSize.A3),
                defaultPort = 9100,
                isDownloaded = true,
                isBuiltIn = true,
                version = "3.0.0",
                description = "Pilote PCL-XL à haut débit binaire pour impressions laser rapides."
            ),
            DriverProfile(
                id = "drv_generic_pcl3",
                name = "Générique PCL 3 GUI (Jet d'encre)",
                manufacturer = "HP / Générique",
                supportedFamily = "Imprimantes jet d'encre (HP DeskJet, OfficeJet, Photosmart)",
                protocol = PrinterProtocol.PCL_3,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER, PaperSize.PHOTO_4X6),
                defaultPort = 9100,
                isDownloaded = true,
                isBuiltIn = true,
                version = "2.1.0",
                description = "Pilote graphique pour imprimantes jet d'encre HP et compatibles."
            ),
            DriverProfile(
                id = "drv_epson_tm_t20",
                name = "Epson TM-T Series (TM-T20, TM-T88)",
                manufacturer = "Epson",
                supportedFamily = "Epson TM-T20II, TM-T20III, TM-T88IV, TM-T88V, TM-m30",
                protocol = PrinterProtocol.ESC_POS_80,
                supportedPaperSizes = listOf(PaperSize.ROLL_80MM),
                defaultPort = 9100,
                isDownloaded = true,
                isBuiltIn = true,
                version = "1.8.5",
                description = "Pilote certifié Epson ESC/POS avec découpe automatique et tiroir caisse."
            ),
            DriverProfile(
                id = "drv_hp_laserjet_pro",
                name = "HP LaserJet Series (PCL 5/6)",
                manufacturer = "HP",
                supportedFamily = "HP LaserJet Pro P1102, M102, M130, M402, 1020, 1018",
                protocol = PrinterProtocol.PCL_5,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER),
                defaultPort = 9100,
                isDownloaded = true,
                isBuiltIn = true,
                version = "4.2.0",
                description = "Pilote haute performance pour imprimantes de bureau HP LaserJet."
            ),
            DriverProfile(
                id = "drv_brother_hl_series",
                name = "Brother HL & MFC Laser Series",
                manufacturer = "Brother",
                supportedFamily = "Brother HL-L2300, HL-L2350, HL-1110, MFC-L2700, DCP-L2520",
                protocol = PrinterProtocol.PCL_6,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER),
                defaultPort = 9100,
                isDownloaded = true,
                isBuiltIn = true,
                version = "2.5.0",
                description = "Pilote PCL et émulation Brother pour imprimantes monochromes et couleur."
            ),
            DriverProfile(
                id = "drv_canon_imageclass",
                name = "Canon imageCLASS & LBP Series",
                manufacturer = "Canon",
                supportedFamily = "Canon LBP2900, LBP6030, MF3010, i-SENSYS LBP6230",
                protocol = PrinterProtocol.PCL_5,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER),
                defaultPort = 9100,
                isDownloaded = true,
                isBuiltIn = true,
                version = "2.0.1",
                description = "Pilote PCL Canon pour imprimantes de bureau et laser personnelles."
            ),
            DriverProfile(
                id = "drv_xprinter_xp",
                name = "Xprinter XP Series (58/80mm)",
                manufacturer = "Xprinter",
                supportedFamily = "XP-58, XP-80, XP-N160II, XP-Q800, XP-365B",
                protocol = PrinterProtocol.ESC_POS_80,
                supportedPaperSizes = listOf(PaperSize.ROLL_80MM, PaperSize.ROLL_58MM),
                defaultPort = 9100,
                isDownloaded = true,
                isBuiltIn = true,
                version = "2.2.0",
                description = "Pilote thermique Xprinter avec support découpeur automatique et USB bulk."
            ),
            DriverProfile(
                id = "drv_pwg_raster",
                name = "PWG-Raster / AirPrint Direct",
                manufacturer = "Générique Mopria",
                supportedFamily = "Imprimantes modernes Wi-Fi certifiées Mopria / AirPrint",
                protocol = PrinterProtocol.PWG_RASTER,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER, PaperSize.PHOTO_4X6),
                defaultPort = 9100,
                isDownloaded = true,
                isBuiltIn = true,
                version = "1.0.0",
                description = "Pilote standard PWG-Raster pour imprimantes réseau modernes."
            ),
            DriverProfile(
                id = "drv_android_system",
                name = "Service d'impression Android (Universel)",
                manufacturer = "Android",
                supportedFamily = "Toutes les imprimantes reconnues par les services d'impression du système",
                protocol = PrinterProtocol.ANDROID_PRINT_SERVICE,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER, PaperSize.A3, PaperSize.PHOTO_4X6),
                defaultPort = 0,
                isDownloaded = true,
                isBuiltIn = true,
                version = "Système",
                description = "Passerelle directe vers le gestionnaire d'impression Android pour compatibilité universelle."
            )
        )

        // Online repository catalog (downloadable driver packages available online)
        val ONLINE_CATALOG = listOf(
            DriverProfile(
                id = "drv_online_pantum_p2500",
                name = "Pantum P2500 / M6500 Series",
                manufacturer = "Pantum",
                supportedFamily = "Pantum P2200, P2500W, M6500, M6550, M6600",
                protocol = PrinterProtocol.PCL_6,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER),
                defaultPort = 9100,
                isDownloaded = false,
                isBuiltIn = false,
                version = "1.4.0",
                description = "Pilote officiel pour imprimantes laser compactes Pantum."
            ),
            DriverProfile(
                id = "drv_online_kyocera_ecosys",
                name = "Kyocera ECOSYS Series (PCL 6)",
                manufacturer = "Kyocera",
                supportedFamily = "Kyocera FS-1040, FS-1060DN, ECOSYS P2040dn, M2040dn",
                protocol = PrinterProtocol.PCL_6,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER, PaperSize.A3),
                defaultPort = 9100,
                isDownloaded = false,
                isBuiltIn = false,
                version = "2.1.0",
                description = "Pilote haute performance pour multifonctions professionnels Kyocera."
            ),
            DriverProfile(
                id = "drv_online_bixolon_srp",
                name = "Bixolon SRP Series (SRP-350 / SRP-330)",
                manufacturer = "Bixolon",
                supportedFamily = "Bixolon SRP-350II, SRP-350plusIII, SRP-330II",
                protocol = PrinterProtocol.ESC_POS_80,
                supportedPaperSizes = listOf(PaperSize.ROLL_80MM),
                defaultPort = 9100,
                isDownloaded = false,
                isBuiltIn = false,
                version = "3.0.1",
                description = "Pilote d'impression thermique pour caisse enregistreuse et tickets."
            ),
            DriverProfile(
                id = "drv_online_star_tsp",
                name = "Star Micronics TSP100 / TSP650",
                manufacturer = "Star Micronics",
                supportedFamily = "TSP100III, TSP143, TSP654II, mC-Print3",
                protocol = PrinterProtocol.ESC_POS_80,
                supportedPaperSizes = listOf(PaperSize.ROLL_80MM),
                defaultPort = 9100,
                isDownloaded = false,
                isBuiltIn = false,
                version = "2.7.0",
                description = "Pilote thermique Star Micronics en mode émulation ESC/POS."
            ),
            DriverProfile(
                id = "drv_online_samsung_xpress",
                name = "Samsung Xpress Series (SPL / PCL 6)",
                manufacturer = "Samsung",
                supportedFamily = "Samsung Xpress M2020W, M2070, SL-M2835, ML-2160",
                protocol = PrinterProtocol.PCL_6,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER),
                defaultPort = 9100,
                isDownloaded = false,
                isBuiltIn = false,
                version = "2.3.0",
                description = "Pilote PCL / SPL pour imprimantes laser Samsung."
            ),
            DriverProfile(
                id = "drv_online_ricoh_sp",
                name = "Ricoh Aficio & SP Series",
                manufacturer = "Ricoh",
                supportedFamily = "Ricoh SP 112, SP 211, SP 311DN, Aficio MP 2000",
                protocol = PrinterProtocol.PCL_5,
                supportedPaperSizes = listOf(PaperSize.A4, PaperSize.LETTER, PaperSize.A3),
                defaultPort = 9100,
                isDownloaded = false,
                isBuiltIn = false,
                version = "1.9.0",
                description = "Pilote d'impression bureautique pour imprimantes Ricoh."
            ),
            DriverProfile(
                id = "drv_online_zebra_zd",
                name = "Zebra ZD / ZP Thermal Receipt",
                manufacturer = "Zebra",
                supportedFamily = "Zebra ZD220, ZD420, ZP450, GK420d",
                protocol = PrinterProtocol.ESC_POS_80,
                supportedPaperSizes = listOf(PaperSize.ROLL_80MM),
                defaultPort = 9100,
                isDownloaded = false,
                isBuiltIn = false,
                version = "2.0.0",
                description = "Pilote thermique pour reçus de vente et bordereaux Zebra."
            )
        )
    }

    /**
     * Gets all currently available drivers (built-in + user-downloaded offline drivers)
     */
    fun getInstalledDrivers(): List<DriverProfile> {
        val downloaded = loadDownloadedDrivers()
        val all = mutableListOf<DriverProfile>()
        all.addAll(BUILT_IN_DRIVERS)
        for (d in downloaded) {
            if (all.none { it.id == d.id }) {
                all.add(d)
            }
        }
        return all
    }

    /**
     * Search the online repository for additional manufacturer drivers
     */
    fun searchOnlineRepository(query: String = ""): List<DriverProfile> {
        val installedIds = getInstalledDrivers().map { it.id }.toSet()
        val results = ONLINE_CATALOG.filter { profile ->
            if (installedIds.contains(profile.id)) return@filter false
            if (query.isBlank()) true
            else {
                profile.name.contains(query, ignoreCase = true) ||
                        profile.manufacturer.contains(query, ignoreCase = true) ||
                        profile.supportedFamily.contains(query, ignoreCase = true)
            }
        }
        return results
    }

    /**
     * Downloads and installs a driver profile from the repository into local storage
     */
    fun installDriver(driver: DriverProfile): Boolean {
        val current = loadDownloadedDrivers().toMutableList()
        current.removeAll { it.id == driver.id }
        current.add(driver.copy(isDownloaded = true))
        saveDownloadedDrivers(current)
        return true
    }

    /**
     * Removes an offline downloaded driver
     */
    fun uninstallDriver(driverId: String): Boolean {
        val current = loadDownloadedDrivers().toMutableList()
        current.removeAll { it.id == driverId }
        saveDownloadedDrivers(current)
        return true
    }

    /**
     * Auto-detect matching driver based on USB vendor/product or network model text
     */
    fun autoDetectDriver(
        manufacturer: String,
        model: String,
        vendorId: Int? = null,
        productId: Int? = null
    ): DriverProfile {
        val all = getInstalledDrivers()
        val lowerMfr = manufacturer.lowercase()
        val lowerModel = model.lowercase()

        // 1. Check known USB Vendor IDs
        if (vendorId != null) {
            when (vendorId) {
                0x04b8 -> { // Epson
                    if (lowerModel.contains("pos") || lowerModel.contains("tm-") || lowerModel.contains("ticket")) {
                        return all.find { it.id == "drv_epson_tm_t20" } ?: all.first { it.id == "drv_generic_escpos_80" }
                    }
                }
                0x03f0 -> { // HP
                    if (lowerModel.contains("laser") || lowerModel.contains("p1102") || lowerModel.contains("m102")) {
                        return all.find { it.id == "drv_hp_laserjet_pro" } ?: all.first { it.id == "drv_generic_pcl5" }
                    } else if (lowerModel.contains("deskjet") || lowerModel.contains("ink") || lowerModel.contains("photosmart")) {
                        return all.find { it.id == "drv_generic_pcl3" } ?: all.first { it.id == "drv_generic_pcl5" }
                    }
                }
                0x04f9 -> { // Brother
                    return all.find { it.id == "drv_brother_hl_series" } ?: all.first { it.id == "drv_generic_pcl6" }
                }
                0x04a9 -> { // Canon
                    return all.find { it.id == "drv_canon_imageclass" } ?: all.first { it.id == "drv_generic_pcl5" }
                }
                0x0416, 0x1fc9, 0x0fe6 -> { // Xprinter / POS
                    return all.find { it.id == "drv_xprinter_xp" } ?: all.first { it.id == "drv_generic_escpos_80" }
                }
            }
        }

        // 2. Check model/manufacturer string patterns
        if (lowerModel.contains("tm-t") || lowerModel.contains("tm-m")) {
            return all.find { it.id == "drv_epson_tm_t20" } ?: all.first { it.id == "drv_generic_escpos_80" }
        }
        if (lowerModel.contains("laserjet") || lowerModel.contains("laser jet")) {
            return all.find { it.id == "drv_hp_laserjet_pro" } ?: all.first { it.id == "drv_generic_pcl5" }
        }
        if (lowerMfr.contains("brother") || lowerModel.contains("hl-") || lowerModel.contains("dcp-")) {
            return all.find { it.id == "drv_brother_hl_series" } ?: all.first { it.id == "drv_generic_pcl6" }
        }
        if (lowerMfr.contains("canon") || lowerModel.contains("lbp") || lowerModel.contains("imageclass")) {
            return all.find { it.id == "drv_canon_imageclass" } ?: all.first { it.id == "drv_generic_pcl5" }
        }
        if (lowerMfr.contains("xprinter") || lowerModel.contains("xp-")) {
            return all.find { it.id == "drv_xprinter_xp" } ?: all.first { it.id == "drv_generic_escpos_80" }
        }

        // 3. Check keywords for thermal receipt vs laser vs inkjet
        if (lowerModel.contains("58") || lowerModel.contains("pos58") || lowerModel.contains("pos-58")) {
            return all.first { it.id == "drv_generic_escpos_58" }
        }
        if (lowerModel.contains("pos") || lowerModel.contains("thermal") || lowerModel.contains("receipt") || lowerModel.contains("ticket") || lowerModel.contains("80mm")) {
            return all.first { it.id == "drv_generic_escpos_80" }
        }
        if (lowerModel.contains("laser") || lowerModel.contains("laserjet") || lowerModel.contains("mono")) {
            return all.first { it.id == "drv_generic_pcl5" }
        }
        if (lowerModel.contains("deskjet") || lowerModel.contains("inkjet") || lowerModel.contains("tank") || lowerModel.contains("color")) {
            return all.first { it.id == "drv_generic_pcl3" }
        }

        // 4. Default generic fallback
        return all.first { it.id == "drv_generic_escpos_80" }
    }

    private fun loadDownloadedDrivers(): List<DriverProfile> {
        val jsonStr = prefs.getString("custom_downloaded_drivers", null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<DriverProfile>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val protocol = try {
                    PrinterProtocol.valueOf(obj.getString("protocol"))
                } catch (e: Exception) {
                    PrinterProtocol.ESC_POS_80
                }
                list.add(
                    DriverProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        manufacturer = obj.getString("manufacturer"),
                        supportedFamily = obj.getString("supportedFamily"),
                        protocol = protocol,
                        supportedPaperSizes = if (protocol.isThermal) listOf(PaperSize.ROLL_80MM) else listOf(PaperSize.A4, PaperSize.LETTER),
                        defaultPort = obj.optInt("defaultPort", 9100),
                        isDownloaded = true,
                        isBuiltIn = false,
                        version = obj.optString("version", "1.0"),
                        description = obj.optString("description", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveDownloadedDrivers(list: List<DriverProfile>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("manufacturer", item.manufacturer)
            obj.put("supportedFamily", item.supportedFamily)
            obj.put("protocol", item.protocol.name)
            obj.put("defaultPort", item.defaultPort)
            obj.put("version", item.version)
            obj.put("description", item.description)
            array.put(obj)
        }
        prefs.edit().putString("custom_downloaded_drivers", array.toString()).apply()
    }
}
