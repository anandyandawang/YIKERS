package com.yikers.e2e

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.yikers.bot.app.BotRunner
import com.yikers.control.HumanAgent
import com.yikers.control.KeyProfile
import com.yikers.net.DedicatedServer
import com.yikers.net.NetworkHost
import com.yikers.net.Participant
import com.yikers.net.RoomId
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// A full LAN party on ONE server a human hosts: 2 humans + 2 bots in one world.
// Mirrors LobbyScreen.startHost(): the host human's process owns the
// DedicatedServer and dials 127.0.0.1; a second human + 2 bots dial the same
// port. The two humans share one headless keyboard but read DIFFERENT binds
// (ARROWS vs WASD) so they steer independently. Connect straight to the port;
// UDP discovery may be CI-blocked.
@HeadlessGdx
class LanPartyTest {

    @Test
    fun twoHumansAndTwoBotsShareOneHumanHostedServer() {
        // The hosting human's process owns the server (startHost()).
        val server = DedicatedServer(name = "Host @ test", tcpPort = 0, cfg = SessionConfig(seed = SEED))
        server.start()

        // Both humans read Gdx.input; one fake keyboard, two bind sets:
        // host = ARROWS (holds RIGHT), guest = WASD (holds A = left). Also taps
        // jump every frame so they keep climbing and stay ahead of the kill-line.
        val realInput = Gdx.input
        val pad = TwoHumanKeyboard()
        Gdx.input = pad

        val speed = SessionConfig().runConfig.horizontalSpeed
        // Host human joins own server on loopback (like joinTarget("127.0.0.1", port)).
        val hostSession = NetworkHost("127.0.0.1", server.port).join(RoomId("net"))
        val hostHuman = Participant(hostSession, HumanAgent(speed, KeyProfile.ARROWS))
        // Guest human joins the same port.
        val guestSession = NetworkHost("127.0.0.1", server.port).join(RoomId("net"))
        val guestHuman = Participant(guestSession, HumanAgent(speed, KeyProfile.WASD))
        // Two bots join the same port (own daemon pump thread).
        val bots = BotRunner("127.0.0.1", server.port, count = 2)
        bots.start()

        try {
            // All four slots must land in the one shared world.
            assertTrue(awaitPlayers(server, 4)) {
                "all 4 players (2 human + 2 bot) must join one server; got ${playerCount(server)}"
            }
            assertTrue(server.playerCount == 4) { "server must hold 4 players; got ${server.playerCount}" }

            // One authoritative frame -- the client sessions may not have received
            // their first snapshot yet, but the server already spawned all four.
            val snap0 = server.latestSnapshot!!
            val hostId = hostSession.playerId
            val guestId = guestSession.playerId
            val botIds = playerSlots(snap0).filter { it != hostId && it != guestId }
            assertTrue(botIds.size == 2) { "the other two slots must be the bots; got $botIds" }

            val hostX0 = ballOf(snap0, hostId).x
            val guestX0 = ballOf(snap0, guestId).x
            val botBaseY = botIds.associateWith { ballOf(snap0, it).y }

            // ~2s: pump the two humans like a render loop; bots pump themselves.
            // Read x off the server (authoritative) WHILE the climber is alive --
            // the kill-line can catch a human who only bunny-hops -- so latch the
            // move the moment the wire carries it.
            var hostMovedRight = false
            var guestMovedLeft = false
            repeat(120) {
                hostHuman.pump(1f / 60f)
                guestHuman.pump(1f / 60f)
                server.latestSnapshot?.let { snap ->
                    xOf(snap, hostId)?.let { if (it > hostX0 + MOVE_EPS) hostMovedRight = true }
                    xOf(snap, guestId)?.let { if (it < guestX0 - MOVE_EPS) guestMovedLeft = true }
                }
                Thread.sleep(16)
            }

            // Independent human input: host went RIGHT, guest went LEFT, over the wire.
            assertTrue(hostMovedRight) {
                "host human (ARROWS, hold RIGHT) must move its climber right from x0=$hostX0"
            }
            assertTrue(guestMovedLeft) {
                "guest human (WASD, hold A) must move its climber left from x0=$guestX0"
            }

            // The two bots are real autonomous players: at least one climbs in the same world.
            assertTrue(awaitClimb(server, botIds, botBaseY)) {
                "a bot must climb autonomously in the shared world; base=$botBaseY"
            }

            // Server clock keeps running under the full lobby.
            val t = server.latestSnapshot!!.tick
            Thread.sleep(200)
            assertTrue((server.latestSnapshot?.tick ?: 0) > t) { "server clock must keep ticking" }
        } finally {
            hostHuman.close()
            guestHuman.close()
            bots.stop()
            server.stop()
            Gdx.input = realInput
        }
    }

    private fun playerSlots(snap: WorldSnapshot) =
        snap.entities.filter { it.playerId >= 0 }.map { it.playerId }.distinct()

    private fun playerCount(server: DedicatedServer) =
        server.latestSnapshot?.let { playerSlots(it).size } ?: 0

    private fun ballOf(snap: WorldSnapshot, id: Int) = snap.entities.first { it.playerId == id }

    private fun xOf(snap: WorldSnapshot, id: Int) = snap.entities.firstOrNull { it.playerId == id }?.x

    private fun awaitPlayers(server: DedicatedServer, n: Int, timeoutMs: Long = 8000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (playerCount(server) >= n) return true
            Thread.sleep(20)
        }
        return playerCount(server) >= n
    }

    // Poll until any of the given slots has climbed BOT_CLIMB above its base y.
    private fun awaitClimb(
        server: DedicatedServer,
        slots: List<Int>,
        baseY: Map<Int, Float>,
        timeoutMs: Long = 12_000,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val snap = server.latestSnapshot
            if (snap != null && slots.any { id ->
                    val y = snap.entities.firstOrNull { it.playerId == id }?.y
                    y != null && y > (baseY[id] ?: 0f) + BOT_CLIMB
                }
            ) {
                return true
            }
            Thread.sleep(50)
        }
        return false
    }

    // One headless keyboard, two bind sets. ARROWS human holds RIGHT; WASD human
    // holds A (= left). Both tap jump every frame. Different keys => independent
    // control from a single Gdx.input.
    private class TwoHumanKeyboard : MockInput() {
        override fun isKeyPressed(key: Int): Boolean = when (key) {
            Input.Keys.RIGHT -> true // host (ARROWS) -> right
            Input.Keys.A -> true     // guest (WASD) -> left
            else -> false
        }

        override fun isKeyJustPressed(key: Int): Boolean = when (key) {
            Input.Keys.SPACE, Input.Keys.UP, Input.Keys.W -> true // host + guest jump
            else -> false
        }

        override fun justTouched(): Boolean = false

        override fun isPeripheralAvailable(peripheral: Input.Peripheral): Boolean = false
    }

    companion object {
        private const val SEED = 42L
        private const val MOVE_EPS = 0.05f
        private const val BOT_CLIMB = 0.5f
    }
}
