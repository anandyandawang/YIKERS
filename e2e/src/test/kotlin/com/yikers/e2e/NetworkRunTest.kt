package com.yikers.e2e

import com.yikers.net.DedicatedServer
import com.yikers.net.GameSession
import com.yikers.net.InputCommand
import com.yikers.net.NetworkGameSession
import com.yikers.net.PlayerSnap
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Connect straight to the port; UDP discovery may be CI-blocked.
@HeadlessGdx
class NetworkRunTest {

    @Test
    fun twoClientsConnectToOneServerAndShareTheWorld() {
        val server = DedicatedServer(
            name = "test",
            tcpPort = 0,
            cfg = SessionConfig(seed = SEED),
        )
        server.start()

        val p0 = NetworkGameSession.connect("127.0.0.1", server.port)
        val p1 = NetworkGameSession.connect("127.0.0.1", server.port)
        try {
            awaitTick(p0)
            awaitTick(p1)

            val s0 = p0.snapshot()
            val s1 = p1.snapshot()
            assertTrue(s0.tick > 0) { "client 0 must receive server snapshots" }
            assertTrue(s1.tick > 0) { "client 1 must receive server snapshots" }
            assertTrue(playerBalls(s0).size >= 2) {
                "both climbers must exist in the shared world; got ${playerBalls(s0).size}"
            }

            val x0 = ballOf(p0.snapshot(), 0).x
            repeat(40) {
                p0.submitInput(InputCommand(slot = 0, vx = 4f, jump = false))
                Thread.sleep(16)
            }
            val x1 = ballOf(p0.snapshot(), 0).x
            assertTrue(x1 > x0) {
                "client 0's held-right input must move its own climber over the wire; x0=$x0 x1=$x1"
            }

            val t = p0.snapshot().tick
            Thread.sleep(200)
            assertTrue(p0.snapshot().tick > t) { "server clock must keep ticking" }
        } finally {
            p0.close()
            p1.close()
            server.stop()
        }
    }

    private fun playerBalls(snap: WorldSnapshot) = snap.entities.filterIsInstance<PlayerSnap>()

    private fun ballOf(snap: WorldSnapshot, id: Int) =
        snap.entities.filterIsInstance<PlayerSnap>().first { it.slot == id }

    private fun awaitTick(session: GameSession, timeoutMs: Long = 5000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (session.snapshot().tick > 0) return
            Thread.sleep(20)
        }
    }

    companion object {
        private const val SEED = 42L
    }
}
