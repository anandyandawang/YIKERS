package com.yikers.integration

import com.yikers.config.GameConfig
import com.yikers.bot.app.BotRunner
import com.yikers.net.DedicatedServer
import com.yikers.net.GameSession
import com.yikers.net.NetworkHost
import com.yikers.net.RoomId
import com.yikers.net.SessionConfig
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// The server has NO bot concept. This proves a bot reaches it as an ordinary socket
// client: boot a real DedicatedServer, point a BotRunner at its port, and watch the
// bot DRIVE its own ball over the wire. The bot connects, is handed a slot, reads
// WorldSnapshots and submits InputCommands exactly like a person — so its jump lifts
// its ball well above the spawn floor, round-tripping input -> sim -> snapshot. An
// idle observer client reads the authoritative world. (How well the bot then PLAYS
// under network latency is a separate AI concern; this is the seam.)
@HeadlessGdx
class NetworkBotTest {

    @Test
    fun botClientDrivesItsBallOverTheSocket() {
        val server = DedicatedServer(name = "test", tcpPort = 0, cfg = SessionConfig(seed = SEED))
        server.start()

        val bots = BotRunner("127.0.0.1", server.port, count = 1)
        bots.start()

        val observer = NetworkHost("127.0.0.1", server.port).join(RoomId("obs"))
        try {
            // The bot's jump must cross the wire and lift its ball clear of the spawn
            // floor (~0.64m). Track the highest the bot ball reaches over a few seconds.
            val spawnY = GameConfig.GROUND_HEIGHT + GameConfig.BALL_RADIUS
            val jumped = awaitBallAbove(observer, botId = 0, y = spawnY + 1.0f, timeoutMs = 8_000)
            assertTrue(jumped) {
                "a socket-connected bot must drive its ball (jump) over the wire"
            }
            // Both the bot and the observer must own a ball in the shared world.
            assertTrue(observer.snapshot().entities.count { it.playerId >= 0 } >= 2) {
                "bot + observer must both own a ball"
            }
        } finally {
            observer.close()
            bots.stop()
            server.stop()
        }
    }

    private fun awaitBallAbove(session: GameSession, botId: Int, y: Float, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val ball = session.snapshot().entities.firstOrNull { it.playerId == botId }
            if (ball != null && ball.y >= y) return true
            Thread.sleep(30)
        }
        return false
    }

    companion object {
        private const val SEED = 42L
    }
}
