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

// The LAN server: wraps the existing in-process LocalHost / GameInstance and serves
// ONE room over a socket. It owns the clock — a 60Hz tick thread drains queued
// client input, steps the sim, snapshots once, and broadcasts to every client. An
// acceptor thread handshakes new clients (assigning a player slot), and a UDP
// responder answers LAN discovery. No sim logic is duplicated; only transport.
class DedicatedServer(
    val name: String,
    tcpPort: Int,
    private val cfg: SessionConfig,
) {
    // The authoritative session — the same code singleplayer runs in-process.
    private val host = LocalHost()
    private val room = host.open(cfg)
    private val session = host.join(room)

    private val serverSocket = ServerSocket(tcpPort) // port 0 => OS-assigned ephemeral
    private val maxPlayers = cfg.humans.coerceAtLeast(1)

    // Connected clients. Guarded by `conns` itself for slot assignment + iteration.
    private val conns = ArrayList<ClientConn>()

    // Inbound input is funnelled here from reader threads and drained on the tick
    // thread, so all sim mutation stays single-threaded.
    private val inbound = ConcurrentLinkedQueue<InputCommand>()

    @Volatile
    private var running = false

    private var acceptor: Thread? = null
    private var ticker: Thread? = null
    private var responder: DiscoveryResponder? = null

    // The resolved TCP port (meaningful after construction; ephemeral when 0 passed).
    val port: Int get() = serverSocket.localPort

    val playerCount: Int get() = synchronized(conns) { conns.size }

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

    // Read the client's Join, assign the next free slot, reply Welcome (or Rejected
    // if the room is full), then register the connection for input + broadcast.
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

            val pid = synchronized(conns) {
                val used = conns.map { it.playerId }.toSet()
                (0 until maxPlayers).firstOrNull { it !in used }
            }
            if (pid == null) {
                Framing.writeFrame(output, Wire.encode(Rejected("server full")))
                socket.close()
                return
            }

            Framing.writeFrame(output, Wire.encode(Welcome(pid, cfg)))
            val conn = ClientConn(pid, socket, input, output)
            synchronized(conns) { conns.add(conn) }
            conn.start(
                onInput = { inbound.add(it) },
                onClose = { c -> synchronized(conns) { conns.remove(c) } },
            )
        } catch (_: Exception) {
            runCatching { socket.close() }
        }
    }

    private fun runTickLoop() {
        val stepNanos = 1_000_000_000L / TICK_HZ
        var next = System.nanoTime()
        while (running) {
            // Drain all queued input first (jump is latched server-side, so applying
            // several commands before one step never drops an edge press).
            while (true) {
                val cmd = inbound.poll() ?: break
                session.submitInput(cmd)
            }
            session.step(DT)
            val snap = session.snapshot()
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
        // Closing serverSocket wakes accept() with an exception; wait for both worker
        // threads to actually exit before tearing the room down.
        acceptor?.let { runCatching { it.join(500) } }
        ticker?.let { runCatching { it.join(500) } }
        host.close(room)
    }

    companion object {
        const val TICK_HZ = 60
        const val DT = 1f / TICK_HZ
    }
}
