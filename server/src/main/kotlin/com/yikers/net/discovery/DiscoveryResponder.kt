package com.yikers.net.discovery

import com.yikers.net.wire.ServerAd
import com.yikers.net.wire.Wire
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

// Server-side LAN discovery: binds the well-known UDP port and, for each valid query
// datagram, unicasts a ServerAd back to the sender. Runs on its own daemon thread.
// Best-effort — if the port is taken or UDP is blocked, discovery simply goes dark
// and clients fall back to a direct host:port entry.
class DiscoveryResponder(
    private val name: String,
    private val tcpPort: Int,
    private val maxPlayers: Int,
    private val players: () -> Int,
) {
    private var socket: DatagramSocket? = null

    @Volatile
    private var running = false

    private var worker: Thread? = null

    fun start() {
        val sock = try {
            DatagramSocket(DISCOVERY_PORT).apply { broadcast = true }
        } catch (_: Exception) {
            return // port busy / no UDP -> discovery disabled, server still joinable
        }
        socket = sock
        running = true
        worker = thread(name = "yikers-discovery", isDaemon = true) {
            val buf = ByteArray(256)
            while (running) {
                val packet = DatagramPacket(buf, buf.size)
                try {
                    sock.receive(packet)
                } catch (_: Exception) {
                    break // socket closed by stop()
                }
                if (!isDiscoveryQuery(packet.data, packet.length)) continue
                val ad = ServerAd(name, tcpPort, players(), maxPlayers)
                val reply = Wire.encodeAd(ad)
                runCatching {
                    sock.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
                }
            }
        }
    }

    fun stop() {
        running = false
        runCatching { socket?.close() }
    }
}
