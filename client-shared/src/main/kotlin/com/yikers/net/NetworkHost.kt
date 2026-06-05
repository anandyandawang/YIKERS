package com.yikers.net

import com.yikers.net.wire.Framing
import com.yikers.net.wire.Join
import com.yikers.net.wire.Rejected
import com.yikers.net.wire.Welcome
import com.yikers.net.wire.Wire
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

// GameHost over a socket. Join-only; join() does the Join/Welcome handshake.
class NetworkHost(
    private val host: String,
    private val port: Int,
) : GameHost {
    override fun open(cfg: SessionConfig): RoomId = REMOTE // sentinel; client never opens

    override fun join(room: RoomId): GameSession {
        val socket = Socket()
        try {
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            val input = DataInputStream(socket.getInputStream().buffered())
            val output = DataOutputStream(socket.getOutputStream().buffered())

            Framing.writeFrame(output, Wire.encode(Join()))
            val reply = Framing.readFrame(input)?.let { Wire.decode(it) }
                ?: error("server closed during handshake")
            return when (reply) {
                is Welcome -> NetworkGameSession(socket, input, output, reply.playerId, reply.config)
                is Rejected -> error("server rejected connection: ${reply.reason}")
                else -> error("unexpected handshake reply: ${reply::class.simpleName}")
            }
        } catch (e: Throwable) {
            // Any handshake failure must not leak the socket.
            runCatching { socket.close() }
            throw e
        }
    }

    override fun list(): List<RoomInfo> = emptyList()

    override fun close(room: RoomId) = Unit

    companion object {
        private val REMOTE = RoomId("net")
        private const val CONNECT_TIMEOUT_MS = 4000
    }
}
