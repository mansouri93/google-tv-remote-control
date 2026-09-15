package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.model.TvDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.util.Collections

class TvDiscoveryService(private val context: Context) {

    private val TAG = "TvDiscoveryService"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _currentSubnet = MutableStateFlow<String?>(null)
    val currentSubnet: StateFlow<String?> = _currentSubnet.asStateFlow()

    private var scanJob: Job? = null
    private var nsdListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanProgress.value = 0.05f

        acquireMulticastLock()

        scanJob?.cancel()
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Start mDNS discovery via NsdManager for Google Cast / Android TV
                startNsd()

                // 2. Detect actual local network interface IPv4 prefix
                val (localIp, prefix) = getLocalIpAndSubnet()
                _currentSubnet.value = localIp
                Log.d(TAG, "Local IP detected: $localIp, Scanning subnet: $prefix.*")

                // 3. Scan local Wi-Fi subnet for real TVs (Port 5555 for ADB, 8008 for Cast/DIAL, 6466 for Remote v2)
                scanSubnet(prefix)
            } catch (e: Exception) {
                Log.e(TAG, "Error in discovery process: ${e.message}")
            } finally {
                _isScanning.value = false
                _scanProgress.value = 1f
                releaseMulticastLock()
            }
        }
    }

    fun stopDiscovery() {
        scanJob?.cancel()
        stopNsd()
        releaseMulticastLock()
        _isScanning.value = false
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("TvDiscoveryMulticastLock")?.apply {
                    setReferenceCounted(true)
                }
            }
            multicastLock?.acquire()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire multicast lock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release multicast lock: ${e.message}")
        }
    }

    private fun startNsd() {
        try {
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    Log.e(TAG, "NSD Discovery start failed: $errorCode")
                }

                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}

                override fun onDiscoveryStarted(serviceType: String?) {
                    Log.d(TAG, "NSD discovery started: $serviceType")
                }

                override fun onDiscoveryStopped(serviceType: String?) {}

                override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                    serviceInfo?.let { resolveService(it) }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
            }
            nsdListener = listener
            nsdManager?.discoverServices("_googlecast._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.w(TAG, "NSD error: ${e.message}")
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        try {
            nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.d(TAG, "NSD Resolve failed: $errorCode")
                }

                override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                    val hostAddress = resolvedInfo?.host?.hostAddress ?: return
                    val serviceName = resolvedInfo.serviceName ?: "Google TV"
                    val cleanName = if (serviceName.contains("-")) serviceName.substringBefore("-").trim() else serviceName
                    addOrUpdateDevice(
                        TvDevice(
                            id = "nsd_$hostAddress",
                            name = cleanName,
                            ipAddress = hostAddress,
                            port = 5555,
                            model = "Google TV / Android TV",
                            isOnline = true,
                            latencyMs = 8L,
                            isSimulated = false
                        )
                    )
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Resolve error: ${e.message}")
        }
    }

    private fun stopNsd() {
        nsdListener?.let {
            try {
                nsdManager?.stopServiceDiscovery(it)
            } catch (ignored: Exception) {}
        }
        nsdListener = null
    }

    private suspend fun scanSubnet(subnetPrefix: String) = withContext(Dispatchers.IO) {
        val candidateIps = (1..254).map { "$subnetPrefix.$it" }
        val batchSize = 32

        for (i in candidateIps.indices step batchSize) {
            val batch = candidateIps.subList(i, minOf(i + batchSize, candidateIps.size))
            val deferreds = batch.map { ip ->
                async {
                    checkDeviceAtIp(ip)
                }
            }
            val results = deferreds.awaitAll()
            results.filterNotNull().forEach { found ->
                addOrUpdateDevice(found)
            }
            _scanProgress.value = (i.toFloat() / candidateIps.size).coerceIn(0.1f, 0.95f)
        }
    }

    private fun checkDeviceAtIp(ip: String): TvDevice? {
        // 1. Check Port 5555 (ADB Wireless Debugging)
        val adbLatency = testPort(ip, 5555, timeoutMs = 180)
        if (adbLatency >= 0) {
            val realName = queryCastInfo(ip)?.first ?: "Google TV ($ip)"
            val realModel = queryCastInfo(ip)?.second ?: "Google TV (ADB: 5555)"
            return TvDevice(
                id = "lan_$ip",
                name = realName,
                ipAddress = ip,
                port = 5555,
                model = realModel,
                isOnline = true,
                latencyMs = adbLatency,
                isSimulated = false
            )
        }

        // 2. Check Port 8008 (Google Cast / DIAL REST API)
        val castLatency = testPort(ip, 8008, timeoutMs = 150)
        if (castLatency >= 0) {
            val castInfo = queryCastInfo(ip)
            val devName = castInfo?.first ?: "Google TV ($ip)"
            val devModel = castInfo?.second ?: "Chromecast / Google TV"
            return TvDevice(
                id = "lan_$ip",
                name = devName,
                ipAddress = ip,
                port = 5555,
                model = devModel,
                isOnline = true,
                latencyMs = castLatency,
                isSimulated = false
            )
        }

        // 3. Check Port 6466 (Google TV Remote v2 Service)
        val remoteLatency = testPort(ip, 6466, timeoutMs = 150)
        if (remoteLatency >= 0) {
            return TvDevice(
                id = "lan_$ip",
                name = "Google TV ($ip)",
                ipAddress = ip,
                port = 5555,
                model = "Google TV Remote v2",
                isOnline = true,
                latencyMs = remoteLatency,
                isSimulated = false
            )
        }

        return null
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

    private fun queryCastInfo(ip: String): Pair<String, String>? {
        return try {
            val url = URL("http://$ip:8008/setup/eureka_info")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 800
            conn.readTimeout = 800
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

    /**
     * Determines real local IPv4 address and /24 subnet prefix using NetworkInterface.
     * Works on all Android versions without requiring location permission.
     */
    private fun getLocalIpAndSubnet(): Pair<String, String> {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                for (intf in Collections.list(interfaces)) {
                    if (!intf.isUp || intf.isLoopback) continue
                    for (addr in Collections.list(intf.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress ?: continue
                            if (host.startsWith("127.")) continue
                            val parts = host.split(".")
                            if (parts.size == 4) {
                                val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                                return Pair(host, prefix)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error enumerating network interfaces: ${e.message}")
        }
        return Pair("192.168.1.1", "192.168.1")
    }

    private fun addOrUpdateDevice(newDevice: TvDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst { it.ipAddress == newDevice.ipAddress }
        if (index >= 0) {
            current[index] = newDevice
        } else {
            current.add(0, newDevice)
        }
        _discoveredDevices.value = current
    }

    fun addManualDevice(name: String, ipAddress: String, port: Int = 5555): TvDevice {
        val cleanIp = ipAddress.trim()
        val device = TvDevice(
            id = "manual_$cleanIp",
            name = name.ifBlank { "Google TV ($cleanIp)" },
            ipAddress = cleanIp,
            port = port,
            model = "Google TV (اتصال دستی)",
            isOnline = true,
            latencyMs = 12L,
            isFavorite = true,
            isSimulated = false
        )
        addOrUpdateDevice(device)
        return device
    }
}
