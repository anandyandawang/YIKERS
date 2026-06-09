package com.yikers.net

import com.yikers.net.wire.AugmentPick
import com.yikers.net.wire.Envelope
import com.yikers.net.wire.Input
import com.yikers.net.wire.Framing
import com.yikers.net.wire.Wire
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import kotlin.concurrent.thread

// One server-side client; send() takes a per-connection write lock so the tick
// thread's broadcast can't interleave bytes.
class ClientConn(
    val slot: Int,
    private val socket: Socket,
    private val input: DataInputStream,
    private val output: DataOutputStream,
) {
    private val writeLock = Any()

    @Volatile
    private var alive = true

    private var reader: Thread? = null

    fun start(
        onInput: (InputCommand) -> Unit,
        onPick: (Int, AugmentPick) -> Unit,
        onClose: (ClientConn) -> Unit,
    ) {
        reader = thread(name = "yikers-conn-$slot", isDaemon = true) {
            try {
                while (alive) {
                    val bytes = Framing.readFrame(input) ?: break // clean EOF
                    val env = Wire.decode(bytes)
                    when (env) {
                        is Input -> onInput(env.cmd.copy(slot = slot))
                        is AugmentPick -> onPick(slot, env)
                        else -> {}
                    }
                }
            } catch (_: Exception) {
                // peer gone -> disconnect
            } finally {
                alive = false
                runCatching { socket.close() }
                onClose(this)
            }
        }
    }

    // Swallows write errors as a disconnect so one dead client can't crash broadcast.
    fun send(env: Envelope) {
        if (!alive) return
        try {
            synchronized(writeLock) { Framing.writeFrame(output, Wire.encode(env)) }
        } catch (_: IOException) {
            alive = false
            runCatching { socket.close() }
        }
    }

    fun close() {
        alive = false
        runCatching { socket.close() }
    }
}
