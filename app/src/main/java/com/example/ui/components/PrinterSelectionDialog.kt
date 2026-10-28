package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.AppLanguage
import com.example.model.DiscoveredPrinter
import com.example.model.DriverProfile
import com.example.model.PrinterConnectionType
import com.example.model.PrinterProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSelectionDialog(
    printers: List<DiscoveredPrinter>,
    selectedPrinter: DiscoveredPrinter?,
    selectedDriver: DriverProfile,
    isScanning: Boolean,
    language: AppLanguage,
    onSelectPrinter: (DiscoveredPrinter) -> Unit,
    onSelectDriver: (DriverProfile) -> Unit,
    onRefresh: () -> Unit,
    onStartNetworkScan: () -> Unit,
    onAddManualPrinter: (mfr: String, model: String, ip: String, port: Int) -> Unit,
    onRemoveSavedPrinter: (DiscoveredPrinter) -> Unit,
    onRequestUsbPermission: (DiscoveredPrinter) -> Unit,
    onOpenDriverManager: () -> Unit,
    onDismiss: () -> Unit
) {
    var showManualAddDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_printer_selection")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Imprimantes & Pilotes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "USB-OTG, Wi-Fi & Réseau local",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar: Scan Wi-Fi & Add Manual & Driver Manager
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onStartNetworkScan,
                        enabled = !isScanning,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recherche...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanner Wi-Fi", fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showManualAddDialog = true },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manuel IP", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Driver Repository button
                OutlinedButton(
                    onClick = onOpenDriverManager,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pilotes : ${selectedDriver.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // List of detected printers
                Text(
                    text = "Imprimantes disponibles (${printers.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(printers) { printer ->
                        val isSelected = selectedPrinter?.id == printer.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPrinter(printer) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (isSelected)
                                androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Connection Type Icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (printer.connectionType) {
                                                PrinterConnectionType.USB -> Color(0xFF00796B).copy(alpha = 0.15f)
                                                PrinterConnectionType.WIFI -> Color(0xFF1976D2).copy(alpha = 0.15f)
                                                PrinterConnectionType.SYSTEM -> Color(0xFF5E35B1).copy(alpha = 0.15f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (printer.connectionType) {
                                            PrinterConnectionType.USB -> Icons.Default.Usb
                                            PrinterConnectionType.WIFI -> Icons.Default.Wifi
                                            PrinterConnectionType.SYSTEM -> Icons.Default.Devices
                                        },
                                        contentDescription = null,
                                        tint = when (printer.connectionType) {
                                            PrinterConnectionType.USB -> Color(0xFF00796B)
                                            PrinterConnectionType.WIFI -> Color(0xFF1976D2)
                                            PrinterConnectionType.SYSTEM -> Color(0xFF5E35B1)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = printer.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${printer.connectionType.label} • ${printer.address}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    val driverName = printer.matchedDriver?.name ?: "Pilote générique"
                                    Text(
                                        text = "Pilote : $driverName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (printer.connectionType == PrinterConnectionType.USB && !printer.hasUsbPermission) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { onRequestUsbPermission(printer) },
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Autoriser l'accès USB", fontSize = 10.sp)
                                        }
                                    }
                                }

                                if (printer.connectionType == PrinterConnectionType.WIFI && printer.id.startsWith("net_")) {
                                    IconButton(
                                        onClick = { onRemoveSavedPrinter(printer) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Supprimer",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectPrinter(printer) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirmer la sélection", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showManualAddDialog) {
        ManualPrinterAddDialog(
            onDismiss = { showManualAddDialog = false },
            onAdd = { mfr, model, ip, port ->
                onAddManualPrinter(mfr, model, ip, port)
                showManualAddDialog = false
            }
        )
    }
}

@Composable
fun ManualPrinterAddDialog(
    onDismiss: () -> Unit,
    onAdd: (mfr: String, model: String, ip: String, port: Int) -> Unit
) {
    var manufacturer by remember { mutableStateOf("Epson") }
    var modelName by remember { mutableStateOf("TM-T20") }
    var ipAddress by remember { mutableStateOf("192.168.1.100") }
    var portText by remember { mutableStateOf("9100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une imprimante réseau") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("Adresse IP (ex: 192.168.1.100)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port RAW (Défaut : 9100)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Marque (ex: Epson, HP, Canon, Xprinter)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Modèle (ex: TM-T20, LaserJet, XP-80)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: 9100
                    if (ipAddress.isNotBlank()) {
                        onAdd(manufacturer.trim(), modelName.trim(), ipAddress.trim(), port)
                    }
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
