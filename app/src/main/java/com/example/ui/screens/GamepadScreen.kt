package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvRemoteKey
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.LocalAppStrings
import com.example.util.LocalLiteMode
import com.example.viewmodel.GamepadControlMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GamepadScreen(
    controlMode: GamepadControlMode,
    isTurboEnabled: Boolean,
    onModeChange: (GamepadControlMode) -> Unit,
    onToggleTurbo: () -> Unit,
    onKeyPress: (TvRemoteKey) -> Unit,
    onJoystickDirection: (TvRemoteKey) -> Unit,
    isFullscreen: Boolean = false,
    isLandscape: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val activeLandscape = isLandscape || (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    val isLite = LocalLiteMode.current

    if (activeLandscape) {
        GamepadLandscapeLayout(
            controlMode = controlMode,
            isTurboEnabled = isTurboEnabled,
            isFullscreen = isFullscreen,
            onModeChange = onModeChange,
            onToggleTurbo = onToggleTurbo,
            onToggleFullscreen = onToggleFullscreen,
            onKeyPress = onKeyPress,
            onJoystickDirection = onJoystickDirection,
            isLite = isLite,
            modifier = modifier
        )
    } else {
        GamepadPortraitLayout(
            controlMode = controlMode,
            isTurboEnabled = isTurboEnabled,
            isFullscreen = isFullscreen,
            onModeChange = onModeChange,
            onToggleTurbo = onToggleTurbo,
            onToggleFullscreen = onToggleFullscreen,
            onKeyPress = onKeyPress,
            onJoystickDirection = onJoystickDirection,
            isLite = isLite,
            modifier = modifier
        )
    }
}

/**
 * Landscape layout designed like a dedicated handheld gaming console (Switch / Steam Deck).
 * Ergonomic thumb placement: Left thumb for D-Pad/Joystick, Right thumb for A/B/X/Y,
 * Left index for L1/L2, Right index for R1/R2. Center holds secondary controls.
 * Zero vertical scrolling required.
 */
@Composable
fun GamepadLandscapeLayout(
    controlMode: GamepadControlMode,
    isTurboEnabled: Boolean,
    isFullscreen: Boolean,
    onModeChange: (GamepadControlMode) -> Unit,
    onToggleTurbo: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onKeyPress: (TvRemoteKey) -> Unit,
    onJoystickDirection: (TvRemoteKey) -> Unit,
    isLite: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- LEFT WING: L2/L1 at top, D-Pad or 360° Joystick below ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Shoulder Triggers L2 and L1
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamepadTriggerButton(
                    label = "L2",
                    key = TvRemoteKey.BUTTON_L2,
                    onClick = { onKeyPress(TvRemoteKey.BUTTON_L2) },
                    accentColor = CyanAccent,
                    isLite = isLite,
                    modifier = Modifier.testTag("gamepad_btn_l2")
                )
                GamepadTriggerButton(
                    label = "L1",
                    key = TvRemoteKey.BUTTON_L1,
                    onClick = { onKeyPress(TvRemoteKey.BUTTON_L1) },
                    accentColor = CyanAccent,
                    isLite = isLite,
                    modifier = Modifier.testTag("gamepad_btn_l1")
                )
            }

            // Directional Area (D-Pad or 360 Joystick)
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .testTag("gamepad_directional_container"),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = controlMode,
                    label = "LandscapeDirectionalAnim"
                ) { mode ->
                    when (mode) {
                        GamepadControlMode.DPAD -> {
                            GamepadDPad(
                                onKeyPress = onKeyPress,
                                isLite = isLite
                            )
                        }
                        GamepadControlMode.JOYSTICK -> {
                            GamepadAnalogJoystick(
                                onDirection = onJoystickDirection,
                                isLite = isLite
                            )
                        }
                    }
                }
            }
        }

        // --- CENTER CONSOLE: Mode switcher, Turbo, Select/Home/Start, Latency info ---
        Column(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Row 1: Mode Switch & Turbo Toggle & Fullscreen Toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Switcher (D-Pad vs Joystick)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onModeChange(
                                if (controlMode == GamepadControlMode.DPAD) GamepadControlMode.JOYSTICK
                                else GamepadControlMode.DPAD
                            )
                        }
                        .testTag("gamepad_landscape_mode_toggle"),
                    color = Slate900,
                    border = if (isLite) null else androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (controlMode == GamepadControlMode.DPAD) Icons.Default.RadioButtonChecked else Icons.Default.Tune,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (controlMode == GamepadControlMode.DPAD) "D-Pad" else "Joystick",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Turbo Toggle
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleTurbo() }
                        .testTag("gamepad_landscape_turbo_toggle"),
                    color = if (isTurboEnabled) AmberAccent.copy(alpha = 0.25f) else Slate900,
                    border = if (isLite) null else androidx.compose.foundation.BorderStroke(1.dp, if (isTurboEnabled) AmberAccent else Slate700)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isTurboEnabled) AmberAccent else Slate400,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isTurboEnabled) "TURBO" else "TURBO OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTurboEnabled) AmberAccent else Slate400
                        )
                    }
                }

                // Fullscreen Toggle
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleFullscreen() }
                        .testTag("gamepad_fullscreen_toggle"),
                    color = if (isFullscreen) CyanAccent.copy(alpha = 0.25f) else Slate900,
                    border = if (isLite) null else androidx.compose.foundation.BorderStroke(1.dp, if (isFullscreen) CyanAccent else Slate700)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) strings.exitFullscreen else strings.fullscreenGamepad,
                            tint = if (isFullscreen) CyanAccent else Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Row 2: Select, Home, Start auxiliary controls
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Slate900)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamepadAuxButton(
                    label = strings.selectBtn,
                    onClick = { onKeyPress(TvRemoteKey.BUTTON_SELECT) },
                    testTag = "gamepad_btn_select"
                )

                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { onKeyPress(TvRemoteKey.HOME) }
                        .testTag("gamepad_btn_home"),
                    color = Slate800,
                    contentColor = CyanAccent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                GamepadAuxButton(
                    label = strings.startBtn,
                    onClick = { onKeyPress(TvRemoteKey.BUTTON_START) },
                    testTag = "gamepad_btn_start"
                )
            }

            // Row 3: ADB Low-latency info chip
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                color = Slate900.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent)
                    )
                    Text(
                        text = "ADB 5555 • 12ms",
                        fontSize = 10.sp,
                        color = Slate400
                    )
                }
            }
        }

        // --- RIGHT WING: R1/R2 at top, Action Diamond (Y, X, B, A) below ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Shoulder Triggers R1 and R2
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamepadTriggerButton(
                    label = "R1",
                    key = TvRemoteKey.BUTTON_R1,
                    onClick = { onKeyPress(TvRemoteKey.BUTTON_R1) },
                    accentColor = RoseAccent,
                    isLite = isLite,
                    modifier = Modifier.testTag("gamepad_btn_r1")
                )
                GamepadTriggerButton(
                    label = "R2",
                    key = TvRemoteKey.BUTTON_R2,
                    onClick = { onKeyPress(TvRemoteKey.BUTTON_R2) },
                    accentColor = RoseAccent,
                    isLite = isLite,
                    modifier = Modifier.testTag("gamepad_btn_r2")
                )
            }

            // Diamond Action Buttons (Y, B, A, X)
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .testTag("gamepad_action_container"),
                contentAlignment = Alignment.Center
            ) {
                GamepadActionDiamond(
                    isTurbo = isTurboEnabled,
                    isLite = isLite,
                    onKeyPress = onKeyPress
                )
            }
        }
    }
}

