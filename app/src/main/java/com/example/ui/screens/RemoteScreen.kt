package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvRemoteKey
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate950
import com.example.util.LocalAppStrings
import com.example.util.LocalLiteMode

@Composable
fun RemoteScreen(
    onKeyPress: (TvRemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isLiteMode = LocalLiteMode.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Shortcut Row: Assistant, Settings, Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteRoundButton(
                icon = Icons.Default.Mic,
                label = strings.assistant,
                tint = CyanAccent,
                tag = "btn_assistant",
                onClick = { onKeyPress(TvRemoteKey.GOOGLE_ASSISTANT) }
            )
            RemoteRoundButton(
                icon = Icons.Default.Tv,
                label = strings.inputSource,
                tint = IndigoAccent,
                tag = "btn_input_source",
                onClick = { onKeyPress(TvRemoteKey.MENU) }
            )
            RemoteRoundButton(
                icon = Icons.Default.Settings,
                label = strings.settings,
                tint = Slate400,
                tag = "btn_settings",
                onClick = { onKeyPress(TvRemoteKey.SETTINGS) }
            )
        }

        // Circular D-Pad Controller (Shadow disabled in lite mode to prevent GPU stalls)
        Box(
            modifier = Modifier
                .size(240.dp)
                .then(if (!isLiteMode) Modifier.shadow(12.dp, CircleShape) else Modifier)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Slate800, Slate950)
                    )
                )
                .border(2.dp, CyanAccent.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Up Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { onKeyPress(TvRemoteKey.UP) }
                    .testTag("dpad_up"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = strings.up,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Down Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { onKeyPress(TvRemoteKey.DOWN) }
                    .testTag("dpad_down"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = strings.down,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Left Button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { onKeyPress(TvRemoteKey.LEFT) }
                    .testTag("dpad_left"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = strings.left,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Right Button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { onKeyPress(TvRemoteKey.RIGHT) }
                    .testTag("dpad_right"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = strings.right,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Center OK Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CyanAccent, Color(0xFF0284C7))
                        )
                    )
                    .clickable { onKeyPress(TvRemoteKey.CENTER) }
                    .testTag("dpad_center_ok"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.ok,
                    fontWeight = FontWeight.Black,
                    color = Slate950,
                    fontSize = 20.sp
                )
            }
        }

        // Navigation Row: Back, Home, Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = strings.back,
                tag = "btn_nav_back",
                onClick = { onKeyPress(TvRemoteKey.BACK) }
            )
            NavButton(
                icon = Icons.Default.Home,
                label = strings.home,
                tag = "btn_nav_home",
                highlight = true,
                onClick = { onKeyPress(TvRemoteKey.HOME) }
            )
            NavButton(
                icon = Icons.Default.Menu,
                label = strings.menu,
                tag = "btn_nav_menu",
                onClick = { onKeyPress(TvRemoteKey.MENU) }
            )
        }

        // Volume & Media Controls Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate850),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Volume Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.volumeAndMedia,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { onKeyPress(TvRemoteKey.VOLUME_MUTE) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Slate800)
                                .testTag("btn_volume_mute")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                                contentDescription = strings.mute,
                                tint = Color.Red
                            )
                        }

                        IconButton(
                            onClick = { onKeyPress(TvRemoteKey.VOLUME_DOWN) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Slate800)
                                .testTag("btn_volume_down")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                                contentDescription = strings.volumeDown,
                                tint = CyanAccent
                            )
                        }

                        IconButton(
                            onClick = { onKeyPress(TvRemoteKey.VOLUME_UP) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Slate800)
                                .testTag("btn_volume_up")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = strings.volumeUp,
                                tint = CyanAccent
                            )
                        }
                    }
                }

                // Media Playback Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onKeyPress(TvRemoteKey.REWIND) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Slate800)
                            .testTag("btn_media_rewind")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = strings.rewind,
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CyanAccent)
                            .clickable { onKeyPress(TvRemoteKey.PLAY_PAUSE) }
                            .testTag("btn_media_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = strings.playPause,
                            tint = Slate950,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = { onKeyPress(TvRemoteKey.FAST_FORWARD) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Slate800)
                            .testTag("btn_media_fast_forward")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = strings.fastForward,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun RemoteRoundButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Slate850)
                .border(1.dp, Slate700, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate400,
            fontSize = 11.sp
        )
    }
}

@Composable
fun NavButton(
    icon: ImageVector,
    label: String,
    tag: String,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (highlight) IndigoAccent else Slate850)
                .border(1.dp, if (highlight) CyanAccent else Slate700, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) CyanAccent else Slate400,
            fontSize = 11.sp
        )
    }
}
