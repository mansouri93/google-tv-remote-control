package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppTvDatabase
import com.example.data.SavedTvDevice
import com.example.model.TvApp
import com.example.model.TvDevice
import com.example.model.TvFileItem
import com.example.model.TvRemoteKey
import com.example.model.TvScreenState
import com.example.network.TvAdbClient
import com.example.network.TvDiscoveryService
import com.example.util.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RemoteTab {
    REMOTE,
    GAMEPAD,
    MOUSE,
    KEYBOARD,
    APPS,
    FILES,
    TOOLS;

    fun getTitle(isFarsi: Boolean): String = when (this) {
        REMOTE -> if (isFarsi) "ریموت" else "Remote"
        GAMEPAD -> if (isFarsi) "دسته بازی" else "Gamepad"
        MOUSE -> if (isFarsi) "ماوس" else "Mouse"
        KEYBOARD -> if (isFarsi) "کیبورد" else "Keyboard"
        APPS -> if (isFarsi) "برنامه‌ها" else "Apps"
        FILES -> if (isFarsi) "فایل‌ها" else "Files"
        TOOLS -> if (isFarsi) "ابزارها" else "Tools"
    }

    val title: String
        get() = getTitle(true)
}

enum class GamepadControlMode {
    DPAD,
    JOYSTICK
}

class TvRemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppTvDatabase.getDatabase(application)
    private val dao = db.tvDeviceDao()

    val discoveryService = TvDiscoveryService(application)
    val adbClient = TvAdbClient()

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // UI States
    private val _currentTab = MutableStateFlow(RemoteTab.REMOTE)
    val currentTab: StateFlow<RemoteTab> = _currentTab.asStateFlow()

    // Bilingual (FA / EN)
    private val _appLanguage = MutableStateFlow(AppLanguage.FA)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // Performance Mode for Low-End Smartphones (Defaults to true for buttery-smooth experience)
    private val _isLiteMode = MutableStateFlow(true)
    val isLiteMode: StateFlow<Boolean> = _isLiteMode.asStateFlow()

    private val _mouseSensitivity = MutableStateFlow(1.2f)
    val mouseSensitivity: StateFlow<Float> = _mouseSensitivity.asStateFlow()

    private val _keyboardInputText = MutableStateFlow("")
    val keyboardInputText: StateFlow<String> = _keyboardInputText.asStateFlow()

    private val _showDeviceSheet = MutableStateFlow(false)
    val showDeviceSheet: StateFlow<Boolean> = _showDeviceSheet.asStateFlow()

    private val _showHelpDialog = MutableStateFlow(false)
    val showHelpDialog: StateFlow<Boolean> = _showHelpDialog.asStateFlow()

    private val _showTvMonitor = MutableStateFlow(true)
    val showTvMonitor: StateFlow<Boolean> = _showTvMonitor.asStateFlow()

    private val _appsSearchQuery = MutableStateFlow("")
    val appsSearchQuery: StateFlow<String> = _appsSearchQuery.asStateFlow()

    private val _customCommandInput = MutableStateFlow("")
    val customCommandInput: StateFlow<String> = _customCommandInput.asStateFlow()

    private val _terminalOutput = MutableStateFlow("ترمینال آماده است. دستور ADB یا شل را وارد کنید.\nTerminal ready. Enter ADB or shell command.\n")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    private val _gamepadControlMode = MutableStateFlow(GamepadControlMode.DPAD)
    val gamepadControlMode: StateFlow<GamepadControlMode> = _gamepadControlMode.asStateFlow()

    private val _isTurboEnabled = MutableStateFlow(false)
    val isTurboEnabled: StateFlow<Boolean> = _isTurboEnabled.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val savedDevices: StateFlow<List<SavedTvDevice>> = dao.getAllDevices().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        // Auto connect to first simulated TV so app is immediately usable
        viewModelScope.launch {
            val firstDev = discoveryService.discoveredDevices.value.firstOrNull()
            if (firstDev != null) {
                adbClient.connect(firstDev)
            }
        }
    }

    fun selectTab(tab: RemoteTab) {
        vibrateClick()
        _currentTab.value = tab
    }

    fun setMouseSensitivity(value: Float) {
        _mouseSensitivity.value = value
    }

    fun setKeyboardInput(text: String) {
        _keyboardInputText.value = text
    }

    fun setAppsSearchQuery(query: String) {
        _appsSearchQuery.value = query
    }

    fun setCustomCommand(cmd: String) {
        _customCommandInput.value = cmd
    }

    fun toggleLanguage() {
        _appLanguage.value = if (_appLanguage.value == AppLanguage.FA) AppLanguage.EN else AppLanguage.FA
    }

    fun setLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
    }

    fun toggleLiteMode() {
        _isLiteMode.value = !_isLiteMode.value
        val isFa = _appLanguage.value == AppLanguage.FA
        _toastMessage.value = if (_isLiteMode.value) {
            if (isFa) "حالت روان برای گوشی‌های ضعیف فعال شد" else "Smooth Lite Mode enabled for low-end devices"
        } else {
            if (isFa) "حالت گرافیک استاندارد فعال شد" else "Standard visual mode enabled"
        }
    }

    fun toggleDeviceSheet(show: Boolean) {
        _showDeviceSheet.value = show
    }

    fun toggleHelpDialog(show: Boolean) {
        _showHelpDialog.value = show
    }

    fun toggleTvMonitor() {
        _showTvMonitor.value = !_showTvMonitor.value
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // ==================== ACTIONS ====================

    fun connectToDevice(device: TvDevice) {
        vibrateClick()
        viewModelScope.launch {
            val success = adbClient.connect(device)
            val isFa = _appLanguage.value == AppLanguage.FA
            if (success) {
                dao.insertDevice(
                    SavedTvDevice(
                        ipAddress = device.ipAddress,
                        name = device.name,
                        port = device.port,
                        model = device.model,
                        isFavorite = device.isFavorite
                    )
                )
                _toastMessage.value = if (isFa) "متصل شد به ${device.name}" else "Connected to ${device.name}"
                _showDeviceSheet.value = false
            } else {
                _toastMessage.value = if (isFa) "خطا در اتصال به ${device.ipAddress}" else "Failed to connect to ${device.ipAddress}"
            }
        }
    }

    fun disconnect() {
        vibrateClick()
        adbClient.disconnect()
        val isFa = _appLanguage.value == AppLanguage.FA
        _toastMessage.value = if (isFa) "اتصال قطع شد" else "Disconnected"
    }

    fun scanDevices() {
        vibrateClick()
        discoveryService.startDiscovery()
    }

    fun addManualDevice(name: String, ip: String, port: Int) {
        vibrateClick()
        val dev = discoveryService.addManualDevice(name, ip, port)
        connectToDevice(dev)
    }

    // Gamepad Actions
    fun setGamepadControlMode(mode: GamepadControlMode) {
        vibrateClick()
        _gamepadControlMode.value = mode
        val isFa = _appLanguage.value == AppLanguage.FA
        _toastMessage.value = when (mode) {
            GamepadControlMode.DPAD -> if (isFa) "حالت کلیدهای ۴ جهته (D-Pad) فعال شد" else "4-Way D-Pad mode activated"
            GamepadControlMode.JOYSTICK -> if (isFa) "حالت جویستیک آنالوگ ۳۶۰ درجه فعال شد" else "360° Joystick mode activated"
        }
    }

    fun toggleTurbo() {
        vibrateClick()
        _isTurboEnabled.value = !_isTurboEnabled.value
        val isFa = _appLanguage.value == AppLanguage.FA
        _toastMessage.value = if (_isTurboEnabled.value) {
            if (isFa) "حالت توربو (شلیک رگباری) فعال شد" else "Turbo rapid-fire activated"
        } else {
            if (isFa) "حالت توربو غیرفعال شد" else "Turbo deactivated"
        }
    }

    fun onGamepadKeyPress(key: TvRemoteKey) {
        vibrateClick()
        viewModelScope.launch {
            adbClient.sendKey(key)
        }
    }

    fun onJoystickDirection(key: TvRemoteKey) {
        viewModelScope.launch {
            adbClient.sendKey(key)
        }
    }

    // Remote Key Presses
    fun onKeyPress(key: TvRemoteKey) {
        vibrateClick()
        viewModelScope.launch {
            adbClient.sendKey(key)
        }
    }

    // Mouse Actions - Optimized for low-end phones: zero coroutine allocation per move event
    fun onMouseMove(deltaX: Float, deltaY: Float) {
        adbClient.moveCursorSync(deltaX, deltaY, _mouseSensitivity.value)
    }

    fun onMouseClick(isRightClick: Boolean = false) {
        vibrateClick()
        viewModelScope.launch {
            adbClient.sendMouseClick(isRightClick)
        }
    }

    fun onMouseScroll(delta: Float) {
        viewModelScope.launch {
            adbClient.sendMouseScroll(delta)
        }
    }

    // Keyboard
    fun sendCurrentKeyboardText() {
        val text = _keyboardInputText.value
        if (text.isBlank()) return
        vibrateClick()
        val isFa = _appLanguage.value == AppLanguage.FA
        viewModelScope.launch {
            adbClient.sendText(text)
            _toastMessage.value = if (isFa) "متن به تلویزیون ارسال شد" else "Text sent to TV"
            _keyboardInputText.value = ""
        }
    }

    fun sendBackspace() {
        vibrateClick()
        viewModelScope.launch {
            adbClient.sendCustomKeyCode(67) // KEYCODE_DEL
        }
    }

    fun sendEnter() {
        vibrateClick()
        viewModelScope.launch {
            adbClient.sendCustomKeyCode(66) // KEYCODE_ENTER
        }
    }

    fun sendSpace() {
        vibrateClick()
        viewModelScope.launch {
            adbClient.sendCustomKeyCode(62) // KEYCODE_SPACE
        }
    }

    fun sendPasteClipboard(text: String) {
        if (text.isBlank()) return
        vibrateClick()
        val isFa = _appLanguage.value == AppLanguage.FA
        viewModelScope.launch {
            adbClient.sendText(text)
            _toastMessage.value = if (isFa) "از کلیپ‌بورد به تلویزیون ارسال شد" else "Clipboard sent to TV"
        }
    }

    // Apps
    fun launchApp(app: TvApp) {
        vibrateClick()
        val isFa = _appLanguage.value == AppLanguage.FA
        viewModelScope.launch {
            adbClient.launchApp(app)
            _toastMessage.value = if (isFa) "در حال اجرای ${app.name}" else "Launching ${app.name}"
        }
    }

    fun forceStopApp(app: TvApp) {
        vibrateClick()
        val isFa = _appLanguage.value == AppLanguage.FA
        viewModelScope.launch {
            adbClient.forceStopApp(app)
            _toastMessage.value = if (isFa) "${app.name} متوقف شد" else "${app.name} stopped"
        }
    }

    fun openAppDetails(app: TvApp) {
        vibrateClick()
        viewModelScope.launch {
            adbClient.openAppDetails(app)
        }
    }

    // File Manager
    fun navigateFolder(path: String) {
        vibrateClick()
        adbClient.loadDirectory(path)
    }

    fun navigateUp() {
        vibrateClick()
        adbClient.navigateUp()
    }

    fun createFolder(name: String) {
        vibrateClick()
        val isFa = _appLanguage.value == AppLanguage.FA
        viewModelScope.launch {
            val res = adbClient.createFolder(name)
            if (res) _toastMessage.value = if (isFa) "پوشه ایجاد شد" else "Folder created"
        }
    }

    fun deleteFileItem(item: TvFileItem) {
        vibrateClick()
        val isFa = _appLanguage.value == AppLanguage.FA
        viewModelScope.launch {
            val res = adbClient.deleteFileItem(item)
            if (res) _toastMessage.value = if (isFa) "${item.name} حذف شد" else "${item.name} deleted"
        }
    }

    fun uploadSampleFile(type: String) {
        vibrateClick()
        val isFa = _appLanguage.value == AppLanguage.FA
        viewModelScope.launch {
            val fileName = when (type) {
                "apk" -> "New_App_Installer_${System.currentTimeMillis() % 1000}.apk"
                "video" -> "Travel_Vlog_${System.currentTimeMillis() % 1000}.mp4"
                "music" -> "Song_Track_${System.currentTimeMillis() % 1000}.mp3"
                else -> "Document_${System.currentTimeMillis() % 1000}.pdf"
            }
            adbClient.sendFileToTv(fileName, 15_240_000L)
            _toastMessage.value = if (isFa) "فایل $fileName با موفقیت به تلویزیون ارسال شد" else "$fileName sent to TV successfully"
        }
    }

    fun playMedia(item: TvFileItem) {
        vibrateClick()
        val isFa = _appLanguage.value == AppLanguage.FA
        viewModelScope.launch {
            adbClient.playMediaOnTv(item)
            _toastMessage.value = if (isFa) "در حال پخش ${item.name} روی تلویزیون" else "Playing ${item.name} on TV"
        }
    }

    // Tools
    fun openWebUrl(url: String) {
        if (url.isBlank()) return
        vibrateClick()
        val isFa = _appLanguage.value == AppLanguage.FA
        viewModelScope.launch {
            adbClient.openUrlOnTv(url)
            _toastMessage.value = if (isFa) "آدرس اینترنتی روی تلویزیون باز شد" else "URL opened on TV"
        }
    }

    fun takeScreenshot() {
        vibrateClick()
        val isFa = _appLanguage.value == AppLanguage.FA
        viewModelScope.launch {
            val res = adbClient.takeScreenshot()
            _toastMessage.value = if (isFa) res else "Screenshot saved to /sdcard/Pictures"
        }
    }

    fun executeTerminalCommand() {
        val cmd = _customCommandInput.value
        if (cmd.isBlank()) return
        vibrateClick()
        viewModelScope.launch {
            val out = adbClient.runCustomAdbCommand(cmd)
            _terminalOutput.value += "\n$ > $cmd\n$out\n"
            _customCommandInput.value = ""
        }
    }

    fun setSleepTimer(minutes: Int) {
        vibrateClick()
        val isFa = _appLanguage.value == AppLanguage.FA
        _toastMessage.value = if (isFa) "تایمر خاموشی تلویزیون برای $minutes دقیقه تنظیم شد" else "TV sleep timer set for $minutes minutes"
    }

    private fun vibrateClick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30L)
            }
        } catch (ignored: Exception) {}
    }
}
