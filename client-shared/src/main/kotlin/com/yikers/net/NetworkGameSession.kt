package com.yikers.net

import com.yikers.net.wire.Framing
import com.yikers.net.wire.Input
import com.yikers.net.wire.Join
import com.yikers.net.wire.Rejected
import com.yikers.net.wire.Snapshot
import com.yikers.net.wire.Welcome
import com.yikers.net.wire.Wire
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

// GameSession over a socket. Server owns the clock: snapshot() returns the reader
// thread's last frame. Built via connect(), which runs the Join/Welcome handshake.
class NetworkGameSession private constructor(
    private val socket: Socket,
    private val input: DataInputStream,
    private val output: DataOutputStream,
    override val slot: Int,
    override val config: SessionConfig,
) : GameSession {
    private val latest = AtomicReference(WorldSnapshot.EMPTY)
    private val writeLock = Any()

    @Volatile
    private var alive = true

    private val reader = thread(name = "yikers-net-reader", isDaemon = true) {
        try {
            while (alive) {
                val bytes = Framing.readFrame(input) ?: break
                val env = Wire.decode(bytes)
                if (env is Snapshot) latest.set(env.world)
            }
        } catch (_: Exception) {
            // server gone -> stop updating; last snapshot stays put
        } finally {
            alive = false
        }
    }

    override fun submitInput(cmd: InputCommand) {
        if (!alive) return
        try {
            synchronized(writeLock) {
                Framing.writeFrame(output, Wire.encode(Input(cmd.copy(slot = slot))))
            }
        } catch (_: IOException) {
            alive = false
        }
    }

    override fun snapshot(): WorldSnapshot = latest.get()

    override fun close() {
        alive = false
        runCatching { socket.close() }
        runCatching { reader.join(500) }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 4000

        // Dial the server and run the Join/Welcome handshake; the Welcome binds the
        // seat + run config for the session's whole life.
        fun connect(host: String, port: Int): NetworkGameSession {
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
                    is Welcome -> NetworkGameSession(socket, input, output, reply.slot, reply.config)
                    is Rejected -> error("server rejected connection: ${reply.reason}")
                    else -> error("unexpected handshake reply: ${reply::class.simpleName}")
                }
            } catch (e: Throwable) {
                // Any handshake failure must not leak the socket.
                runCatching { socket.close() }
                throw e
            }
        }
    }
}
