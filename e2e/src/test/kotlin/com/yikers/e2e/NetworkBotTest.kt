package com.yikers.e2e

import com.yikers.bot.app.BotRunner
import com.yikers.net.DedicatedServer
import com.yikers.net.SessionConfig
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@HeadlessGdx
class NetworkBotTest {

    @Test
    fun botClientClimbsOverTheSocket() {
        val server = DedicatedServer(name = "test", tcpPort = 0, cfg = SessionConfig(seed = SEED))
        server.start()

        val bots = BotRunner("127.0.0.1", server.port, count = 1)
        bots.start()
        try {
            val climbed = awaitScore(server, target = 1, timeoutMs = 12_000)
            assertTrue(climbed) {
                "a socket-connected bot must climb + score; score=${server.latestSnapshot?.score}"
            }
        } finally {
            bots.stop()
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
