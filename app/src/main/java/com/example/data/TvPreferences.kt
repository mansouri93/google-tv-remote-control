package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.util.AppLanguage
import com.example.viewmodel.GamepadControlMode

class TvPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tv_remote_settings", Context.MODE_PRIVATE)

    var language: AppLanguage
        get() {
            val code = prefs.getString("key_language", AppLanguage.FA.code) ?: AppLanguage.FA.code
            return if (code == AppLanguage.EN.code) AppLanguage.EN else AppLanguage.FA
        }
        set(value) {
            prefs.edit().putString("key_language", value.code).apply()
        }

    var isLiteMode: Boolean
        get() = prefs.getBoolean("key_lite_mode", true)
        set(value) = prefs.edit().putBoolean("key_lite_mode", value).apply()

    var mouseSensitivity: Float
        get() = prefs.getFloat("key_mouse_sensitivity", 1.2f)
        set(value) = prefs.edit().putFloat("key_mouse_sensitivity", value).apply()

    var showTvMonitor: Boolean
        get() = prefs.getBoolean("key_show_monitor", true)
        set(value) = prefs.edit().putBoolean("key_show_monitor", value).apply()

    var gamepadControlMode: GamepadControlMode
        get() {
            val modeName = prefs.getString("key_gamepad_mode", GamepadControlMode.DPAD.name)
            return try {
                GamepadControlMode.valueOf(modeName ?: GamepadControlMode.DPAD.name)
            } catch (e: Exception) {
                GamepadControlMode.DPAD
            }
        }
        set(value) = prefs.edit().putString("key_gamepad_mode", value.name).apply()

    var isTurboEnabled: Boolean
        get() = prefs.getBoolean("key_turbo_enabled", false)
        set(value) = prefs.edit().putBoolean("key_turbo_enabled", value).apply()

    var lastConnectedIp: String?
        get() = prefs.getString("key_last_ip", null)
        set(value) = prefs.edit().putString("key_last_ip", value).apply()

    var lastConnectedName: String?
        get() = prefs.getString("key_last_name", null)
        set(value) = prefs.edit().putString("key_last_name", value).apply()

    var lastConnectedPort: Int
        get() = prefs.getInt("key_last_port", 5555)
        set(value) = prefs.edit().putInt("key_last_port", value).apply()

    var lastConnectedModel: String
        get() = prefs.getString("key_last_model", "Google TV") ?: "Google TV"
        set(value) = prefs.edit().putString("key_last_model", value).apply()
}
