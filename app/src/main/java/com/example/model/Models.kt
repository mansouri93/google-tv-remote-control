package com.example.model

data class TvDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 5555,
    val model: String = "Google TV",
    val isOnline: Boolean = true,
    val isConnected: Boolean = false,
    val latencyMs: Long = 12L,
    val isFavorite: Boolean = false,
    val isSimulated: Boolean = false
)

data class TvApp(
    val name: String,
    val packageName: String,
    val iconName: String,
    val category: String = "Media",
    val isFavorite: Boolean = false
)

data class TvFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModified: Long = System.currentTimeMillis(),
    val permissions: String = "rw-rw----"
) {
    fun formatSize(isFarsi: Boolean): String = when {
        isDirectory -> if (isFarsi) "پوشه" else "Folder"
        sizeBytes < 1024 -> if (isFarsi) "$sizeBytes بایت" else "$sizeBytes B"
        sizeBytes < 1024 * 1024 -> if (isFarsi) "${sizeBytes / 1024} کیلوبایت" else "${sizeBytes / 1024} KB"
        sizeBytes < 1024 * 1024 * 1024 -> if (isFarsi) String.format("%.1f مگابایت", sizeBytes / (1024.0 * 1024.0)) else String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
        else -> if (isFarsi) String.format("%.2f گیگابایت", sizeBytes / (1024.0 * 1024.0 * 1024.0)) else String.format("%.2f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0))
    }

    val formattedSize: String
        get() = formatSize(true)

    val fileType: String
        get() = when {
            isDirectory -> "folder"
            name.endsWith(".apk", ignoreCase = true) -> "apk"
            name.endsWith(".mp4", ignoreCase = true) || name.endsWith(".mkv", ignoreCase = true) || name.endsWith(".avi", ignoreCase = true) -> "video"
            name.endsWith(".mp3", ignoreCase = true) || name.endsWith(".aac", ignoreCase = true) || name.endsWith(".wav", ignoreCase = true) -> "audio"
            name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".png", ignoreCase = true) || name.endsWith(".webp", ignoreCase = true) -> "image"
            name.endsWith(".txt", ignoreCase = true) || name.endsWith(".pdf", ignoreCase = true) || name.endsWith(".json", ignoreCase = true) -> "doc"
            else -> "other"
        }
}

enum class TvRemoteKey(val keyCode: Int, val label: String) {
    UP(19, "بالا"),
    DOWN(20, "پایین"),
    LEFT(21, "چپ"),
    RIGHT(22, "راست"),
    CENTER(23, "انتخاب"),
    BACK(4, "بازگشت"),
    HOME(3, "خانه"),
    MENU(82, "منو"),
    VOLUME_UP(24, "صدا +"),
    VOLUME_DOWN(25, "صدا -"),
    VOLUME_MUTE(164, "بی‌صدا"),
    POWER(26, "روشن/خاموش"),
    PLAY_PAUSE(85, "پخش/توقف"),
    REWIND(89, "عقب"),
    FAST_FORWARD(90, "جلو"),
    SETTINGS(176, "تنظیمات"),
    GOOGLE_ASSISTANT(219, "دستیار صوتی"),
    // Gamepad Buttons
    BUTTON_A(96, "A"),
    BUTTON_B(97, "B"),
    BUTTON_X(99, "X"),
    BUTTON_Y(100, "Y"),
    BUTTON_L1(102, "L1"),
    BUTTON_R1(103, "R1"),
    BUTTON_L2(104, "L2"),
    BUTTON_R2(105, "R2"),
    BUTTON_START(108, "START"),
    BUTTON_SELECT(109, "SELECT")
}

data class TvScreenState(
    val cursorX: Float = 960f,
    val cursorY: Float = 540f,
    val screenWidth: Int = 1920,
    val screenHeight: Int = 1080,
    val isCursorVisible: Boolean = true,
    val currentActiveApp: String = "Google TV Home",
    val volumeLevel: Int = 45,
    val isMuted: Boolean = false,
    val powerOn: Boolean = true,
    val lastAction: String = "آماده دریافت دستورات",
    val lastActionTimestamp: Long = System.currentTimeMillis()
)
