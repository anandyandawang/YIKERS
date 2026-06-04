package com.yikers.net

import com.yikers.net.wire.Envelope
import com.yikers.net.wire.Input
import com.yikers.net.wire.Framing
import com.yikers.net.wire.Wire
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import kotlin.concurrent.thread

// One connected client on the server side: its socket, the player slot it owns, and
// a reader thread that pulls Input frames off the wire. Inbound commands are handed
// to [onInput] (the server queues them for the tick thread — never touches the sim
// here). Writes go through send() under a per-connection lock so the tick thread's
// Snapshot broadcast can't interleave bytes with anything else.
class ClientConn(
    val playerId: Int,
    private val socket: Socket,
    private val input: DataInputStream,
    private val output: DataOutputStream,
) {
    private val writeLock = Any()

    @Volatile
    private var alive = true

    private var reader: Thread? = null

    // Start pulling frames. onInput gets each Input's command (already stamped with
    // this connection's playerId by the server). onClose fires once on disconnect.
    fun start(onInput: (InputCommand) -> Unit, onClose: (ClientConn) -> Unit) {
        reader = thread(name = "yikers-conn-$playerId", isDaemon = true) {
            try {
                while (alive) {
                    val bytes = Framing.readFrame(input) ?: break // clean EOF
                    val env = Wire.decode(bytes)
                    if (env is Input) onInput(env.cmd.copy(playerId = playerId))
                }
            } catch (_: Exception) {
                // peer gone / socket reset -> treat as disconnect
            } finally {
                alive = false
                runCatching { socket.close() }
                onClose(this)
            }
        }
    }

    // Send one frame. Swallows write errors as a disconnect so one dead client can't
    // crash the broadcast loop; the reader thread will observe the close and clean up.
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