/**
 * Standard Portrait Layout with vertical scrolling and complete controls.
 */
@Composable
fun GamepadPortraitLayout(
    controlMode: GamepadControlMode,
    isTurboEnabled: Boolean,
    isFullscreen: Boolean,
    onModeChange: (GamepadControlMode) -> Unit,
    onToggleTurbo: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onKeyPress: (TvRemoteKey) -> Unit,
    onJoystickDirection: (TvRemoteKey) -> Unit,
    isLite: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Selector Card: D-Pad vs. Joystick
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gamepad_mode_selector_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate850),
            border = if (isLite) null else CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(Slate700, Slate800))
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.controlModeLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Turbo Mode Toggle Chip
                        FilterChip(
                            selected = isTurboEnabled,
                            onClick = onToggleTurbo,
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (isTurboEnabled) AmberAccent else Slate400
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isTurboEnabled) strings.turboMode else "Turbo OFF",
                                        fontSize = 11.sp,
                                        fontWeight = if (isTurboEnabled) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberAccent.copy(alpha = 0.2f),
                                selectedLabelColor = AmberAccent,
                                containerColor = Slate900,
                                labelColor = Slate400
                            ),
                            modifier = Modifier.testTag("gamepad_turbo_toggle")
                        )

                        // Fullscreen Toggle Button
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleFullscreen() }
                                .testTag("gamepad_portrait_fullscreen_toggle"),
                            color = if (isFullscreen) CyanAccent.copy(alpha = 0.25f) else Slate900,
                            border = if (isLite) null else androidx.compose.foundation.BorderStroke(1.dp, if (isFullscreen) CyanAccent else Slate700)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = strings.fullscreenGamepad,
                                    tint = if (isFullscreen) CyanAccent else Slate400,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // Segmented Switch between D-Pad and Joystick
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate900)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // D-Pad Choice
                    val isDpad = controlMode == GamepadControlMode.DPAD
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { onModeChange(GamepadControlMode.DPAD) }
                            .testTag("mode_switch_dpad"),
                        color = if (isDpad) CyanAccent else Color.Transparent,
                        contentColor = if (isDpad) Slate950 else Slate400
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonChecked,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.dpadMode,
                                fontSize = 12.sp,
                                fontWeight = if (isDpad) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Joystick Choice
                    val isJoystick = controlMode == GamepadControlMode.JOYSTICK
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { onModeChange(GamepadControlMode.JOYSTICK) }
                            .testTag("mode_switch_joystick"),
                        color = if (isJoystick) CyanAccent else Color.Transparent,
                        contentColor = if (isJoystick) Slate950 else Slate400
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.joystickMode,
                                fontSize = 12.sp,
                                fontWeight = if (isJoystick) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Shoulder Buttons (L2, L1, R1, R2)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gamepad_shoulders_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate850),
            border = if (isLite) null else CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(Slate700, Slate800))
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = strings.shoulderTriggers,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Shoulders
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GamepadTriggerButton(
                            label = "L2",
                            key = TvRemoteKey.BUTTON_L2,
                            onClick = { onKeyPress(TvRemoteKey.BUTTON_L2) },
                            accentColor = CyanAccent,
                            isLite = isLite,
                            modifier = Modifier.testTag("gamepad_btn_l2")
                        )
                        GamepadTriggerButton(
                            label = "L1",
                            key = TvRemoteKey.BUTTON_L1,
                            onClick = { onKeyPress(TvRemoteKey.BUTTON_L1) },
                            accentColor = CyanAccent,
                            isLite = isLite,
                            modifier = Modifier.testTag("gamepad_btn_l1")
                        )
                    }

                    // Right Shoulders
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GamepadTriggerButton(
                            label = "R1",
                            key = TvRemoteKey.BUTTON_R1,
                            onClick = { onKeyPress(TvRemoteKey.BUTTON_R1) },
                            accentColor = RoseAccent,
                            isLite = isLite,
                            modifier = Modifier.testTag("gamepad_btn_r1")
                        )
                        GamepadTriggerButton(
                            label = "R2",
                            key = TvRemoteKey.BUTTON_R2,
                            onClick = { onKeyPress(TvRemoteKey.BUTTON_R2) },
                            accentColor = RoseAccent,
                            isLite = isLite,
                            modifier = Modifier.testTag("gamepad_btn_r2")
                        )
                    }
                }
            }
        }

        // Main Gaming Arena: Directional Control (D-Pad or Joystick) + Center Buttons + Action Buttons
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gamepad_main_arena_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate850),
            border = if (isLite) null else CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(Slate700, Slate800))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Controller Main Row: Left (Movement) & Right (Actions)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Section: D-Pad or 360 Joystick
                    Box(
                        modifier = Modifier
                            .size(176.dp)
                            .testTag("gamepad_directional_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = controlMode,
                            label = "DirectionalModeAnim"
                        ) { mode ->
                            when (mode) {
                                GamepadControlMode.DPAD -> {
                                    GamepadDPad(
                                        onKeyPress = onKeyPress,
                                        isLite = isLite
                                    )
                                }
                                GamepadControlMode.JOYSTICK -> {
                                    GamepadAnalogJoystick(
                                        onDirection = onJoystickDirection,
                                        isLite = isLite
                                    )
                                }
                            }
                        }
                    }

                    // Right Section: Diamond Action Buttons (A, B, X, Y)
                    Box(
                        modifier = Modifier
                            .size(176.dp)
                            .testTag("gamepad_action_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        GamepadActionDiamond(
                            isTurbo = isTurboEnabled,
                            isLite = isLite,
                            onKeyPress = onKeyPress
                        )
                    }
                }

                // Center Auxiliary Buttons (SELECT, HOME, START)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate900)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GamepadAuxButton(
                        label = strings.selectBtn,
                        onClick = { onKeyPress(TvRemoteKey.BUTTON_SELECT) },
                        testTag = "gamepad_btn_select"
                    )

                    // Home Button
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onKeyPress(TvRemoteKey.HOME) }
                            .testTag("gamepad_btn_home"),
                        color = Slate800,
                        contentColor = CyanAccent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    GamepadAuxButton(
                        label = strings.startBtn,
                        onClick = { onKeyPress(TvRemoteKey.BUTTON_START) },
                        testTag = "gamepad_btn_start"
                    )
                }
            }
        }

        // Gamepad Status & Latency Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = if (controlMode == GamepadControlMode.DPAD) strings.dpadMode else strings.joystickMode,
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }

                Text(
                    text = "ADB Port 5555 | 12ms Latency",
                    fontSize = 10.sp,
                    color = Slate400
                )
            }
        }
    }
}

