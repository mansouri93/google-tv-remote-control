package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvDevice
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePickerSheet(
    devices: List<TvDevice>,
    isScanning: Boolean,
    scanProgress: Float,
    currentDevice: TvDevice?,
    onScan: () -> Unit,
    onSelectDevice: (TvDevice) -> Unit,
    onAddManualDevice: (String, String, Int) -> Unit,
    onDismiss: () -> Unit,
    onShowHelp: () -> Unit,
    onDiagnose: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strings = LocalAppStrings.current

    var showManualDialog by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualIp by remember { mutableStateOf("192.168.1.") }
    var manualPort by remember { mutableStateOf("5555") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Slate900,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Slate700)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sheet Header with flexible layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = strings.detectedDevices,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = strings.deviceSheetSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onScan,
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate800,
                        contentColor = CyanAccent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("dialog_btn_scan")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.scanAgain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            if (isScanning) {
                LinearProgressIndicator(
                    progress = { scanProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = CyanAccent,
                    trackColor = Slate800
                )
            }

            // Quick Actions: Manual IP & Help Guide
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showManualDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dialog_btn_manual_ip"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.manualIpButton, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onShowHelp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dialog_btn_help_guide"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.setupGuideButton, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Devices list or Empty State
            if (devices.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate850)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TvOff,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (isScanning) strings.scanningWifi else strings.noDevicesFound,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        if (!isScanning) {
                            Button(
                                onClick = { showManualDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950)
                            ) {
                                Text(strings.manualIpButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices, key = { it.ipAddress }) { dev ->
                        val isCurrent = dev.ipAddress == currentDevice?.ipAddress
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectDevice(dev) }
                                .testTag("device_item_${dev.ipAddress}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Slate800 else Slate850
                            ),
                            border = if (isCurrent)
                                CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CyanAccent, IndigoAccent)))
                            else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) CyanAccent.copy(alpha = 0.2f) else Slate800),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = if (isCurrent) CyanAccent else Slate400,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dev.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${dev.model} • ${dev.ipAddress}:${dev.port}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        // Status badge
                                        if (dev.isAdbOpen) {
                                            Text(
                                                text = "✓ پورت ADB فعال و آماده",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldAccent,
                                                fontSize = 10.sp
                                            )
                                        } else if (dev.isCastOpen) {
                                            Text(
                                                text = "⚠ کست آنلاین (اشکال‌زدایی ADB خاموش)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AmberAccent,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Diagnostic Button
                                    IconButton(
                                        onClick = { onDiagnose(dev.ipAddress) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = "عیب‌یابی",
                                            tint = Slate400,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = strings.connectedTo,
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Button(
                                            onClick = { onSelectDevice(dev) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950)
                                        ) {
                                            Text(
                                                text = strings.directConnect,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Manual IP Entry Dialog
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text(strings.manualDialogTitle, color = Color.White) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text(strings.manualNameLabel) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Slate700
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        label = { Text(strings.manualIpLabel) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Slate700
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualPort,
                        onValueChange = { manualPort = it },
                        label = { Text(strings.manualPortLabel) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Slate700
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val port = manualPort.toIntOrNull() ?: 5555
                        if (manualIp.isNotBlank()) {
                            onAddManualDevice(manualName, manualIp.trim(), port)
                            showManualDialog = false
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950)
                ) {
                    Text(strings.directConnect)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text(strings.cancel, color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun TvSetupHelpDialog(onDismiss: () -> Unit) {
    val strings = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.helpDialogTitle, color = Color.White)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = strings.helpDialogDescription,
                    color = Slate400,
                    fontSize = 13.sp
                )

                SetupStepItem("1", strings.helpStep1)
                SetupStepItem("2", strings.helpStep2)
                SetupStepItem("3", strings.helpStep3)
                SetupStepItem("4", strings.helpStep4)
                SetupStepItem("5", strings.helpStep5)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950)
            ) {
                Text(strings.helpDismissButton, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Slate900
    )
}

@Composable
fun SetupStepItem(number: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(CyanAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = description,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
