package it.sorguido.chromecastkillswitch.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

/** Resolves both service types, never selects or authenticates a target. Main-thread lifecycle. */
@Suppress("DEPRECATION")
class AdbDiscovery(context: Context) {
    private val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val handler = Handler(Looper.getMainLooper())
    private val found = linkedMapOf<String, Endpoint>()
    private val queue = ArrayDeque<Pair<NsdServiceInfo, EndpointKind>>()
    private val listeners = mutableListOf<NsdManager.DiscoveryListener>()
    private val _endpoints = MutableStateFlow<List<Endpoint>>(emptyList())
    val endpoints: StateFlow<List<Endpoint>> = _endpoints.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private var multicast: WifiManager.MulticastLock? = null
    private var generation = 0
    private var running = false
    private var resolving = false
    private val lost = mutableSetOf<String>()

    fun start() {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (running) return
        running = true
        generation++
        _error.value = null
        try {
            multicast = wifi.createMulticastLock("chromecast-discovery").apply {
                setReferenceCounted(false); acquire()
            }
            discover("_adb-tls-pairing._tcp.", EndpointKind.PAIRING)
            discover("_adb-tls-connect._tcp.", EndpointKind.CONNECT)
        } catch (_: SecurityException) {
            _error.value = "Accesso alla rete locale negato. Controlla i permessi dell’app nelle impostazioni del telefono."
            stop()
        } catch (_: RuntimeException) {
            _error.value = "Ricerca non disponibile. Riprova o inserisci l’endpoint mostrato sul televisore."
            stop()
        }
    }

    private fun discover(type: String, kind: EndpointKind) {
        val epoch = generation
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                handler.post {
                    if (epoch == generation && running) {
                        _error.value = "Ricerca non riuscita. Controlla la rete locale e riprova."
                        stop()
                    }
                }
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceFound(info: NsdServiceInfo) {
                handler.post {
                    if (epoch != generation || !running) return@post
                    lost.remove(key(info, kind))
                    queue.add(info to kind)
                    resolveNext(epoch)
                }
            }
            override fun onServiceLost(info: NsdServiceInfo) {
                handler.post {
                    if (epoch != generation || !running) return@post
                    val id = key(info, kind)
                    lost.add(id)
                    found.remove(id)
                    publish()
                }
            }
        }
        listeners.add(listener)
        manager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun resolveNext(epoch: Int) {
        if (resolving || !running || epoch != generation) return
        val next = queue.poll() ?: return
        resolving = true
        try {
            manager.resolveService(next.first, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    handler.post {
                        if (epoch != generation) return@post
                        resolving = false
                        resolveNext(epoch)
                    }
                }
                override fun onServiceResolved(info: NsdServiceInfo) {
                    handler.post {
                        if (epoch != generation || !running) return@post
                        resolving = false
                        val id = key(info, next.second)
                        val host = info.host?.hostAddress
                        if (id !in lost && host != null) {
                            runCatching { Endpoint(host, info.port, info.serviceName, next.second) }
                                .onSuccess { found[id] = it }
                            publish()
                        }
                        resolveNext(epoch)
                    }
                }
            })
        } catch (_: RuntimeException) {
            resolving = false
            _error.value = "Impossibile risolvere l’endpoint. Prova il collegamento manuale."
            resolveNext(epoch)
        }
    }

    private fun key(info: NsdServiceInfo, kind: EndpointKind) = "${kind.name}:${info.serviceName}"

    private fun publish() {
        _endpoints.value = found.values.distinctBy { Triple(it.kind, it.host, it.port) }
            .sortedWith(compareBy({ it.kind.name }, { it.serviceName }, { it.host }, { it.port }))
    }

    fun stop() {
        check(Looper.myLooper() == Looper.getMainLooper())
        running = false
        generation++
        listeners.forEach { runCatching { manager.stopServiceDiscovery(it) } }
        listeners.clear()
        queue.clear(); found.clear(); lost.clear()
        resolving = false
        _endpoints.value = emptyList()
        multicast?.let { if (it.isHeld) runCatching { it.release() } }
        multicast = null
    }
}
