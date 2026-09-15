package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvDevice
import com.example.model.TvScreenState
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.util.AppLanguage
import com.example.util.LocalAppStrings

@Composable
fun TvHeader(
    device: TvDevice?,
    isConnected: Boolean,
    screenState: TvScreenState,
    showMonitor: Boolean,
    language: AppLanguage,
    isLiteMode: Boolean,
    onToggleMonitor: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleLiteMode: () -> Unit,
    onOpenDeviceSheet: () -> Unit,
    onOpenHelpDialog: () -> Unit,
    onPowerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Slate900)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Top row: App title & Connection status chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TV Device info button (Clickable to switch TV)
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Slate800)
                    .border(1.dp, if (isConnected) CyanAccent.copy(alpha = 0.5f) else Slate700, RoundedCornerShape(20.dp))
                    .clickable { onOpenDeviceSheet() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("device_selector_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) EmeraldAccent else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = device?.name ?: strings.selectTv,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isConnected) "${device?.ipAddress ?: ""} • ${device?.latencyMs ?: 0}ms" else strings.tapToConnectWifi,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "WiFi",
                    tint = if (isConnected) CyanAccent else Slate400,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Quick actions on header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Language Toggle Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate800)
                        .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { onToggleLanguage() }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("btn_language_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.FA) "EN" else "فا",
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Lite Mode Optimization Toggle (Low-end device helper)
                IconButton(
                    onClick = onToggleLiteMode,
                    modifier = Modifier.size(36.dp).testTag("btn_lite_mode_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = if (isLiteMode) strings.liteModeOn else strings.liteModeOff,
                        tint = if (isLiteMode) EmeraldAccent else Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Monitor Toggle
                IconButton(
                    onClick = onToggleMonitor,
                    modifier = Modifier.size(36.dp).testTag("monitor_toggle_button")
                ) {
                    Icon(
                        imageVector = if (showMonitor) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = strings.toggleTvMonitor,
                        tint = if (showMonitor) CyanAccent else Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Developer Help Guide
                IconButton(
                    onClick = onOpenHelpDialog,
                    modifier = Modifier.size(36.dp).testTag("help_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = strings.connectionGuide,
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Power button
                IconButton(
                    onClick = onPowerClick,
                    modifier = Modifier.size(36.dp).testTag("header_power_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = strings.powerButton,
                        tint = if (screenState.powerOn) Color(0xFFEF4444) else Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Live TV Preview Monitor (Collapsible)
        AnimatedVisibility(
            visible = showMonitor,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("tv_monitor_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate850),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = listOf(CyanAccent.copy(alpha = 0.6f), Color(0xFF6366F1).copy(alpha = 0.4f))
                    )
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // TV screen mockup container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2.2f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (screenState.powerOn)
                                        listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                                    else
                                        listOf(Color.Black, Color(0xFF0B0F19))
                                )
                            )
                            .border(1.dp, Slate700, RoundedCornerShape(8.dp))
                    ) {
                        if (screenState.powerOn) {
                            // Active App title & status bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = screenState.currentActiveApp,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (screenState.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = if (screenState.isMuted) Color.Red else Slate400,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (screenState.isMuted) "Mute" else "${screenState.volumeLevel}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Center simulated content badge
                            Box(
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = screenState.currentActiveApp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Google TV 4K HDR",
                                        color = CyanAccent.copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Simulated Mouse Cursor Arrow on TV Screen
                            val normX = (screenState.cursorX / screenState.screenWidth).coerceIn(0.05f, 0.95f)
                            val normY = (screenState.cursorY / screenState.screenHeight).coerceIn(0.05f, 0.95f)

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = (normX * 260).dp, top = (normY * 80).dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(CyanAccent)
                                        .border(2.dp, Color.White, CircleShape)
                                )
                            }
                        } else {
                            Box(modifier = Modifier.align(Alignment.Center)) {
                                Text(
                                    text = if (language == AppLanguage.FA) "تلویزیون در حالت آماده‌باش (Standby)" else "TV in Standby Mode",
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Bottom info line: last command ticker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.FA) "آخرین عملیات: ${screenState.lastAction}" else "Last action: ${screenState.lastAction}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (language == AppLanguage.FA) "ماوس: (${screenState.cursorX.toInt()}, ${screenState.cursorY.toInt()})" else "Mouse: (${screenState.cursorX.toInt()}, ${screenState.cursorY.toInt()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
