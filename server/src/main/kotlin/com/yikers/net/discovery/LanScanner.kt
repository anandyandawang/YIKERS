package com.yikers.net.discovery

import com.yikers.net.wire.Wire
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

// Client-side LAN discovery: broadcast one query, collect ServerAd replies for a
// short window (de-duped by host:port). Blocking -> run off the render thread.
object LanScanner {
    private const val BROADCAST = "255.255.255.255"

    fun scan(timeoutMs: Int = 2000): List<DiscoveredServer> {
        val found = LinkedHashMap<String, DiscoveredServer>()
        val socket = try {
            DatagramSocket().apply {
                broadcast = true
                soTimeout = 250
            }
        } catch (_: Exception) {
            return emptyList()
        }
        try {
            val query = DatagramPacket(
                DISCOVERY_QUERY, DISCOVERY_QUERY.size,
                InetAddress.getByName(BROADCAST), DISCOVERY_PORT,
            )
            runCatching { socket.send(query) }

            val deadline = System.currentTimeMillis() + timeoutMs
            val buf = ByteArray(256)
            while (System.currentTimeMillis() < deadline) {
                val packet = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                } catch (_: Exception) {
                    break
                }
                val ad = runCatching {
                    Wire.decodeAd(packet.data.copyOf(packet.length))
                }.getOrNull() ?: continue
                val host = packet.address.hostAddress
                val server = DiscoveredServer.from(ad, host)
                found["$host:${server.port}"] = server
            }
        } finally {
            socket.close()
        }
        return found.values.toList()
    }
}
