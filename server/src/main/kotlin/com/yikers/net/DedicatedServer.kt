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

// The LAN server: wraps the embedded GameInstance and serves ONE room over a
// socket. It owns the clock — a 60Hz tick thread drains queued client input, steps
// the sim, snapshots once, and broadcasts to every client. An acceptor thread
// handshakes new clients, assigning each a dynamic player slot (despawned on
// disconnect); a UDP responder answers LAN discovery. It knows NOTHING about who or
// what connects: a client is a socket that sends InputCommands and reads
// WorldSnapshots — human or bot is meaningless here. Bots, when wanted, are launched
// elsewhere (BotRunner) and connect over this same socket like anyone else.
class DedicatedServer(
    val name: String,
    tcpPort: Int,
    private val cfg: SessionConfig,
    private val maxPlayers: Int = DEFAULT_MAX_PLAYERS,
) {
    // The authoritative session — the same code singleplayer runs in-process.
    private val host = LocalHost()
    private val room = host.open(cfg)
    private val instance = host.instance(room)

    private val serverSocket = ServerSocket(tcpPort) // port 0 => OS-assigned ephemeral

    // Connected socket clients. Guarded by `conns` itself for iteration.
    private val conns = ArrayList<ClientConn>()

    // Inbound input is funnelled here from reader threads and drained on the tick
    // thread, so all sim mutation stays single-threaded.
    private val inbound = ConcurrentLinkedQueue<InputCommand>()

    // In-process clients pumped each tick on the authoritative thread. Opaque
    // callbacks: the server never inspects what's behind them (human, bot, recorder)
    // — it only ever sees the InputCommands they submit through their session.
    private val localPumps = java.util.concurrent.CopyOnWriteArrayList<(Float) -> Unit>()

    @Volatile
    private var running = false

    private var acceptor: Thread? = null
    private var ticker: Thread? = null
    private var responder: DiscoveryResponder? = null

    // The resolved TCP port (meaningful after construction; ephemeral when 0 passed).
    val port: Int get() = serverSocket.localPort

    val playerCount: Int get() = instance.players

    // The most recent broadcast frame (immutable), published by the tick thread for
    // safe cross-thread reads (monitoring / tests) — never touch the live world off
    // the tick thread. Null until the first tick.
    @Volatile
    var latestSnapshot: WorldSnapshot? = null
        private set

    // An in-process client handle: its own player slot on this server, reached with
    // NO socket. The caller wraps it (e.g. a Participant with some InputAgent) and
    // registers its per-tick pump via addLocalPump. The server stays oblivious to
    // what the client is — it only ever sees the InputCommands it submits.
    fun localSession(): GameSession = LocalGameSession(instance, instance.addPlayer())

    // Register a callback pumped once per tick on the authoritative thread, so its
    // input lands in the very next step (~1 tick of lag, no socket round-trip). The
    // server never inspects it.
    fun addLocalPump(pump: (Float) -> Unit) {
        localPumps.add(pump)
    }

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

    // Read the client's Join, assign a dynamic player slot (or Reject if the room is
    // full), reply Welcome, then register the connection for input + broadcast.
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
            // Drain all queued input first (jump is latched server-side, so applying
            // several commands before one step never drops an edge press).
            while (true) {
                val cmd = inbound.poll() ?: break
                instance.applyInput(cmd)
            }
            // In-process clients decide + submit now, so their input lands this step
            // (~1 tick of lag) instead of a socket round-trip's worth.
            localPumps.forEach { it(DT) }
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
        // Closing serverSocket wakes accept() with an exception; wait for both worker
        // threads to actually exit before tearing the room down.
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
