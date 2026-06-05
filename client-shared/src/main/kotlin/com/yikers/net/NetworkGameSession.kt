package com.yikers.net

import com.yikers.net.wire.Framing
import com.yikers.net.wire.Input
import com.yikers.net.wire.Snapshot
import com.yikers.net.wire.Wire
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

// Client GameSession over a socket. Server owns the clock: step() is a no-op,
// snapshot() returns the reader thread's last frame.
class NetworkGameSession(
    private val socket: Socket,
    private val input: DataInputStream,
    private val output: DataOutputStream,
    override val playerId: Int,
    val config: SessionConfig,
) : GameSession {
    private val latest = AtomicReference(EMPTY)
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
                Framing.writeFrame(output, Wire.encode(Input(cmd.copy(playerId = playerId))))
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
        private val EMPTY = WorldSnapshot(
            tick = 0L,
            entities = emptyList(),
            platforms = emptyList(),
            score = 0,
            dead = false,
            scrollY = 0f,
            highScore = 0,
        )
    }
}
