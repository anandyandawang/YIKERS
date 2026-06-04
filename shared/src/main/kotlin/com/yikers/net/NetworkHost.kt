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

// GameHost over a socket. The remote DedicatedServer already opened the room, so this
// is join-only: open/list/close are thin. join() connects, does the Join/Welcome
// handshake, and returns a NetworkGameSession bound to the assigned player slot.
class NetworkHost(
    private val host: String,
    private val port: Int,
) : GameHost {
    // The server owns the room; the client never opens one. Returned id is a sentinel
    // PlayScreen passes straight back to join().
    override fun open(cfg: SessionConfig): RoomId = REMOTE

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
            // Any handshake failure (connect refused, EOF, rejection) must not leak the
            // socket. NetworkGameSession owns it only on the success path above.
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
