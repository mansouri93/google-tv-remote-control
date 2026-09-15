package com.example

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TvRemoteKey
import com.example.ui.components.TvHeader
import com.example.ui.dialogs.DevicePickerSheet
import com.example.ui.dialogs.TvSetupHelpDialog
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.GamepadScreen
import com.example.ui.screens.KeyboardScreen
import com.example.ui.screens.MouseScreen
import com.example.ui.screens.RemoteScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.AppLanguage
import com.example.util.EnglishStrings
import com.example.util.LocalAppLanguage
import com.example.util.LocalAppStrings
import com.example.util.LocalLiteMode
import com.example.util.PersianStrings
import com.example.viewmodel.RemoteTab
import com.example.viewmodel.TvRemoteViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TvRemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: TvRemoteViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isLiteMode by viewModel.isLiteMode.collectAsStateWithLifecycle()
    val strings = remember(appLanguage) { if (appLanguage == AppLanguage.FA) PersianStrings else EnglishStrings }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.adbClient.connectedDevice.collectAsStateWithLifecycle()
    val isConnected by viewModel.adbClient.isConnected.collectAsStateWithLifecycle()
    val screenState by viewModel.adbClient.screenState.collectAsStateWithLifecycle()
    val showTvMonitor by viewModel.showTvMonitor.collectAsStateWithLifecycle()

    val discoveredDevices by viewModel.discoveryService.discoveredDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.discoveryService.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.discoveryService.scanProgress.collectAsStateWithLifecycle()

    val showDeviceSheet by viewModel.showDeviceSheet.collectAsStateWithLifecycle()
    val showHelpDialog by viewModel.showHelpDialog.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val mouseSensitivity by viewModel.mouseSensitivity.collectAsStateWithLifecycle()
    val keyboardInputText by viewModel.keyboardInputText.collectAsStateWithLifecycle()
    val installedApps by viewModel.adbClient.installedApps.collectAsStateWithLifecycle()
    val appsSearchQuery by viewModel.appsSearchQuery.collectAsStateWithLifecycle()

    val currentDirectory by viewModel.adbClient.currentDirectory.collectAsStateWithLifecycle()
    val fileList by viewModel.adbClient.fileList.collectAsStateWithLifecycle()

    val customCommand by viewModel.customCommandInput.collectAsStateWithLifecycle()
    val terminalOutput by viewModel.terminalOutput.collectAsStateWithLifecycle()

    val gamepadControlMode by viewModel.gamepadControlMode.collectAsStateWithLifecycle()
    val isTurboEnabled by viewModel.isTurboEnabled.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isGamepadFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearToast()
        }
    }

    CompositionLocalProvider(
        LocalAppLanguage provides appLanguage,
        LocalAppStrings provides strings,
        LocalLiteMode provides isLiteMode
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Slate950,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!(isLandscape && currentTab == RemoteTab.GAMEPAD && isGamepadFullscreen)) {
                    TvHeader(
                        device = connectedDevice,
                        isConnected = isConnected,
                        screenState = screenState,
                        showMonitor = showTvMonitor,
                        language = appLanguage,
                        isLiteMode = isLiteMode,
                        onToggleMonitor = { viewModel.toggleTvMonitor() },
                        onToggleLanguage = { viewModel.toggleLanguage() },
                        onToggleLiteMode = { viewModel.toggleLiteMode() },
                        onOpenDeviceSheet = { viewModel.toggleDeviceSheet(true) },
                        onOpenHelpDialog = { viewModel.toggleHelpDialog(true) },
                        onPowerClick = { viewModel.onKeyPress(TvRemoteKey.POWER) },
                        modifier = Modifier.padding(top = if (isLandscape) 0.dp else 28.dp)
                    )
                }
            },
            bottomBar = {
                if (!(isLandscape && currentTab == RemoteTab.GAMEPAD && isGamepadFullscreen)) {
                    NavigationBar(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .height(if (isLandscape) 46.dp else 56.dp)
                            .testTag("main_navigation_bar"),
                        containerColor = Slate900,
                        tonalElevation = 6.dp
                    ) {
                        val tabs = listOf(
                            Triple(RemoteTab.REMOTE, Icons.Filled.Tv, Icons.Outlined.Tv),
                            Triple(RemoteTab.GAMEPAD, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
                            Triple(RemoteTab.MOUSE, Icons.Filled.Mouse, Icons.Outlined.Mouse),
                            Triple(RemoteTab.KEYBOARD, Icons.Filled.Keyboard, Icons.Outlined.Keyboard),
                            Triple(RemoteTab.APPS, Icons.Filled.Apps, Icons.Outlined.Apps),
                            Triple(RemoteTab.FILES, Icons.Filled.Folder, Icons.Outlined.Folder),
                            Triple(RemoteTab.TOOLS, Icons.Filled.Build, Icons.Outlined.Build)
                        )

                        tabs.forEach { (tab, filledIcon, outlinedIcon) ->
                            val isSelected = currentTab == tab
                            val tabTitle = tab.getTitle(appLanguage == AppLanguage.FA)
                            NavigationBarItem(
                                selected = isSelected,
                                alwaysShowLabel = false,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                                        contentDescription = tabTitle,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = null,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Slate950,
                                    selectedTextColor = CyanAccent,
                                    indicatorColor = CyanAccent,
                                    unselectedIconColor = Slate400,
                                    unselectedTextColor = Slate400
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Slate950)
            ) {
                when (currentTab) {
                    RemoteTab.REMOTE -> {
                        RemoteScreen(
                            onKeyPress = { viewModel.onKeyPress(it) }
                        )
                    }
                    RemoteTab.GAMEPAD -> {
                        GamepadScreen(
                            controlMode = gamepadControlMode,
                            isTurboEnabled = isTurboEnabled,
                            isFullscreen = isGamepadFullscreen,
                            isLandscape = isLandscape,
                            onModeChange = { viewModel.setGamepadControlMode(it) },
                            onToggleTurbo = { viewModel.toggleTurbo() },
                            onToggleFullscreen = { isGamepadFullscreen = !isGamepadFullscreen },
                            onKeyPress = { viewModel.onGamepadKeyPress(it) },
                            onJoystickDirection = { viewModel.onJoystickDirection(it) }
                        )
                    }
                    RemoteTab.MOUSE -> {
                        MouseScreen(
                            screenState = screenState,
                            sensitivity = mouseSensitivity,
                            onMouseMove = { dx, dy -> viewModel.onMouseMove(dx, dy) },
                            onMouseClick = { isRight -> viewModel.onMouseClick(isRight) },
                            onMouseScroll = { delta -> viewModel.onMouseScroll(delta) },
                            onSensitivityChange = { viewModel.setMouseSensitivity(it) }
                        )
                    }
                    RemoteTab.KEYBOARD -> {
                        KeyboardScreen(
                            inputText = keyboardInputText,
                            onInputTextChange = { viewModel.setKeyboardInput(it) },
                            onSendText = { viewModel.sendCurrentKeyboardText() },
                            onBackspace = { viewModel.sendBackspace() },
                            onEnter = { viewModel.sendEnter() },
                            onSpace = { viewModel.sendSpace() },
                            onPasteClipboard = { viewModel.sendPasteClipboard(it) }
                        )
                    }
                    RemoteTab.APPS -> {
                        AppsScreen(
                            apps = installedApps,
                            searchQuery = appsSearchQuery,
                            onSearchQueryChange = { viewModel.setAppsSearchQuery(it) },
                            onLaunchApp = { viewModel.launchApp(it) },
                            onForceStopApp = { viewModel.forceStopApp(it) },
                            onOpenAppDetails = { viewModel.openAppDetails(it) }
                        )
                    }
                    RemoteTab.FILES -> {
                        FilesScreen(
                            currentDirectory = currentDirectory,
                            files = fileList,
                            onNavigateFolder = { viewModel.navigateFolder(it) },
                            onNavigateUp = { viewModel.navigateUp() },
                            onCreateFolder = { viewModel.createFolder(it) },
                            onDeleteFile = { viewModel.deleteFileItem(it) },
                            onUploadSampleFile = { viewModel.uploadSampleFile(it) },
                            onPlayMedia = { viewModel.playMedia(it) }
                        )
                    }
                    RemoteTab.TOOLS -> {
                        ToolsScreen(
                            device = connectedDevice,
                            customCommand = customCommand,
                            terminalOutput = terminalOutput,
                            onCustomCommandChange = { viewModel.setCustomCommand(it) },
                            onExecuteCommand = { viewModel.executeTerminalCommand() },
                            onOpenWebUrl = { viewModel.openWebUrl(it) },
                            onTakeScreenshot = { viewModel.takeScreenshot() },
                            onSetSleepTimer = { viewModel.setSleepTimer(it) }
                        )
                    }
                }
            }
        }

        // Bottom Sheet for Device Discovery & Pairing
        if (showDeviceSheet) {
            DevicePickerSheet(
                devices = discoveredDevices,
                isScanning = isScanning,
                scanProgress = scanProgress,
                currentDevice = connectedDevice,
                onScan = { viewModel.scanDevices() },
                onSelectDevice = { viewModel.connectToDevice(it) },
                onAddManualDevice = { name, ip, port -> viewModel.addManualDevice(name, ip, port) },
                onDismiss = { viewModel.toggleDeviceSheet(false) },
                onShowHelp = { viewModel.toggleHelpDialog(true) }
            )
        }

        // Help Dialog for Google TV Wireless Debugging
        if (showHelpDialog) {
            TvSetupHelpDialog(onDismiss = { viewModel.toggleHelpDialog(false) })
        }
    }
}
