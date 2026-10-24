package it.sorguido.chromecastkillswitch.transport

import java.net.InetAddress

enum class EndpointKind { PAIRING, CONNECT }

data class Endpoint(
    val host: String,
    val port: Int,
    val serviceName: String = "",
    val kind: EndpointKind = EndpointKind.CONNECT,
) {
    init {
        require(port in 1..65535) { "La porta deve essere tra 1 e 65535." }
        require(validAddress(host)) { "Inserisci un indirizzo IP valido, senza porta." }
    }

    val address: String get() = if (':' in host) "[$host]:$port" else "$host:$port"

    companion object {
        fun parse(host: String, port: String, kind: EndpointKind = EndpointKind.CONNECT): Endpoint =
            Endpoint(host.trim().removeSurrounding("[", "]"),
                port.trim().toIntOrNull() ?: throw IllegalArgumentException("Inserisci una porta numerica."), kind = kind)

        private fun validAddress(host: String): Boolean {
            if (host.isEmpty() || host.length > 128) return false
            if (':' in host) {
                if (!host.matches(Regex("[0-9a-fA-F:.]+(%[a-zA-Z0-9_.-]+)?"))) return false
                return runCatching { InetAddress.getByName(host).address.size == 16 }.getOrDefault(false)
            }
            val parts = host.split('.')
            return parts.size == 4 && parts.all {
                it.matches(Regex("0|[1-9][0-9]{0,2}")) && (it.toIntOrNull() ?: 256) <= 255
            } && host != "0.0.0.0" && host != "255.255.255.255"
        }
    }
}
