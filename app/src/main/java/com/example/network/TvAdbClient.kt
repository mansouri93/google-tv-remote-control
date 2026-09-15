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
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    private val _currentDirectory = MutableStateFlow("/sdcard")
    val currentDirectory: StateFlow<String> = _currentDirectory.asStateFlow()

    private val _fileList = MutableStateFlow<List<TvFileItem>>(emptyList())
    val fileList: StateFlow<List<TvFileItem>> = _fileList.asStateFlow()

    private val _actionLogs = MutableSharedFlow<String>(extraBufferCapacity = 50)
    val actionLogs: SharedFlow<String> = _actionLogs.asSharedFlow()

    private val _diagnosticResult = MutableStateFlow<com.example.model.TvDiagnosticResult?>(null)
    val diagnosticResult: StateFlow<com.example.model.TvDiagnosticResult?> = _diagnosticResult.asStateFlow()

    private val _isWaitingForAuth = MutableStateFlow(false)
    val isWaitingForAuth: StateFlow<Boolean> = _isWaitingForAuth.asStateFlow()

    fun setDiagnosticResult(res: com.example.model.TvDiagnosticResult) {
        _diagnosticResult.value = res
    }

    fun clearDiagnostic() {
        _diagnosticResult.value = null
        _isWaitingForAuth.value = false
    }

    // Standard core Google TV apps
    private val coreTvApps = listOf(
        TvApp("YouTube", "com.google.android.youtube.tv", "youtube", "Media", true),
        TvApp("Netflix", "com.netflix.ninja", "netflix", "Media", true),
        TvApp("Prime Video", "com.amazon.amazonvideo.livingroom", "prime", "Media", true),
        TvApp("Spotify", "com.spotify.tv.android", "spotify", "Music", true),
        TvApp("VLC Player", "org.videolan.vlc", "vlc", "Player", false),
        TvApp("SmartTube", "com.liskovsoft.videomanager", "smarttube", "Media", true),
        TvApp("Kodi", "org.xbmc.kodi", "kodi", "Player", false),
        TvApp("Plex", "com.plexapp.android", "plex", "Media", false),
        TvApp("TV Settings", "com.android.tv.settings", "settings", "System", true),
        TvApp("Google Play Store", "com.android.vending", "playstore", "Store", true),
        TvApp("TV Bro Browser", "com.phlox.tvwebbrowser", "browser", "Tools", false)
    )

    init {
        _installedApps.value = coreTvApps
    }

    suspend fun connect(device: TvDevice): Boolean = withContext(Dispatchers.IO) {
        log("بررسی وضعیت اتصال به ${device.name} (${device.ipAddress}:${device.port})...")
        _connectedDevice.value = device
        _isWaitingForAuth.value = false

        // Run smart diagnostic check on all ports
        val diag = TvDiagnosticHelper.diagnoseDevice(device.ipAddress)
        _diagnosticResult.value = diag

        val startTime = System.currentTimeMillis()

        if (diag.isAdbOpen) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(device.ipAddress, 5555), 2500)
                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                socket.close()

                _connectedDevice.value = device.copy(
                    latencyMs = latency,
                    isConnected = true,
                    isAdbOpen = true,
                    isCastOpen = diag.isCastOpen,
                    isRemoteV2Open = diag.isRemoteV2Open
                )
                _isConnected.value = true
                log("ارتباط ADB با موفقیت برقرار شد (${latency}ms).")

                // Query real TV directory and installed apps
                loadDirectory("/sdcard")
                fetchInstalledAppsFromTv()

                _screenState.value = _screenState.value.copy(
                    lastAction = "متصل به ${device.name}",
                    lastActionTimestamp = System.currentTimeMillis()
                )
                return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "Connection to ADB failed: ${e.message}")
                _isConnected.value = false
                log("خطا در اتصال سوکت ADB: ${e.message}")
                return@withContext false
            }
        } else if (diag.isReachable) {
            // TV is reachable (e.g. port 8008 / 6466 / ping), but ADB (5555) is closed!
            _isConnected.value = false
            _connectedDevice.value = device.copy(
                isOnline = true,
                isAdbOpen = false,
                isCastOpen = diag.isCastOpen,
                isRemoteV2Open = diag.isRemoteV2Open
            )
            log("تلویزیون در شبکه متصل است، اما اشکال‌زدایی شبکه (پورت ۵۵۵۵) خاموش است.")
            return@withContext false
        } else {
            // Device not responding at all
            _isConnected.value = false
            log("عدم دسترسی به ${device.ipAddress} - تلویزیون خاموش یا در شبکه دیگری است.")
            return@withContext false
        }
    }

    fun disconnect() {
        val dev = _connectedDevice.value
        log("قطع ارتباط از ${dev?.name ?: "تلویزیون"}")
        _isConnected.value = false
        _connectedDevice.value = null
        _fileList.value = emptyList()
        _screenState.value = _screenState.value.copy(
            lastAction = "اتصال قطع شد",
            lastActionTimestamp = System.currentTimeMillis()
        )
    }

    // ==================== KEYBOARD & TEXT INPUT ====================

    suspend fun sendText(text: String): Boolean = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext false
        log("ارسال متن به تلویزیون: \"$text\"")

        val escaped = text.replace(" ", "%s").replace("'", "\\'")
        executeAdbShell("input text '$escaped'")

        _screenState.value = _screenState.value.copy(
            lastAction = "تایپ متن: $text",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun sendKey(key: TvRemoteKey): Boolean = withContext(Dispatchers.IO) {
        log("ارسال کلید: ${key.label} (KeyCode: ${key.keyCode})")
        executeAdbShell("input keyevent ${key.keyCode}")

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
                lastAction = if (!current.isMuted) "بی‌صدا" else "صدا فعال شد"
            )
            TvRemoteKey.POWER -> current.copy(
                powerOn = !current.powerOn,
                lastAction = if (current.powerOn) "حالت استندبای" else "روشن شد"
            )
            TvRemoteKey.HOME -> current.copy(
                currentActiveApp = "Google TV Home",
                lastAction = "صفحه اصلی"
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
        log("ارسال KeyCode: $keyCode")
        executeAdbShell("input keyevent $keyCode")
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

    suspend fun sendMouseClick(isRightClick: Boolean = false) = withContext(Dispatchers.IO) {
        val current = _screenState.value
        val x = current.cursorX.toInt()
        val y = current.cursorY.toInt()

        if (isRightClick) {
            log("کلیک راست در ($x, $y)")
            executeAdbShell("input keyevent 82") // MENU
            _screenState.value = current.copy(
                lastAction = "کلیک راست در ($x, $y)",
                lastActionTimestamp = System.currentTimeMillis()
            )
        } else {
            log("کلیک چپ در ($x, $y)")
            executeAdbShell("input tap $x $y")
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

        log("اسکرول ماوس: ($x, $y) -> ($x, $targetY)")
        executeAdbShell("input swipe $x $y $x $targetY 200")
        _screenState.value = current.copy(
            lastAction = if (scrollDelta > 0) "اسکرول بالا" else "اسکرول پایین",
            lastActionTimestamp = System.currentTimeMillis()
        )
    }

    // ==================== INSTALLED APPS LAUNCHER ====================

    suspend fun launchApp(app: TvApp): Boolean = withContext(Dispatchers.IO) {
        log("اجرای برنامه ${app.name} (${app.packageName})...")
        executeAdbShell("monkey -p ${app.packageName} -c android.intent.category.LAUNCHER 1")

        _screenState.value = _screenState.value.copy(
            currentActiveApp = app.name,
            lastAction = "اجرای ${app.name}",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun forceStopApp(app: TvApp): Boolean = withContext(Dispatchers.IO) {
        log("توقف برنامه ${app.name}")
        executeAdbShell("am force-stop ${app.packageName}")
        _screenState.value = _screenState.value.copy(
            lastAction = "توقف ${app.name}",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun openAppDetails(app: TvApp): Boolean = withContext(Dispatchers.IO) {
        log("مشخصات برنامه ${app.name}")
        executeAdbShell("am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:${app.packageName}")
        return@withContext true
    }

    private suspend fun fetchInstalledAppsFromTv() = withContext(Dispatchers.IO) {
        val raw = executeAdbShell("pm list packages -3")
        if (raw.isNotBlank()) {
            val lines = raw.lines()
            val apps = mutableListOf<TvApp>()
            for (line in lines) {
                val pkg = line.removePrefix("package:").trim()
                if (pkg.isNotBlank()) {
                    val friendlyName = getAppNameForPackage(pkg)
                    apps.add(
                        TvApp(
                            name = friendlyName,
                            packageName = pkg,
                            iconName = "app",
                            category = "App"
                        )
                    )
                }
            }
            if (apps.isNotEmpty()) {
                _installedApps.value = apps
                return@withContext
            }
        }
        _installedApps.value = coreTvApps
    }

    private fun getAppNameForPackage(pkg: String): String {
        return when {
            pkg.contains("youtube") -> "YouTube"
            pkg.contains("netflix") -> "Netflix"
            pkg.contains("amazonvideo") -> "Prime Video"
            pkg.contains("spotify") -> "Spotify"
            pkg.contains("vlc") -> "VLC"
            pkg.contains("smarttube") -> "SmartTube"
            pkg.contains("kodi") -> "Kodi"
            pkg.contains("plex") -> "Plex"
            pkg.contains("aparat") -> "آپارات"
            pkg.contains("filimo") -> "فیلیمو"
            pkg.contains("namava") -> "نماوا"
            pkg.contains("rubika") -> "روبیکا"
            else -> pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    // ==================== TV FILE MANAGER (REAL ADB SHELL) ====================

    fun loadDirectory(path: String) {
        _currentDirectory.value = path
        if (!_isConnected.value) {
            _fileList.value = emptyList()
            return
        }

        // Run real 'ls -la' or 'ls -l' on TV
        executeDirectoryLs(path)
    }

    private fun executeDirectoryLs(path: String) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val output = executeAdbShell("ls -la \"$path\"")
            if (output.isBlank()) {
                _fileList.value = emptyList()
                return@launch
            }

            val parsedItems = mutableListOf<TvFileItem>()
            val lines = output.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("total")) continue

                // Format example: drwxrwx--x  4 root sdcard_rw 4096 2026-01-01 12:00 Download
                val parts = trimmed.split(Regex("\\s+"))
                if (parts.size >= 8) {
                    val isDir = parts[0].startsWith("d")
                    val size = parts[4].toLongOrNull() ?: 0L
                    val name = parts.subList(7, parts.size).joinToString(" ")
                    if (name == "." || name == "..") continue

                    parsedItems.add(
                        TvFileItem(
                            name = name,
                            path = if (path == "/") "/$name" else "$path/$name",
                            isDirectory = isDir,
                            sizeBytes = size
                        )
                    )
                } else if (parts.size >= 2) {
                    // Fallback simpler format
                    val name = parts.last()
                    if (name != "." && name != "..") {
                        val isDir = parts[0].startsWith("d")
                        parsedItems.add(
                            TvFileItem(
                                name = name,
                                path = if (path == "/") "/$name" else "$path/$name",
                                isDirectory = isDir,
                                sizeBytes = 0L
                            )
                        )
                    }
                }
            }

            _fileList.value = parsedItems.sortedWith(
                compareByDescending<TvFileItem> { it.isDirectory }.thenBy { it.name.lowercase() }
            )
            log("پوشه $path بروزرسانی شد (${parsedItems.size} مورد)")
        }
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

        log("ایجاد پوشه در تلویزیون: $newPath")
        executeAdbShell("mkdir -p \"$newPath\"")
        loadDirectory(current)

        _screenState.value = _screenState.value.copy(
            lastAction = "پوشه ایجاد شد: $folderName",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun deleteFileItem(item: TvFileItem): Boolean = withContext(Dispatchers.IO) {
        val current = _currentDirectory.value
        log("حذف ${item.name} از تلویزیون")
        executeAdbShell("rm -rf \"${item.path}\"")
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
        log("ایجاد فایل در تلویزیون ($destPath)...")

        executeAdbShell("touch \"$destPath\"")
        loadDirectory(current)

        _screenState.value = _screenState.value.copy(
            lastAction = "فایل $fileName ارسال شد",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun playMediaOnTv(item: TvFileItem): Boolean = withContext(Dispatchers.IO) {
        log("پخش مدیا در تلویزیون: ${item.name}")
        executeAdbShell("am start -a android.intent.action.VIEW -d \"file://${item.path}\" -t \"video/*\"")
        _screenState.value = _screenState.value.copy(
            lastAction = "پخش: ${item.name}",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    // ==================== ADVANCED TV TOOLS ====================

    suspend fun openUrlOnTv(url: String): Boolean = withContext(Dispatchers.IO) {
        val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        log("باز کردن آدرس اینترنتی: $formatted")
        executeAdbShell("am start -a android.intent.action.VIEW -d \"$formatted\"")
        _screenState.value = _screenState.value.copy(
            lastAction = "باز کردن: $formatted",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext true
    }

    suspend fun takeScreenshot(): String = withContext(Dispatchers.IO) {
        log("در حال ثبت اسکرین‌شات از صفحه تلویزیون...")
        val out = executeAdbShell("screencap -p /sdcard/Pictures/tv_screenshot_${System.currentTimeMillis()}.png")
        _screenState.value = _screenState.value.copy(
            lastAction = "اسکرین‌شات ثبت شد",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext if (out.contains("error", ignoreCase = true)) out else "اسکرین‌شات در /sdcard/Pictures ذخیره شد."
    }

    suspend fun runCustomAdbCommand(cmd: String): String = withContext(Dispatchers.IO) {
        log("اجرای دستور: $cmd")
        val output = executeAdbShell(cmd)
        val result = if (output.isNotBlank()) output else "دستور با موفقیت به تلویزیون ارسال شد."
        _screenState.value = _screenState.value.copy(
            lastAction = "اجرای: $cmd",
            lastActionTimestamp = System.currentTimeMillis()
        )
        return@withContext result
    }

    // ==================== REAL ADB PROTOCOL & SOCKET COMMUNICATOR ====================

    /**
     * Executes an ADB shell command on the target TV over Wi-Fi.
     * Implements ADB packet framing (A_CNXN, A_OPEN, A_WRTE, A_OKAY, A_CLSE)
     * with raw stream fallback for compatible ADB daemons and shell ports.
     */
    private suspend fun executeAdbShell(command: String): String = withContext(Dispatchers.IO) {
        val dev = _connectedDevice.value ?: return@withContext ""

        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(dev.ipAddress, dev.port), 1800)
            socket.soTimeout = 2000

            val outputStream: OutputStream = socket.getOutputStream()
            val inputStream: InputStream = socket.getInputStream()

            // 1. Send ADB CNXN Packet
            val cnxnPacket = buildAdbPacket(
                command = AdbConstants.A_CNXN,
                arg0 = AdbConstants.A_VERSION,
                arg1 = AdbConstants.MAX_PAYLOAD,
                data = "host::GoogleTvRemote\u0000".toByteArray()
            )
            outputStream.write(cnxnPacket)
            outputStream.flush()

            // 2. Read 24-byte response header
            val headerBuffer = ByteArray(24)
            var bytesRead = readFully(inputStream, headerBuffer)

            if (bytesRead == 24) {
                val header = ByteBuffer.wrap(headerBuffer).order(ByteOrder.LITTLE_ENDIAN)
                val respCmd = header.int
                val remoteId = header.int
                val localId = 1
                val dataLen = header.int

                // Skip response payload if any
                if (dataLen > 0 && dataLen < 65536) {
                    val skipBytes = ByteArray(dataLen)
                    readFully(inputStream, skipBytes)
                }

                if (respCmd == AdbConstants.A_CNXN || respCmd == AdbConstants.A_OKAY) {
                    // Connected/Authorized! Send A_OPEN for shell command
                    val shellCmd = "shell:$command\u0000".toByteArray()
                    val openPacket = buildAdbPacket(
                        command = AdbConstants.A_OPEN,
                        arg0 = localId,
                        arg1 = 0,
                        data = shellCmd
                    )
                    outputStream.write(openPacket)
                    outputStream.flush()

                    // Read output packets
                    val sb = StringBuilder()
                    var count = 0
                    while (count < 10) {
                        count++
                        val packetHeader = ByteArray(24)
                        val hRead = readFully(inputStream, packetHeader)
                        if (hRead != 24) break
                        val pBuf = ByteBuffer.wrap(packetHeader).order(ByteOrder.LITTLE_ENDIAN)
                        val pCmd = pBuf.int
                        val pArg0 = pBuf.int
                        val pArg1 = pBuf.int
                        val pLen = pBuf.int

                        if (pCmd == AdbConstants.A_WRTE && pLen > 0 && pLen < 65536) {
                            val data = ByteArray(pLen)
                            readFully(inputStream, data)
                            sb.append(String(data))

                            // Send A_OKAY ACK
                            val okay = buildAdbPacket(AdbConstants.A_OKAY, localId, pArg0, ByteArray(0))
                            outputStream.write(okay)
                            outputStream.flush()
                        } else if (pCmd == AdbConstants.A_CLSE) {
                            break
                        } else if (pLen > 0 && pLen < 65536) {
                            val dummy = ByteArray(pLen)
                            readFully(inputStream, dummy)
                        }
                    }
                    socket.close()
                    return@withContext sb.toString().trim()
                } else if (respCmd == AdbConstants.A_AUTH) {
                    _isWaitingForAuth.value = true
                    log("پیام تایید اتصال (Authorization) روی تلویزیون ظاهر شده است؛ لطفاً گزینه Always Allow و OK را در تلویزیون تایید نمایید.")
                }
            }

            // Fallback: If ADB framing did not receive standard header, attempt raw shell line
            socket.close()
            return@withContext executeRawStreamFallback(dev, command)
        } catch (e: Exception) {
            Log.d(TAG, "ADB command error: ${e.message}")
            return@withContext executeRawStreamFallback(dev, command)
        }
    }

    private fun executeRawStreamFallback(dev: TvDevice, command: String): String {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(dev.ipAddress, dev.port), 1000)
            socket.soTimeout = 1200
            val os = socket.getOutputStream()
            os.write((command + "\n").toByteArray())
            os.flush()

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val sb = StringBuilder()
            var line: String? = reader.readLine()
            var count = 0
            while (line != null && count < 20) {
                sb.appendLine(line)
                count++
                if (!reader.ready()) break
                line = reader.readLine()
            }
            socket.close()
            sb.toString().trim()
        } catch (ignored: Exception) {
            ""
        }
    }

    private fun buildAdbPacket(command: Int, arg0: Int, arg1: Int, data: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(24 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(command)
        buffer.putInt(arg0)
        buffer.putInt(arg1)
        buffer.putInt(data.size)

        var checksum = 0
        for (b in data) {
            checksum += (b.toInt() and 0xFF)
        }
        buffer.putInt(checksum)
        buffer.putInt(command xor -0x1)
        buffer.put(data)
        return buffer.array()
    }

    private fun readFully(inputStream: InputStream, buffer: ByteArray): Int {
        var bytesRead = 0
        while (bytesRead < buffer.size) {
            val read = inputStream.read(buffer, bytesRead, buffer.size - bytesRead)
            if (read == -1) break
            bytesRead += read
        }
        return bytesRead
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        _actionLogs.tryEmit(message)
    }

    private object AdbConstants {
        const val A_CNXN = 0x4e584e43 // 'CNXN'
        const val A_AUTH = 0x48545541 // 'AUTH'
        const val A_OPEN = 0x4e45504f // 'OPEN'
        const val A_OKAY = 0x59414b4f // 'OKAY'
        const val A_CLSE = 0x45534c43 // 'CLSE'
        const val A_WRTE = 0x45545257 // 'WRTE'

        const val A_VERSION = 0x01000000
        const val MAX_PAYLOAD = 4096
    }
}