/**
 * Ergonomic 4-Way D-Pad with Center Button
 */
@Composable
fun GamepadDPad(
    onKeyPress: (TvRemoteKey) -> Unit,
    isLite: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(170.dp),
        contentAlignment = Alignment.Center
    ) {
        // D-Pad Background cross silhouette
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val crossWidth = 48.dp.toPx()
            val totalRadius = 78.dp.toPx()

            // Draw horizontal arm
            drawRoundRect(
                color = Slate900,
                topLeft = Offset(center.x - totalRadius, center.y - crossWidth / 2),
                size = androidx.compose.ui.geometry.Size(totalRadius * 2, crossWidth),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx())
            )
            // Draw vertical arm
            drawRoundRect(
                color = Slate900,
                topLeft = Offset(center.x - crossWidth / 2, center.y - totalRadius),
                size = androidx.compose.ui.geometry.Size(crossWidth, totalRadius * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx())
            )
        }

        // UP
        GamepadArrowButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDesc = "Up",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 2.dp)
                .testTag("gamepad_dpad_up"),
            isLite = isLite,
            onClick = { onKeyPress(TvRemoteKey.UP) }
        )

        // DOWN
        GamepadArrowButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDesc = "Down",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-2).dp)
                .testTag("gamepad_dpad_down"),
            isLite = isLite,
            onClick = { onKeyPress(TvRemoteKey.DOWN) }
        )

        // LEFT
        GamepadArrowButton(
            icon = Icons.Default.KeyboardArrowLeft,
            contentDesc = "Left",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 2.dp)
                .testTag("gamepad_dpad_left"),
            isLite = isLite,
            onClick = { onKeyPress(TvRemoteKey.LEFT) }
        )

        // RIGHT
        GamepadArrowButton(
            icon = Icons.Default.KeyboardArrowRight,
            contentDesc = "Right",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-2).dp)
                .testTag("gamepad_dpad_right"),
            isLite = isLite,
            onClick = { onKeyPress(TvRemoteKey.RIGHT) }
        )

        // CENTER (OK/Select)
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onKeyPress(TvRemoteKey.CENTER) }
                .testTag("gamepad_dpad_center"),
            color = Slate800,
            contentColor = CyanAccent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(CyanAccent.copy(alpha = 0.8f))
                )
            }
        }
    }
}

