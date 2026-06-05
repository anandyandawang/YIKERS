package com.yikers.integration

import com.yikers.net.DedicatedServer
import com.yikers.net.EntitySnap
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.net.wire.Rejected
import com.yikers.net.wire.Welcome
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Black-box integration over a real loopback socket: a raw wire-protocol peer
// (no :client / :bot code) drives Input into the DedicatedServer and reads the
// Snapshot stream back out. Asserts only on observable wire behaviour.
@HeadlessGdx
class DedicatedServerIntegrationTest {

    @Test
    fun handshakeAssignsSlotAndStreamsSnapshots() {
        withServer { server ->
            val client = RawClient("127.0.0.1", server.port)
            try {
                val welcome = client.join()
                assertTrue(welcome is Welcome) { "first reply must be Welcome, got $welcome" }
                assertEquals(0, (welcome as Welcome).playerId) { "first peer gets slot 0" }

                val snap = client.awaitSnapshot()
                assertTrue(snap.tick > 0) { "server must stream ticking snapshots" }
                assertTrue(playerBalls(snap).isNotEmpty()) { "joined peer must appear in the world" }
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun heldRightInputMovesOwnClimber() {
        withServer { server ->
            val client = RawClient("127.0.0.1", server.port)
            try {
                client.join()
                client.awaitSnapshot()

                val x0 = client.ball().x
                repeat(40) {
                    client.send(vx = 4f)
                    Thread.sleep(16)
                }
                val x1 = client.ball().x
                assertTrue(x1 > x0) { "held-right input must move the peer's climber over the wire; x0=$x0 x1=$x1" }
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun jumpInputLaunchesClimberUpward() {
        withServer { server ->
            val client = RawClient("127.0.0.1", server.port)
            try {
                client.join()
                // Settle a few ticks so the climber rests grounded on the floor.
                repeat(10) { Thread.sleep(16) }
                val groundY = client.ball().y

                // Jump is latched server-side, so one jump=true launches; send a few
                // frames and track the peak height the snapshot stream reports.
                var peakY = groundY
                repeat(30) {
                    client.send(jump = true)
                    Thread.sleep(16)
                    peakY = maxOf(peakY, client.ball().y)
                }
                assertTrue(peakY > groundY + 1f) {
                    "jump input must launch the climber above its grounded height; groundY=$groundY peakY=$peakY"
                }
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun serverReStampsForgedPlayerId() {
        withServer { server ->
            val p0 = RawClient("127.0.0.1", server.port)
            val p1 = RawClient("127.0.0.1", server.port)
            try {
                p0.join(); p1.join()
                p0.awaitSnapshot(); p1.awaitSnapshot()

                // Both peers forge the OTHER's slot, pushing opposite ways. Server
                // re-stamps each input to its own connection, so direction reveals
                // the truth: p0 (pushing right) ends right, p1 (pushing left) ends
                // left. Were the forged ids honoured, the moves would land swapped.
                val p0x0 = p0.ball().x
                val p1x0 = p1.ball().x
                repeat(40) {
                    p0.send(playerId = 1, vx = 4f)   // forge p1's slot, push right
                    p1.send(playerId = 0, vx = -4f)  // forge p0's slot, push left
                    Thread.sleep(16)
                }
                val p0dx = p0.ball().x - p0x0
                val p1dx = p1.ball().x - p1x0
                assertTrue(p0dx > 0f) { "re-stamp must apply p0's own input: p0 should move right; dx=$p0dx" }
                assertTrue(p1dx < 0f) { "re-stamp must apply p1's own input: p1 should move left; dx=$p1dx" }
            } finally {
                p0.close(); p1.close()
            }
        }
    }

    @Test
    fun serverRejectsJoinWhenFull() {
        withServer(maxPlayers = 1) { server ->
            val p0 = RawClient("127.0.0.1", server.port)
            val p1 = RawClient("127.0.0.1", server.port)
            try {
                assertTrue(p0.join() is Welcome) { "first peer fills the one slot" }
                val reply = p1.join()
                assertTrue(reply is Rejected) { "a full server must reject the next peer, got $reply" }
                assertEquals("server full", (reply as Rejected).reason)
            } finally {
                p0.close(); p1.close()
            }
        }
    }

    // --- helpers -------------------------------------------------------------

    // Ephemeral port (0), loopback-only (discoverable=false: no UDP advertise on CI).
    private fun withServer(maxPlayers: Int = 8, body: (DedicatedServer) -> Unit) {
        val server = DedicatedServer(
            name = "test",
            tcpPort = 0,
            cfg = SessionConfig(seed = SEED),
            maxPlayers = maxPlayers,
            discoverable = false,
        )
        server.start()
        try {
            body(server)
        } finally {
            server.stop()
        }
    }

    private fun RawClient.awaitSnapshot(timeoutMs: Long = 5000): WorldSnapshot {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            latest?.let { if (it.tick > 0) return it }
            Thread.sleep(20)
        }
        return latest ?: error("no snapshot within ${timeoutMs}ms")
    }

    // This peer's own climber from the latest streamed snapshot.
    private fun RawClient.ball(): EntitySnap =
        awaitSnapshot().entities.first { it.playerId == playerId }

    private fun playerBalls(snap: WorldSnapshot) = snap.entities.filter { it.playerId >= 0 }

    companion object {
        private const val SEED = 42L
    }
}
