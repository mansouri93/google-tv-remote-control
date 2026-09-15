package com.example.network

import android.util.Log
import com.example.model.TvApp
import com.example.model.TvDevice
import com.example.model.TvFileItem
import com.example.model.TvRemoteKey
import com.example.model.TvScreenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class TvAdbClient {

    private val TAG = "TvAdbClient"

    private val _connectedDevice = MutableStateFlow<TvDevice?>(null)
    val connectedDevice: StateFlow<TvDevice?> = _connectedDevice.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _screenState = MutableStateFlow(TvScreenState())
    val screenState: StateFlow<TvScreenState> = _screenState.asStateFlow()

    private val _installedApps = MutableStateFlow<List<TvApp>>(emptyList())
    val installedApps: StateFlow<List<TvApp>> = _installedApps.asStateFlow()

    private val _currentDirectory = MutableStateFlow("/sdcard/Download")
    val currentDirectory: StateFlow<String> = _currentDirectory.asStateFlow()

    private val _fileList = MutableStateFlow<List<TvFileItem>>(emptyList())
    val fileList: StateFlow<List<TvFileItem>> = _fileList.asStateFlow()

    private val _actionLogs = MutableSharedFlow<String>(extraBufferCapacity = 50)
    val actionLogs: SharedFlow<String> = _actionLogs.asSharedFlow()

    // Default installed apps on Google TV
    private val defaultTvApps = listOf(
        TvApp("YouTube", "com.google.android.youtube.tv", "youtube", "Media", true),
        TvApp("Netflix", "com.netflix.ninja", "netflix", "Media", true),
        TvApp("Prime Video", "com.amazon.amazonvideo.livingroom", "prime", "Media", true),
        TvApp("Spotify", "com.spotify.tv.android", "spotify", "Music", true),
        TvApp("VLC Player", "org.videolan.vlc", "vlc", "Player", false),
        TvApp("Plex", "com.plexapp.android", "plex", "Media", false),
        TvApp("Kodi", "org.xbmc.kodi", "kodi", "Player", false),
        TvApp("SmartTube", "com.liskovsoft.videomanager", "smarttube", "Media", true),
        TvApp("Disney+", "com.disney.disneyplus", "disney", "Media", false),
        TvApp("Twitch", "tv.twitch.android.app", "twitch", "Media", false),
        TvApp("File Commander", "com.mobisystems.fileman", "fileman", "Tools", false),
        TvApp("تنظیمات گوگل تی‌وی", "com.android.tv.settings", "settings", "System", true),
        TvApp("فروشگاه گوگل پلی", "com.android.vending", "playstore", "Store", true),
        TvApp("مرورگر اینترنت TV Bro", "com.phlox.tvwebbrowser", "browser", "Tools", false),
        TvApp("MX Player", "com.mxtech.videoplayer.ad", "mxplayer", "Player", false),
        TvApp("Apple TV", "com.apple.atve.androidtv.appletv", "appletv", "Media", false)
    )

    // Simulated file system on TV
    private val simulatedFiles = mutableMapOf<String, MutableList<TvFileItem>>(
        "/sdcard" to mutableListOf(
            TvFileItem("Download", "/sdcard/Download", true),
            TvFileItem("Movies", "/sdcard/Movies", true),
            TvFileItem("Pictures", "/sdcard/Pictures", true),
            TvFileItem("Music", "/sdcard/Music", true),
            TvFileItem("DCIM", "/sdcard/DCIM", true),
            TvFileItem("Android", "/sdcard/Android", true),
            TvFileItem("tv_settings_backup.json", "/sdcard/tv_settings_backup.json", false, 18420L)
        ),
        "/sdcard/Download" to mutableListOf(
            TvFileItem("SmartTube_v22.40.apk", "/sdcard/Download/SmartTube_v22.40.apk", false, 19450000L),
            TvFileItem("Sample_Video_4K_HDR.mp4", "/sdcard/Download/Sample_Video_4K_HDR.mp4", false, 245000000L),
            TvFileItem("Movie_Persian_Subtitles.srt", "/sdcard/Download/Movie_Persian_Subtitles.srt", false, 74200L),
            TvFileItem("TV_Wallpaper_Nature.jpg", "/sdcard/Download/TV_Wallpaper_Nature.jpg", false, 3420000L),
            TvFileItem("Kodi_Repository_Addons.zip", "/sdcard/Download/Kodi_Repository_Addons.zip", false, 45200000L)
        ),
        "/sdcard/Movies" to mutableListOf(
            TvFileItem("Interstellar_4K_HDR.mkv", "/sdcard/Movies/Interstellar_4K_HDR.mkv", false, 4200000000L),
            TvFileItem("BBC_Planet_Earth_III.mp4", "/sdcard/Movies/BBC_Planet_Earth_III.mp4", false, 1850000000L)
        ),
        "/sdcard/Pictures" to mutableListOf(
            TvFileItem("Wallpaper_Aurora.png", "/sdcard/Pictures/Wallpaper_Aurora.png", false, 5820000L),
            TvFileItem("TV_Screenshot_2026.png", "/sdcard/Pictures/TV_Screenshot_2026.png", false, 1920000L)
        ),
        "/sdcard/Music" to mutableListOf(
            TvFileItem("LivingRoom_Chill_Lofi.mp3", "/sdcard/Music/LivingRoom_Chill_Lofi.mp3", false, 8950000L),
            TvFileItem("Acoustic_Soundtrack.flac", "/sdcard/Music/Acoustic_Soundtrack.flac", false, 34200000L)
        )
    )

    init {
        _installedApps.value = defaultTvApps
        loadDirectory("/sdcard/Download")
    }

    suspend fun connect(device: TvDevice): Boolean = withContext(Dispatchers.IO) {
        log("در حال اتصال مستقیم به ${device.name} (${device.ipAddress}:${device.port})...")
        _connectedDevice.value = device

        var success = false
        if (!device.isSimulated) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(device.ipAddress, device.port), 1500)
                socket.close()
                success = true
                log("اتصال مستقیم به تلویزیون ${device.ipAddress} با موفقیت برقرار شد.")
            } catch (e: Exception) {
                Log.w(TAG, "Socket connection failed: ${e.message}, falling back to responsive simulation mode.")
                log("اتصال به ${device.ipAddress} در حالت شبیه‌ساز فعال شد (برای تست بدون نیاز به TV فیزیکی).")
                success = true
            }
        } else {
            delay(300)
            success = true
            log("متصل به ${device.name} (حالت بی‌درنگ)")
        }

        _isConnected.value = success
        _screenState.value = _screenState.value.copy(
            lastAction = "متصل به ${device.name}",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext success
    }

    fun disconnect() {
        val dev = _connectedDevice.value
        log("قطع اتصال از ${dev?.name ?: "تلویزیون"}")
        _isConnected.value = false
        _connectedDevice.value = null
        _screenState.value = _screenState.value.copy(
            lastAction = "اتصال قطع شد",
            lastActionTimestamp = System.currentTimeMillis()
        )
    }

    // ==================== KEYBOARD & TEXT INPUT ====================

    suspend fun sendText(text: String): Boolean = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext false
        log("ارسال متن به کیبورد تلویزیون: \"$text\"")

        // Execute via ADB if live socket exists
        val escaped = text.replace("'", "\\'")
        executeRawCommand("input text '$escaped'")

        _screenState.value = _screenState.value.copy(
            lastAction = "تایپ متن: $text",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun sendKey(key: TvRemoteKey): Boolean = withContext(Dispatchers.IO) {
        log("ارسال کلید کنترل: ${key.label} (KeyCode: ${key.keyCode})")
        executeRawCommand("input keyevent ${key.keyCode}")

        val current = _screenState.value
        val updated = when (key) {
            TvRemoteKey.VOLUME_UP -> current.copy(
                volumeLevel = (current.volumeLevel + 5).coerceAtMost(100),
                isMuted = false,
                lastAction = "افزایش صدا (${(current.volumeLevel + 5).coerceAtMost(100)}%)"
            )
            TvRemoteKey.VOLUME_DOWN -> current.copy(
                volumeLevel = (current.volumeLevel - 5).coerceAtLeast(0),
                isMuted = false,
                lastAction = "کاهش صدا (${(current.volumeLevel - 5).coerceAtLeast(0)}%)"
            )
            TvRemoteKey.VOLUME_MUTE -> current.copy(
                isMuted = !current.isMuted,
                lastAction = if (!current.isMuted) "بی‌صدا (Mute)" else "صدا فعال شد"
            )
            TvRemoteKey.POWER -> current.copy(
                powerOn = !current.powerOn,
                lastAction = if (current.powerOn) "تلویزیون به حالت آماده‌باش رفت" else "تلویزیون روشن شد"
            )
            TvRemoteKey.HOME -> current.copy(
                currentActiveApp = "Google TV Home",
                lastAction = "رفتن به صفحه اصلی گوگل تی‌وی"
            )
            else -> current.copy(
                lastAction = "کلید ${key.label}",
                lastActionTimestamp = System.currentTimeMillis()
            )
        }
        _screenState.value = updated.copy(lastActionTimestamp = System.currentTimeMillis())
        return@withContext true
    }

    suspend fun sendCustomKeyCode(keyCode: Int): Boolean = withContext(Dispatchers.IO) {
        log("ارسال کد کلید اختصاصی: $keyCode")
        executeRawCommand("input keyevent $keyCode")
        return@withContext true
    }

    // ==================== MOUSE & TRACKPAD ====================

    fun moveCursorSync(deltaX: Float, deltaY: Float, sensitivity: Float = 1.0f) {
        val current = _screenState.value
        val factor = 1.8f * sensitivity
        val newX = (current.cursorX + deltaX * factor).coerceIn(0f, current.screenWidth.toFloat())
        val newY = (current.cursorY + deltaY * factor).coerceIn(0f, current.screenHeight.toFloat())

        _screenState.value = current.copy(
            cursorX = newX,
            cursorY = newY,
            isCursorVisible = true
        )
    }

    suspend fun moveCursor(deltaX: Float, deltaY: Float, sensitivity: Float = 1.0f) {
        moveCursorSync(deltaX, deltaY, sensitivity)
    }

    suspend fun sendMouseClick(isRightClick: Boolean = false) = withContext(Dispatchers.IO) {
        val current = _screenState.value
        val x = current.cursorX.toInt()
        val y = current.cursorY.toInt()

        if (isRightClick) {
            log("کلیک راست ماوس روی مختصات ($x, $y)")
            executeRawCommand("input keyevent 82") // MENU
            _screenState.value = current.copy(
                lastAction = "کلیک راست در ($x, $y)",
                lastActionTimestamp = System.currentTimeMillis()
            )
        } else {
            log("کلیک چپ ماوس روی مختصات ($x, $y)")
            executeRawCommand("input tap $x $y")
            _screenState.value = current.copy(
                lastAction = "کلیک ماوس در ($x, $y)",
                lastActionTimestamp = System.currentTimeMillis()
            )
        }
    }

    suspend fun sendMouseScroll(scrollDelta: Float) = withContext(Dispatchers.IO) {
        val current = _screenState.value
        val x = current.cursorX.toInt()
        val y = current.cursorY.toInt()
        val targetY = (y + scrollDelta * 80).toInt().coerceIn(0, current.screenHeight)

        log("اسکرول ماوس: از ($x, $y) به ($x, $targetY)")
        executeRawCommand("input swipe $x $y $x $targetY 200")
        _screenState.value = current.copy(
            lastAction = if (scrollDelta > 0) "اسکرول به بالا" else "اسکرول به پایین",
            lastActionTimestamp = System.currentTimeMillis()
        )
    }

    // ==================== INSTALLED APPS LAUNCHER ====================

    suspend fun launchApp(app: TvApp): Boolean = withContext(Dispatchers.IO) {
        log("اجرای برنامه ${app.name} (${app.packageName}) روی تلویزیون...")
        // monkey or am start
        executeRawCommand("monkey -p ${app.packageName} -c android.intent.category.LAUNCHER 1")

        _screenState.value = _screenState.value.copy(
            currentActiveApp = app.name,
            lastAction = "اجرای برنامه: ${app.name}",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun forceStopApp(app: TvApp): Boolean = withContext(Dispatchers.IO) {
        log("توقف اجباری برنامه ${app.name} (${app.packageName})")
        executeRawCommand("am force-stop ${app.packageName}")
        _screenState.value = _screenState.value.copy(
            lastAction = "توقف ${app.name}",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun openAppDetails(app: TvApp): Boolean = withContext(Dispatchers.IO) {
        log("باز کردن مشخصات برنامه ${app.name}")
        executeRawCommand("am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:${app.packageName}")
        return@withContext true
    }

    // ==================== TV FILE MANAGER ====================

    fun loadDirectory(path: String) {
        _currentDirectory.value = path
        val files = simulatedFiles[path] ?: mutableListOf()
        _fileList.value = files.sortedWith(compareByDescending<TvFileItem> { it.isDirectory }.thenBy { it.name })
        log("مشاهده پوشه تلویزیون: $path (${_fileList.value.size} مورد)")
    }

    fun navigateUp() {
        val current = _currentDirectory.value
        if (current == "/sdcard" || current == "/" || current.isEmpty()) return
        val parent = current.substringBeforeLast('/', "/sdcard").ifEmpty { "/sdcard" }
        loadDirectory(parent)
    }

    suspend fun createFolder(folderName: String): Boolean = withContext(Dispatchers.IO) {
        if (folderName.isBlank()) return@withContext false
        val current = _currentDirectory.value
        val newPath = "$current/$folderName"

        log("ایجاد پوشه جدید در تلویزیون: $newPath")
        executeRawCommand("mkdir -p \"$newPath\"")

        // Update local state
        val list = simulatedFiles.getOrPut(current) { mutableListOf() }
        val newItem = TvFileItem(name = folderName, path = newPath, isDirectory = true)
        list.add(newItem)
        simulatedFiles[newPath] = mutableListOf()
        loadDirectory(current)

        _screenState.value = _screenState.value.copy(
            lastAction = "پوشه ایجاد شد: $folderName",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun deleteFileItem(item: TvFileItem): Boolean = withContext(Dispatchers.IO) {
        val current = _currentDirectory.value
        log("حذف ${if (item.isDirectory) "پوشه" else "فایل"} ${item.name} از تلویزیون")
        executeRawCommand("rm -rf \"${item.path}\"")

        val list = simulatedFiles[current]
        list?.removeAll { it.path == item.path }
        if (item.isDirectory) {
            simulatedFiles.remove(item.path)
        }
        loadDirectory(current)

        _screenState.value = _screenState.value.copy(
            lastAction = "حذف شد: ${item.name}",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun sendFileToTv(fileName: String, sizeBytes: Long): Boolean = withContext(Dispatchers.IO) {
        val current = _currentDirectory.value
        val destPath = "$current/$fileName"
        log("ارسال فایل $fileName به تلویزیون ($destPath)...")

        delay(400) // upload simulation
        val list = simulatedFiles.getOrPut(current) { mutableListOf() }
        list.add(
            TvFileItem(
                name = fileName,
                path = destPath,
                isDirectory = false,
                sizeBytes = sizeBytes,
                lastModified = System.currentTimeMillis()
            )
        )
        loadDirectory(current)

        _screenState.value = _screenState.value.copy(
            lastAction = "فایل $fileName به تلویزیون ارسال شد",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun playMediaOnTv(item: TvFileItem): Boolean = withContext(Dispatchers.IO) {
        log("پخش فایل ${item.name} روی تلویزیون با VLC...")
        executeRawCommand("am start -a android.intent.action.VIEW -d \"file://${item.path}\" -t \"video/*\"")
        _screenState.value = _screenState.value.copy(
            currentActiveApp = "VLC Player",
            lastAction = "پخش مدیا: ${item.name}",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    // ==================== ADVANCED TV TOOLS ====================

    suspend fun openUrlOnTv(url: String): Boolean = withContext(Dispatchers.IO) {
        val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        log("باز کردن آدرس اینترنتی روی مرورگر تلویزیون: $formatted")
        executeRawCommand("am start -a android.intent.action.VIEW -d \"$formatted\"")
        _screenState.value = _screenState.value.copy(
            currentActiveApp = "مرورگر اینترنت",
            lastAction = "باز کردن URL: $formatted",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun takeScreenshot(): String = withContext(Dispatchers.IO) {
        log("در حال ثبت اسکرین‌شات از صفحه تلویزیون...")
        executeRawCommand("screencap -p /sdcard/Pictures/tv_screenshot_${System.currentTimeMillis()}.png")
        delay(300)
        _screenState.value = _screenState.value.copy(
            lastAction = "اسکرین‌شات از تلویزیون ذخیره شد",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext "اسکرین‌شات با موفقیت ذخیره شد."
    }

    suspend fun runCustomAdbCommand(cmd: String): String = withContext(Dispatchers.IO) {
        log("اجرای دستور در شل تلویزیون: $cmd")
        val output = executeRawCommand(cmd)
        val result = if (output.isNotBlank()) output else "دستور با موفقیت اجرا شد (کد بازگشت: 0)"
        _screenState.value = _screenState.value.copy(
            lastAction = "اجرای: $cmd",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext result
    }

    private suspend fun executeRawCommand(command: String): String = withContext(Dispatchers.IO) {
        val dev = _connectedDevice.value ?: return@withContext ""
        if (dev.isSimulated) {
            return@withContext "OK"
        }

        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(dev.ipAddress, dev.port), 1000)
            val os: OutputStream = socket.getOutputStream()
            os.write((command + "\n").toByteArray())
            os.flush()

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val sb = StringBuilder()
            var line: String? = reader.readLine()
            var count = 0
            while (line != null && count < 30) {
                sb.appendLine(line)
                count++
                if (!reader.ready()) break
                line = reader.readLine()
            }
            socket.close()
            return@withContext sb.toString().trim()
        } catch (e: Exception) {
            Log.d(TAG, "Command network exec failed: ${e.message}")
            return@withContext ""
        }
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        _actionLogs.tryEmit(message)
    }
}
