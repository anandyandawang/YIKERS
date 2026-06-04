package com.yikers.integration

import com.yikers.bot.BotAgent
import com.yikers.config.RunConfig
import com.yikers.net.DedicatedServer
import com.yikers.net.Participant
import com.yikers.net.SessionConfig
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// In-process bots: a bot client lives in the server's own process and is pumped on
// the authoritative tick thread (~1 tick of lag, no socket round-trip). The server
// stays bot-blind: it just hands out a generic localSession() slot and pumps an
// opaque callback. Score is read from the server's published latest snapshot (no
// extra observer client, which would spawn a second ball in the lone bot's path).
@HeadlessGdx
class InProcessBotTest {

    @Test
    fun inProcessBotClimbsOnTheServer() {
        val server = DedicatedServer(name = "test", tcpPort = 0, cfg = SessionConfig(seed = SEED))
        val bot = Participant(server.localSession(), BotAgent(RunConfig()))
        server.addLocalPump(bot::pump)
        server.start()
        try {
            val climbed = awaitScore(server, target = 1, timeoutMs = 10_000)
            assertTrue(climbed) {
                "an in-process bot must climb + score; score=${server.latestSnapshot?.score}"
            }
        } finally {
            server.stop()
        }
    }

    private fun awaitScore(server: DedicatedServer, target: Int, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if ((server.latestSnapshot?.score ?: 0) >= target) return true
            Thread.sleep(50)
        }
        return (server.latestSnapshot?.score ?: 0) >= target
    }

    companion object {
        private const val SEED = 42L
    }
}
