package com.yikers.net

import com.yikers.net.wire.AugmentOffer
import com.yikers.net.wire.AugmentPick
import com.yikers.net.wire.Framing
import com.yikers.net.wire.Input
import com.yikers.net.wire.ResumePlay
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
    override val slot: Int,
    val config: SessionConfig,
) : GameSession {
    private val latest = AtomicReference(EMPTY)
    private val offer = AtomicReference<AugmentOffer?>(null)
    private val writeLock = Any()

    @Volatile
    private var alive = true

    // True after we pick but before the room resumes: shows "waiting for others".
    @Volatile
    private var waiting = false

    private val reader = thread(name = "yikers-net-reader", isDaemon = true) {
        try {
            while (alive) {
                val bytes = Framing.readFrame(input) ?: break
                when (val env = Wire.decode(bytes)) {
                    is Snapshot -> latest.set(env.world)
                    is AugmentOffer -> { offer.set(env); waiting = false }
                    is ResumePlay -> { offer.set(null); waiting = false }
                    else -> {}
                }
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

    override fun submitAugmentPick(pick: AugmentPick) {
        if (!alive) return
        try {
            synchronized(writeLock) {
                Framing.writeFrame(output, Wire.encode(pick))
            }
            offer.set(null)   // picked: drop the overlay, wait for the room to resume
            waiting = true
        } catch (_: IOException) {
            alive = false
        }
    }

    override fun snapshot(): WorldSnapshot = latest.get()

    override fun augmentOffer(): AugmentOffer? = offer.get()

    override fun awaitingAugmentResume(): Boolean = waiting

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
