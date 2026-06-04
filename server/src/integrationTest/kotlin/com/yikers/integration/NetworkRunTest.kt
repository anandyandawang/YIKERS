package com.yikers.integration

import com.yikers.config.GameConfig
import com.yikers.net.DedicatedServer
import com.yikers.net.GameSession
import com.yikers.net.InputCommand
import com.yikers.net.NetworkHost
import com.yikers.net.RoomId
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

// End-to-end LAN test: boot a real DedicatedServer on an ephemeral localhost port,
// connect TWO socket clients to the one room, and prove they share the server's
// authoritative world. Connects straight to server.port (NOT via UDP discovery, which
// CI may block). The server owns the clock, so the clients WAIT on snapshot ticks
// rather than stepping anything themselves.
@HeadlessGdx
class NetworkRunTest {

    @Test
    fun twoClientsConnectToOneServerAndShareTheWorld() {
        val server = DedicatedServer(
            name = "test",
            tcpPort = 0, // OS-assigned ephemeral port
            cfg = SessionConfig(seed = SEED),
        )
        server.start()

        val p0 = NetworkHost("127.0.0.1", server.port).join(RoomId("net"))
        val p1 = NetworkHost("127.0.0.1", server.port).join(RoomId("net"))
        try {
            // Both clients must begin receiving the server's broadcast frames.
            awaitTick(p0)
            awaitTick(p1)

            val s0 = p0.snapshot()
            val s1 = p1.snapshot()
            assertTrue(s0.tick > 0) { "client 0 must receive server snapshots" }
            assertTrue(s1.tick > 0) { "client 1 must receive server snapshots" }
            assertTrue(playerBalls(s0).size >= 2) {
                "both climbers must exist in the shared world; got ${playerBalls(s0).size}"
            }

            // End-to-end input: hold right on client 0 while client 1 stays idle. The
            // wire carries no entity ids, so we track the rightmost player ball — the
            // only one being driven right — and prove it advanced. This shows an
            // InputCommand travelled the wire, hit the sim, and came back in a snapshot.
            // Short window so the rising kill-line can't end the run first.
            val x0 = playerBalls(p0.snapshot()).maxOf { it.x }
            repeat(40) {
                p0.submitInput(InputCommand(playerId = 0, vx = 4f, jump = false))
                Thread.sleep(16)
            }
            val x1 = playerBalls(p0.snapshot()).maxOf { it.x }
            assertTrue(x1 > x0) {
                "client 0's held-right input must move its climber over the wire; x0=$x0 x1=$x1"
            }

            // The shared clock keeps advancing on its own thread.
            val t = p0.snapshot().tick
            Thread.sleep(200)
            assertTrue(p0.snapshot().tick > t) { "server clock must keep ticking" }
        } finally {
            p0.close()
            p1.close()
            server.stop()
        }
    }

    // Player balls only (by size; boulders are bigger circles, no ids on the wire).
    private fun playerBalls(snap: WorldSnapshot) =
        snap.entities.filter { abs(it.sizeX - GameConfig.BALL_RADIUS * 2f) < 0.12f }

    // Spin until the client has seen at least one real frame, or give up.
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
