package com.yikers.net

import com.yikers.net.discovery.DiscoveryResponder
import com.yikers.net.wire.Framing
import com.yikers.net.wire.Join
import com.yikers.net.wire.Rejected
import com.yikers.net.wire.Snapshot
import com.yikers.net.wire.Welcome
import com.yikers.net.wire.Wire
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.LockSupport
import kotlin.concurrent.thread

// LAN server: wraps the GameInstance, serves ONE room over a socket. Owns the clock
// (60Hz tick thread); an acceptor handshakes clients; a UDP responder answers discovery.
class DedicatedServer(
    val name: String,
    tcpPort: Int,
    private val cfg: SessionConfig,
    private val maxPlayers: Int = DEFAULT_MAX_PLAYERS,
) {
    private val host = LocalHost()
    private val room = host.open(cfg)
    private val instance = host.instance(room)

    private val serverSocket = ServerSocket(tcpPort) // port 0 => OS-assigned ephemeral

    // Guarded by `conns` itself for iteration.
    private val conns = ArrayList<ClientConn>()

    // Drained on the tick thread so all sim mutation stays single-threaded.
    private val inbound = ConcurrentLinkedQueue<InputCommand>()

    @Volatile
    private var running = false

    private var acceptor: Thread? = null
    private var ticker: Thread? = null
    private var responder: DiscoveryResponder? = null

    val port: Int get() = serverSocket.localPort // ephemeral when 0 passed

    val playerCount: Int get() = instance.players

    // Latest broadcast frame, published for cross-thread reads (monitoring/tests).
    @Volatile
    var latestSnapshot: WorldSnapshot? = null
        private set

    fun start() {
        running = true
        ticker = thread(name = "yikers-server-tick", isDaemon = true) { runTickLoop() }
        acceptor = thread(name = "yikers-server-accept", isDaemon = true) { runAcceptLoop() }
        responder = DiscoveryResponder(
            name = name,
            tcpPort = port,
            maxPlayers = maxPlayers,
            players = { playerCount },
        ).also { it.start() }
    }

    private fun runAcceptLoop() {
        while (running) {
            val socket = try {
                serverSocket.accept()
            } catch (_: Exception) {
                break // serverSocket closed by stop()
            }
            handshake(socket)
        }
    }

    private fun handshake(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            val input = DataInputStream(socket.getInputStream().buffered())
            val output = DataOutputStream(socket.getOutputStream().buffered())

            val first = Framing.readFrame(input)?.let { Wire.decode(it) }
            if (first !is Join) {
                socket.close()
                return
            }

            if (instance.players >= maxPlayers) {
                Framing.writeFrame(output, Wire.encode(Rejected("server full")))
                socket.close()
                return
            }

            val pid = instance.addPlayer()
            Framing.writeFrame(output, Wire.encode(Welcome(pid, cfg)))
            val conn = ClientConn(pid, socket, input, output)
            synchronized(conns) { conns.add(conn) }
            conn.start(
                onInput = { inbound.add(it) },
                onClose = { c ->
                    synchronized(conns) { conns.remove(c) }
                    instance.removePlayer(c.playerId)
                },
            )
        } catch (_: Exception) {
            runCatching { socket.close() }
        }
    }

    private fun runTickLoop() {
        val stepNanos = 1_000_000_000L / TICK_HZ
        var next = System.nanoTime()
        while (running) {
            // Drain all queued input first (jump latch survives many commands/step).
            while (true) {
                val cmd = inbound.poll() ?: break
                instance.applyInput(cmd)
            }
            instance.tick(DT)
            val snap = instance.snapshot()
            latestSnapshot = snap
            val targets = synchronized(conns) { conns.toList() }
            targets.forEach { it.send(Snapshot(snap)) }

            next += stepNanos
            val sleep = next - System.nanoTime()
            if (sleep > 0) LockSupport.parkNanos(sleep) else next = System.nanoTime()
        }
    }

    fun stop() {
        running = false
        responder?.stop()
        runCatching { serverSocket.close() }
        synchronized(conns) {
            conns.forEach { it.close() }
            conns.clear()
        }
        // serverSocket.close() wakes accept(); wait for both workers before teardown.
        acceptor?.let { runCatching { it.join(500) } }
        ticker?.let { runCatching { it.join(500) } }
        host.close(room)
    }

    companion object {
        const val TICK_HZ = 60
        const val DT = 1f / TICK_HZ
        const val DEFAULT_MAX_PLAYERS = 8
    }
}
