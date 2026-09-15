package com.example.network

import android.util.Log
import com.example.model.DiagnosticStatus
import com.example.model.TvDiagnosticResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

object TvDiagnosticHelper {
    private const val TAG = "TvDiagnosticHelper"

    suspend fun diagnoseDevice(ip: String): TvDiagnosticResult = withContext(Dispatchers.IO) {
        val cleanIp = ip.trim()
        val timeout = 1200

        // Test ports in parallel
        val def5555 = async { testPort(cleanIp, 5555, timeout) }
        val def8008 = async { testPort(cleanIp, 8008, timeout) }
        val def6466 = async { testPort(cleanIp, 6466, timeout) }
        val def8009 = async { testPort(cleanIp, 8009, timeout) }
        val defPing = async { checkPing(cleanIp, timeout) }

        val lat5555 = def5555.await()
        val lat8008 = def8008.await()
        val lat6466 = def6466.await()
        val lat8009 = def8009.await()
        val isPingable = defPing.await()

        val isAdbOpen = lat5555 >= 0
        val isCastOpen = lat8008 >= 0 || lat8009 >= 0
        val isRemoteOpen = lat6466 >= 0
        val isReachable = isAdbOpen || isCastOpen || isRemoteOpen || isPingable

        val bestLatency = listOf(lat5555, lat8008, lat6466, lat8009)
            .filter { it >= 0 }
            .minOrNull() ?: if (isPingable) 20L else -1L

        var tvName = "Google TV ($cleanIp)"
        var tvModel = "Google TV / Android TV"

        if (isCastOpen) {
            val info = queryCastInfo(cleanIp)
            if (info != null) {
                tvName = info.first
                tvModel = info.second
            }
        }

        val status = when {
            isAdbOpen -> DiagnosticStatus.READY_TO_CONNECT
            isCastOpen || isRemoteOpen || isPingable -> DiagnosticStatus.ADB_DEBUGGING_DISABLED
            else -> DiagnosticStatus.OFFLINE
        }

        Log.i(TAG, "Diagnose $cleanIp: reachable=$isReachable, adb=$isAdbOpen, cast=$isCastOpen, status=$status")

        TvDiagnosticResult(
            ip = cleanIp,
            isReachable = isReachable,
            isAdbOpen = isAdbOpen,
            isCastOpen = isCastOpen,
            isRemoteV2Open = isRemoteOpen,
            latencyMs = bestLatency,
            tvName = tvName,
            tvModel = tvModel,
            status = status
        )
    }

    fun queryCastInfoOrFallback(ip: String, hasAdb: Boolean, hasCast: Boolean, hasRemote: Boolean): Pair<String, String> {
        if (hasCast) {
            val info = queryCastInfo(ip)
            if (info != null) return info
        }
        val name = "Google TV ($ip)"
        val model = when {
            hasAdb && hasCast -> "Google TV (ADB & Cast فعال)"
            hasAdb -> "Google TV (ADB فعال)"
            hasCast -> "Google TV (سرویس کست فعال)"
            hasRemote -> "Google TV (ریموت نسخه ۲)"
            else -> "Google TV / Android TV"
        }
        return Pair(name, model)
    }

    private fun testPort(ip: String, port: Int, timeoutMs: Int): Long {
        return try {
            val socket = Socket()
            val startTime = System.currentTimeMillis()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            socket.close()
            latency
        } catch (e: Exception) {
            -1L
        }
    }

    private fun checkPing(ip: String, timeoutMs: Int): Boolean {
        return try {
            InetAddress.getByName(ip).isReachable(timeoutMs)
        } catch (e: Exception) {
            false
        }
    }

    private fun queryCastInfo(ip: String): Pair<String, String>? {
        return try {
            val url = URL("http://$ip:8008/setup/eureka_info")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 1200
            conn.readTimeout = 1200
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val json = JSONObject(response)
                val name = json.optString("name", "Google TV")
                val model = json.optString("model_name", "Google TV / Chromecast")
                Pair(name, model)
            } else {
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
