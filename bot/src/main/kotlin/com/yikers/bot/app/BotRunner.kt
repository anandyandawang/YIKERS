package com.yikers.bot.app

import com.yikers.bot.BotAgent
import com.yikers.config.GameConfig
import com.yikers.net.NetworkGameSession
import com.yikers.net.NetworkHost
import com.yikers.net.Participant
import com.yikers.net.RoomId
import java.util.concurrent.locks.LockSupport
import kotlin.concurrent.thread

// Connects N bot clients over the real socket; one daemon thread pumps them at the
// sim tick rate (server owns the clock).
class BotRunner(
    private val host: String,
    private val port: Int,
    private val count: Int,
) {
    private val participants = ArrayList<Participant>()

    @Volatile
    private var running = false
    private var pumpThread: Thread? = null

    fun start() {
        repeat(count) {
            val session = NetworkHost(host, port).join(RoomId("bot")) as NetworkGameSession
            // Use the server's own run feel, handed over in the Welcome handshake.
            participants.add(Participant(session, BotAgent(session.config.runConfig), autoSkipAugments = true))
        }
        running = true
        pumpThread = thread(name = "yikers-bot-pump", isDaemon = true) { runPumpLoop() }
    }

    private fun runPumpLoop() {
        val stepNanos = 1_000_000_000L / GameConfig.SIM_HZ
        val dt = 1f / GameConfig.SIM_HZ
        var next = System.nanoTime()
        while (running) {
            try {
                participants.forEach { it.pump(dt) }
            } catch (e: Exception) {
                System.err.println("yikers-bot-pump error: ${e.stackTraceToString()}")
            }
            next += stepNanos
            val sleep = next - System.nanoTime()
            if (sleep > 0) LockSupport.parkNanos(sleep) else next = System.nanoTime()
        }
    }

    fun stop() {
        running = false
        pumpThread?.let { runCatching { it.join(500) } }
        participants.forEach { it.close() }
        participants.clear()
    }
}