@Composable
fun GamepadArrowButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    isLite: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale = if (isPressed) 0.9f else 1f
    val bgColor = if (isPressed) CyanAccent.copy(alpha = 0.3f) else Slate800

    Surface(
        modifier = modifier
            .size(46.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        color = bgColor,
        contentColor = if (isPressed) CyanAccent else Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * 360-Degree Analog Virtual Joystick
 */
@Composable
fun GamepadAnalogJoystick(
    onDirection: (TvRemoteKey) -> Unit,
    isLite: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Base boundary size: 170dp, knob: 58dp
    val baseRadiusPx = with(density) { 75.dp.toPx() }
    val knobRadiusPx = with(density) { 26.dp.toPx() }
    val maxTravelPx = baseRadiusPx - knobRadiusPx

    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var knobOffsetY by remember { mutableFloatStateOf(0f) }
    var lastEmittedKey by remember { mutableStateOf<TvRemoteKey?>(null) }
    var lastEmitTime by remember { mutableLongStateOf(0L) }

    // Throttle rate: 90ms for standard, 130ms for Lite Mode
    val throttleInterval = if (isLite) 130L else 90L

    fun processJoystick(x: Float, y: Float) {
        val dist = sqrt(x * x + y * y)
        if (dist > maxTravelPx * 0.32f) {
            val angleRad = atan2(y.toDouble(), x.toDouble())
            val angleDeg = Math.toDegrees(angleRad)

            val directionKey = when {
                angleDeg in -45.0..45.0 -> TvRemoteKey.RIGHT
                angleDeg in 45.0..135.0 -> TvRemoteKey.DOWN
                angleDeg in -135.0..-45.0 -> TvRemoteKey.UP
                else -> TvRemoteKey.LEFT
            }

            val now = System.currentTimeMillis()
            if (directionKey != lastEmittedKey || now - lastEmitTime > throttleInterval) {
                lastEmittedKey = directionKey
                lastEmitTime = now
                onDirection(directionKey)
            }
        } else {
            lastEmittedKey = null
        }
    }

    Box(
        modifier = modifier
            .size(170.dp)
            .clip(CircleShape)
            .background(Slate900)
            .then(
                if (isLite) Modifier else Modifier.border(
                    2.dp,
                    Brush.radialGradient(listOf(Slate700, Slate850)),
                    CircleShape
                )
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobOffsetX = 0f
                        knobOffsetY = 0f
                        lastEmittedKey = null
                    },
                    onDragCancel = {
                        knobOffsetX = 0f
                        knobOffsetY = 0f
                        lastEmittedKey = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newX = knobOffsetX + dragAmount.x
                        val newY = knobOffsetY + dragAmount.y
                        val distance = sqrt(newX * newX + newY * newY)

                        if (distance <= maxTravelPx) {
                            knobOffsetX = newX
                            knobOffsetY = newY
                        } else {
                            val angle = atan2(newY.toDouble(), newX.toDouble())
                            knobOffsetX = (maxTravelPx * cos(angle)).toFloat()
                            knobOffsetY = (maxTravelPx * sin(angle)).toFloat()
                        }

                        processJoystick(knobOffsetX, knobOffsetY)
                    }
                )
            }
            .testTag("gamepad_analog_joystick"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle directional guideline reticles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            // Inner deadzone ring
            drawCircle(
                color = Slate800,
                radius = maxTravelPx * 0.35f,
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
            // Outer max ring
            drawCircle(
                color = Slate700.copy(alpha = 0.5f),
                radius = maxTravelPx,
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )
            // Crosshairs
            drawLine(
                color = Slate800,
                start = Offset(center.x, center.y - maxTravelPx),
                end = Offset(center.x, center.y + maxTravelPx),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Slate800,
                start = Offset(center.x - maxTravelPx, center.y),
                end = Offset(center.x + maxTravelPx, center.y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Center Thumb Knob
        val isOffCenter = sqrt(knobOffsetX * knobOffsetX + knobOffsetY * knobOffsetY) > 8f
        val knobColor = if (isOffCenter) CyanAccent else Slate700

        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffsetX.roundToInt(), knobOffsetY.roundToInt()) }
                .size(54.dp)
                .clip(CircleShape)
                .then(
                    if (isLite) Modifier.background(Slate800)
                    else Modifier.background(Brush.radialGradient(listOf(Slate700, Slate850)))
                )
                .border(2.dp, knobColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(knobColor)
            )
        }
    }
}

/**
 * Diamond Layout for Action Buttons: A (bottom), B (right), X (left), Y (top)
 */
@Composable
fun GamepadActionDiamond(
    isTurbo: Boolean,
    isLite: Boolean,
    onKeyPress: (TvRemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(170.dp),
        contentAlignment = Alignment.Center
    ) {
        // Y (Top - Amber)
        GamepadDiamondButton(
            label = "Y",
            accentColor = AmberAccent,
            key = TvRemoteKey.BUTTON_Y,
            isTurbo = isTurbo,
            isLite = isLite,
            onPress = onKeyPress,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .testTag("gamepad_btn_y")
        )

        // B (Right - Rose)
        GamepadDiamondButton(
            label = "B",
            accentColor = RoseAccent,
            key = TvRemoteKey.BUTTON_B,
            isTurbo = isTurbo,
            isLite = isLite,
            onPress = onKeyPress,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .testTag("gamepad_btn_b")
        )

        // A (Bottom - Emerald)
        GamepadDiamondButton(
            label = "A",
            accentColor = EmeraldAccent,
            key = TvRemoteKey.BUTTON_A,
            isTurbo = isTurbo,
            isLite = isLite,
            onPress = onKeyPress,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag("gamepad_btn_a")
        )

        // X (Left - Cyan)
        GamepadDiamondButton(
            label = "X",
            accentColor = CyanAccent,
            key = TvRemoteKey.BUTTON_X,
            isTurbo = isTurbo,
            isLite = isLite,
            onPress = onKeyPress,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .testTag("gamepad_btn_x")
        )
    }
}

@Composable
fun GamepadDiamondButton(
    label: String,
    accentColor: Color,
    key: TvRemoteKey,
    isTurbo: Boolean,
    isLite: Boolean,
    onPress: (TvRemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Handle Turbo rapid-fire when button is held down
    LaunchedEffect(isPressed, isTurbo) {
        if (isPressed) {
            onPress(key)
            if (isTurbo) {
                while (isActive) {
                    delay(110L)
                    onPress(key)
                }
            }
        }
    }

    val scale = if (isPressed) 0.88f else 1f
    val bg = if (isPressed) accentColor.copy(alpha = 0.35f) else Slate800

    Surface(
        modifier = modifier
            .size(50.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) {
                // If not turbo, LaunchedEffect already handled first press
            },
        color = bg,
        border = if (isLite) null else androidx.compose.foundation.BorderStroke(2.dp, accentColor),
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
        }
    }
}

@Composable
fun GamepadTriggerButton(
    label: String,
    key: TvRemoteKey,
    onClick: () -> Unit,
    accentColor: Color,
    isLite: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        modifier = modifier
            .size(width = 66.dp, height = 38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        color = if (isPressed) accentColor.copy(alpha = 0.3f) else Slate900,
        border = if (isLite) null else androidx.compose.foundation.BorderStroke(1.5.dp, if (isPressed) accentColor else Slate700),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) accentColor else Color.White
            )
        }
    }
}

@Composable
fun GamepadAuxButton(
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag(testTag),
        color = Slate800,
        contentColor = Slate400
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
