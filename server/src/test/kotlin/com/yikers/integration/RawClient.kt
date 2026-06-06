package com.yikers.integration

import com.yikers.net.InputCommand
import com.yikers.net.WorldSnapshot
import com.yikers.net.wire.Envelope
import com.yikers.net.wire.Framing
import com.yikers.net.wire.Input
import com.yikers.net.wire.Join
import com.yikers.net.wire.Snapshot
import com.yikers.net.wire.Welcome
import com.yikers.net.wire.Wire
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import kotlin.concurrent.thread

// Black-box client: raw loopback socket + the shared wire protocol, nothing else.
// No :client / :bot code at all -> this is what any third-party peer sees of the
// DedicatedServer. Drive Input frames in, read Snapshot frames out.
class RawClient(host: String, port: Int) {
    private val socket = Socket(host, port).apply { tcpNoDelay = true }
    private val input = DataInputStream(socket.getInputStream().buffered())
    private val output = DataOutputStream(socket.getOutputStream().buffered())

    // -1 until the server assigns a slot in Welcome.
    var slot: Int = -1
        private set

    // Latest snapshot the reader thread saw; null until the first frame lands.
    @Volatile
    var latest: WorldSnapshot? = null
        private set

    @Volatile
    private var alive = true
    private var reader: Thread? = null

    // Handshake: send Join, return the server's reply (Welcome or Rejected). On
    // Welcome we record the slot and spin up a reader thread that keeps `latest`
    // current, mirroring a real client's snapshot pump.
    fun join(): Envelope {
        Framing.writeFrame(output, Wire.encode(Join()))
        val reply = readEnvelope()
        if (reply is Welcome) {
            slot = reply.slot
            startReader()
        }
        return reply
    }

    // One tick's intent. slot here is whatever we pass; the server re-stamps it to
    // our own seat, so a forged id can't drive someone else's climber.
    fun send(slot: Int = this.slot, vx: Float = 0f, jump: Boolean = false) {
        Framing.writeFrame(output, Wire.encode(Input(InputCommand(slot, vx, jump))))
    }

    fun close() {
        alive = false
        runCatching { socket.close() }
    }

    private fun startReader() {
        reader = thread(name = "raw-client-$slot", isDaemon = true) {
            try {
                while (alive) {
                    val env = readEnvelope()
                    if (env is Snapshot) latest = env.world
                }
            } catch (_: Exception) {
                // socket closed -> reader stops
            }
        }
    }

    // Only called single-threaded: on the test thread for Welcome, then solely on
    // the reader thread once it starts. Never overlaps.
    private fun readEnvelope(): Envelope {
        val bytes = Framing.readFrame(input) ?: error("server closed the socket")
        return Wire.decode(bytes)
    }
}
