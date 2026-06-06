package com.yikers.net.discovery

import com.yikers.net.wire.Wire
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
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
            broadcastTargets().forEach { addr ->
                runCatching {
                    socket.send(
                        DatagramPacket(DISCOVERY_QUERY, DISCOVERY_QUERY.size, addr, DISCOVERY_PORT),
                    )
                }
            }

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

    // iOS / Darwin routinely drops the limited broadcast 255.255.255.255, so also
    // target each interface's subnet-directed broadcast (e.g. 192.168.1.255). Keep
    // the limited broadcast as a fallback for desktop / Android, which route it fine.
    private fun broadcastTargets(): List<InetAddress> {
        val targets = LinkedHashSet<InetAddress>()
        runCatching {
            for (ni in NetworkInterface.getNetworkInterfaces()) {
                if (!ni.isUp || ni.isLoopback) continue
                for (ia in ni.interfaceAddresses) {
                    ia.broadcast?.let { targets.add(it) } // IPv4 subnet broadcast; null for IPv6
                }
            }
        }
        targets.add(InetAddress.getByName(BROADCAST)) // 255.255.255.255 fallback
        return targets.toList()
    }
}
