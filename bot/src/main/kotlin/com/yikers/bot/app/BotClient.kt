package com.yikers.bot.app

import com.yikers.bot.BotAgent
import com.yikers.config.GameConfig
import com.yikers.net.NetworkGameSession
import com.yikers.net.Participant
import java.util.concurrent.locks.LockSupport
import kotlin.concurrent.thread

// One bot = one client. Connects a single autopilot over the real socket and pumps
// it at the sim tick rate (server owns the clock) on its own daemon thread. Want
// more bots? Start more clients.
class BotClient(
    private val host: String,
    private val port: Int,
) {
    private var participant: Participant? = null

    @Volatile
    private var running = false
    private var pumpThread: Thread? = null

    fun start() {
        val session = NetworkGameSession.connect(host, port)
        // Use the server's own run feel, handed over in the Welcome handshake.
        participant = Participant(session, BotAgent(session.config.runConfig))
        running = true
        pumpThread = thread(name = "yikers-bot-pump", isDaemon = true) { runPumpLoop() }
    }

    private fun runPumpLoop() {
        val stepNanos = 1_000_000_000L / GameConfig.SIM_HZ
        val dt = 1f / GameConfig.SIM_HZ
        var next = System.nanoTime()
        while (running) {
            try {
                participant?.pump(dt)
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
        participant?.close()
        participant = null
    }
}
