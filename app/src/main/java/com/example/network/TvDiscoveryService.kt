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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
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

    // Known priority IPs (e.g. 192.168.1.101) to always probe immediately
    private val priorityIps = mutableSetOf("192.168.1.101")

    fun registerPriorityIp(ip: String) {
        if (ip.isNotBlank()) {
            priorityIps.add(ip.trim())
        }
    }

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanProgress.value = 0.05f

        acquireMulticastLock()

        scanJob?.cancel()
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Instant direct probe for known / priority IPs (192.168.1.101, etc.)
                probePriorityDevices()
                _scanProgress.value = 0.15f

                // 2. Start mDNS discovery via NsdManager for Google Cast / Android TV
                startNsd()

                // 3. Send SSDP M-SEARCH broadcast packet for Smart TVs & DIAL
                sendSsdpDiscovery()
                _scanProgress.value = 0.25f

                // 4. Detect Wi-Fi local network interface IPv4 subnets
                val subnets = getLocalSubnets()
                Log.d(TAG, "Subnets to scan: ${subnets.map { it.second }}")

                for ((index, subnetPair) in subnets.withIndex()) {
                    val (localIp, prefix) = subnetPair
                    _currentSubnet.value = "$prefix.*"
                    scanSubnet(prefix, baseProgress = 0.25f + index * 0.35f, maxProgress = 0.25f + (index + 1) * 0.35f)
                }
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

    private suspend fun probePriorityDevices() = withContext(Dispatchers.IO) {
        for (ip in priorityIps) {
            val diag = TvDiagnosticHelper.diagnoseDevice(ip)
            if (diag.isReachable) {
                addOrUpdateDevice(
                    TvDevice(
                        id = "lan_$ip",
                        name = diag.tvName,
                        ipAddress = ip,
                        port = 5555,
                        model = diag.tvModel,
                        isOnline = true,
                        latencyMs = if (diag.latencyMs > 0) diag.latencyMs else 15L,
                        isAdbOpen = diag.isAdbOpen,
                        isCastOpen = diag.isCastOpen,
                        isRemoteV2Open = diag.isRemoteV2Open,
                        isSaved = true
                    )
                )
            }
        }
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
                            isCastOpen = true,
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

    /**
     * Sends SSDP M-SEARCH broadcast over UDP to 239.255.255.250:1900.
     * Google Cast, Smart TVs, DIAL servers immediately reply with their location and IP.
     */
    private suspend fun sendSsdpDiscovery() = withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket()
            socket.soTimeout = 1500
            socket.broadcast = true

            val mSearch = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: urn:dial-multiscreen-org:service:dial:1\r\n" +
                    "\r\n"

            val sendData = mSearch.toByteArray()
            val packet = DatagramPacket(
                sendData,
                sendData.size,
                InetAddress.getByName("239.255.255.250"),
                1900
            )
            socket.send(packet)

            val recvBuf = ByteArray(2048)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 1500) {
                try {
                    socket.receive(recvPacket)
                    val ip = recvPacket.address.hostAddress ?: continue
                    val response = String(recvPacket.data, 0, recvPacket.length)
                    if (response.contains("200 OK", ignoreCase = true)) {
                        val diag = TvDiagnosticHelper.diagnoseDevice(ip)
                        addOrUpdateDevice(
                            TvDevice(
                                id = "ssdp_$ip",
                                name = diag.tvName,
                                ipAddress = ip,
                                port = 5555,
                                model = diag.tvModel,
                                isOnline = true,
                                latencyMs = 10L,
                                isAdbOpen = diag.isAdbOpen,
                                isCastOpen = diag.isCastOpen
                            )
                        )
                    }
                } catch (e: Exception) {
                    break
                }
            }
            socket.close()
        } catch (e: Exception) {
            Log.d(TAG, "SSDP discovery completed or skipped: ${e.message}")
        }
    }

    private suspend fun scanSubnet(subnetPrefix: String, baseProgress: Float, maxProgress: Float) = withContext(Dispatchers.IO) {
        val candidateIps = (1..254).map { "$subnetPrefix.$it" }
        val batchSize = 16 // gentler batch size to avoid Wi-Fi router packet congestion

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
            val fraction = i.toFloat() / candidateIps.size
            _scanProgress.value = (baseProgress + fraction * (maxProgress - baseProgress)).coerceIn(0.1f, 0.98f)
        }
    }

    private fun checkDeviceAtIp(ip: String): TvDevice? {
        // 1. Check Port 5555 (ADB Wireless Debugging) - timeout 350ms
        val adbLatency = testPort(ip, 5555, timeoutMs = 350)

        // 2. Check Port 8008 (Google Cast / DIAL REST API) - timeout 350ms
        val castLatency = testPort(ip, 8008, timeoutMs = 350)

        // 3. Check Port 6466 (Google TV Remote v2 Service) - timeout 350ms
        val remoteLatency = testPort(ip, 6466, timeoutMs = 350)

        if (adbLatency < 0 && castLatency < 0 && remoteLatency < 0) {
            return null
        }

        val bestLatency = listOf(adbLatency, castLatency, remoteLatency).filter { it >= 0 }.minOrNull() ?: 15L
        val diag = TvDiagnosticHelper.queryCastInfoOrFallback(ip, adbLatency >= 0, castLatency >= 0, remoteLatency >= 0)

        return TvDevice(
            id = "lan_$ip",
            name = diag.first,
            ipAddress = ip,
            port = 5555,
            model = diag.second,
            isOnline = true,
            latencyMs = bestLatency,
            isAdbOpen = adbLatency >= 0,
            isCastOpen = castLatency >= 0,
            isRemoteV2Open = remoteLatency >= 0,
            isSimulated = false
        )
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

    /**
     * Determines real local IPv4 subnets, prioritizing Wi-Fi over Cellular (4G/5G).
     * Always ensures 192.168.1 is included.
     */
    private fun getLocalSubnets(): List<Pair<String, String>> {
        val subnets = mutableListOf<Pair<String, String>>()
        try {
            // Check WifiManager first
            val wifiIpInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (wifiIpInt != 0) {
                val wifiIp = String.format(
                    java.util.Locale.US,
                    "%d.%d.%d.%d",
                    wifiIpInt and 0xff,
                    (wifiIpInt shr 8) and 0xff,
                    (wifiIpInt shr 16) and 0xff,
                    (wifiIpInt shr 24) and 0xff
                )
                val parts = wifiIp.split(".")
                if (parts.size == 4 && parts[0] != "0") {
                    subnets.add(Pair(wifiIp, "${parts[0]}.${parts[1]}.${parts[2]}"))
                }
            }

            // Scan NetworkInterfaces prioritizing wlan / eth
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                val list = Collections.list(interfaces).sortedByDescending { intf ->
                    val name = intf.name.lowercase()
                    when {
                        name.startsWith("wlan") -> 100
                        name.startsWith("eth") -> 90
                        name.startsWith("wifi") -> 80
                        name.startsWith("tiwlan") -> 70
                        name.startsWith("rmnet") -> -10
                        name.startsWith("ccmni") -> -10
                        name.startsWith("dummy") -> -20
                        name.startsWith("p2p") -> -20
                        else -> 0
                    }
                }

                for (intf in list) {
                    if (!intf.isUp || intf.isLoopback) continue
                    val intfName = intf.name.lowercase()
                    if (intfName.startsWith("rmnet") || intfName.startsWith("ccmni") || intfName.startsWith("dummy")) continue

                    for (addr in Collections.list(intf.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress ?: continue
                            if (host.startsWith("127.")) continue
                            val parts = host.split(".")
                            if (parts.size == 4) {
                                val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                                if (!subnets.any { it.second == prefix }) {
                                    subnets.add(Pair(host, prefix))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error enumerating network interfaces: ${e.message}")
        }

        // Guarantee 192.168.1 is always scanned
        if (!subnets.any { it.second == "192.168.1" }) {
            subnets.add(Pair("192.168.1.1", "192.168.1"))
        }

        return subnets
    }

    fun addOrUpdateDevice(newDevice: TvDevice) {
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
            isSaved = true,
            isSimulated = false
        )
        addOrUpdateDevice(device)
        return device
    }
}
