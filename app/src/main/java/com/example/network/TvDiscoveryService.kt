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
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class TvDiscoveryService(private val context: Context) {

    private val TAG = "TvDiscoveryService"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private var scanJob: Job? = null
    private var nsdListener: NsdManager.DiscoveryListener? = null

    // Pre-seeded standard TV presets for instant discovery / testing
    private val defaultDevices = listOf(
        TvDevice(
            id = "sim_living_room",
            name = "تلویزیون گوگل تی‌وی پذیرایی",
            ipAddress = "192.168.1.105",
            port = 5555,
            model = "Sony BRAVIA 4K (Google TV)",
            isOnline = true,
            latencyMs = 8L,
            isFavorite = true,
            isSimulated = true
        ),
        TvDevice(
            id = "sim_bedroom",
            name = "کروم‌کست اتاق خواب",
            ipAddress = "192.168.1.120",
            port = 5555,
            model = "Chromecast with Google TV (4K)",
            isOnline = true,
            latencyMs = 15L,
            isFavorite = false,
            isSimulated = true
        ),
        TvDevice(
            id = "sim_xiaomi",
            name = "شیائومی تی‌وی باکس",
            ipAddress = "192.168.1.145",
            port = 5555,
            model = "Xiaomi TV Box S (2nd Gen)",
            isOnline = true,
            latencyMs = 19L,
            isFavorite = false,
            isSimulated = true
        )
    )

    init {
        _discoveredDevices.value = defaultDevices
    }

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanProgress.value = 0.05f

        scanJob?.cancel()
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            // 1. Start mDNS discovery via NsdManager
            startNsd()

            // 2. Scan local WiFi subnet for ADB port 5555 / Cast port 8008 / Google TV remote port 6466
            val subnetPrefix = getLocalSubnetPrefix()
            scanSubnet(subnetPrefix)

            _isScanning.value = false
            _scanProgress.value = 1f
        }
    }

    fun stopDiscovery() {
        scanJob?.cancel()
        stopNsd()
        _isScanning.value = false
    }

    private fun startNsd() {
        try {
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    Log.e(TAG, "Discovery start failed: $errorCode")
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
            // Look for Google Cast / Android TV services
            nsdManager?.discoverServices("_googlecast._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.w(TAG, "NSD error: ${e.message}")
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        try {
            nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}

                override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                    resolvedInfo?.host?.hostAddress?.let { ip ->
                        val devName = resolvedInfo.serviceName ?: "Google TV ($ip)"
                        addOrUpdateDevice(
                            TvDevice(
                                id = "nsd_$ip",
                                name = devName,
                                ipAddress = ip,
                                port = 5555,
                                model = "Android TV / Google TV",
                                isOnline = true,
                                latencyMs = 10L,
                                isSimulated = false
                            )
                        )
                    }
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
            } catch (e: Exception) {
                // Ignore
            }
        }
        nsdListener = null
    }

    private suspend fun scanSubnet(subnetPrefix: String) = withContext(Dispatchers.IO) {
        val candidateIps = (1..254).map { "$subnetPrefix.$it" }
        val batchSize = 35

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
        // Ports: 5555 (ADB Wireless), 6466 (Google TV pairing), 8008 (Google Cast)
        val portsToCheck = listOf(5555, 6466, 8008)
        for (port in portsToCheck) {
            try {
                val socket = Socket()
                val startTime = System.currentTimeMillis()
                socket.connect(InetSocketAddress(ip, port), 120)
                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                socket.close()

                return TvDevice(
                    id = "lan_$ip",
                    name = "تلویزیون در $ip",
                    ipAddress = ip,
                    port = 5555,
                    model = if (port == 5555) "Google TV (ADB فعال)" else "Google TV / Cast",
                    isOnline = true,
                    latencyMs = latency,
                    isSimulated = false
                )
            } catch (ignored: Exception) {
                // Not responding on this port
            }
        }
        return null
    }

    private fun getLocalSubnetPrefix(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                val ip = String.format(
                    "%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff
                )
                return ip
            }
        } catch (e: Exception) {
            // Fallback
        }
        return "192.168.1"
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
        val device = TvDevice(
            id = "manual_$ipAddress",
            name = name.ifBlank { "Google TV ($ipAddress)" },
            ipAddress = ipAddress.trim(),
            port = port,
            model = "گوگل تی‌وی دستی",
            isOnline = true,
            latencyMs = 12L,
            isFavorite = true,
            isSimulated = false
        )
        addOrUpdateDevice(device)
        return device
    }
}
