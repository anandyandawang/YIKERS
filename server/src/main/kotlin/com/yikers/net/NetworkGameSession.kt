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

// Client-side GameSession over a socket. Mirror image of LocalGameSession, but the
// server owns the clock: step() is a no-op, snapshot() returns the LAST frame the
// reader thread received (never touches a sim), and submitInput() ships one Input
// frame. So PlayScreen's render loop is identical to singleplayer — only the host
// behind the seam changed.
class NetworkGameSession(
    private val socket: Socket,
    private val input: DataInputStream,
    private val output: DataOutputStream,
    val playerId: Int,
    val config: SessionConfig,
) : GameSession {
    // Latest authoritative frame; benign empty snapshot until the first arrives.
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
            // server gone / socket reset -> stop updating; last snapshot stays put
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

    // Server-authoritative: the client never advances the sim.
    override fun step(deltaTime: Float) = Unit

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
