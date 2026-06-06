package com.yikers.e2e

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.yikers.control.HumanAgent
import com.yikers.net.DedicatedServer
import com.yikers.net.GameSession
import com.yikers.net.NetworkGameSession
import com.yikers.net.NetworkHost
import com.yikers.net.Participant
import com.yikers.net.PlayerSnap
import com.yikers.net.RoomId
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.random.Random

// A real :client climber over the socket. HumanAgent (from :client) reads a fake
// keyboard that mashes LEFT/RIGHT/SPACE at random; Participant pumps it like the
// render loop. Proves the CLIENT input path -- not :bot -- drives the shared world.
@HeadlessGdx
class NetworkClientTest {

    @Test
    fun randomMashingHumanClientMovesAndKeepsServerTicking() {
        val server = DedicatedServer(name = "test", tcpPort = 0, cfg = SessionConfig(seed = SEED))
        server.start()

        // HumanAgent reads Gdx.input; swap in a fake keyboard, restore at the end.
        val realInput = Gdx.input
        val masher = RandomKeyboard(Random(SEED))
        Gdx.input = masher

        val session = NetworkHost("127.0.0.1", server.port).join(RoomId("net"))
        val speed = (session as? NetworkGameSession)?.config?.runConfig?.horizontalSpeed
            ?: SessionConfig().runConfig.horizontalSpeed
        val client = Participant(session, HumanAgent(speed))
        try {
            awaitTick(session)
            val startX = ballOf(session.snapshot(), session.slot).x

            // ~3s of random mashing, pumping the client like the real render loop.
            var moved = false
            repeat(180) {
                masher.reroll()
                client.pump(1f / 60f)
                val x = ballOf(session.snapshot(), session.slot).x
                if (abs(x - startX) > MOVE_EPS) moved = true
                Thread.sleep(16)
            }

            assertTrue(moved) {
                "random client input must move its climber over the wire; startX=$startX"
            }
            val t = session.snapshot().tick
            Thread.sleep(200)
            assertTrue(session.snapshot().tick > t) { "server clock must keep ticking under a client" }
        } finally {
            client.close()
            server.stop()
            Gdx.input = realInput
        }
    }

    private fun ballOf(snap: WorldSnapshot, id: Int) =
        snap.entities.filterIsInstance<PlayerSnap>().first { it.slot == id }

    private fun awaitTick(session: GameSession, timeoutMs: Long = 5000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (session.snapshot().tick > 0) return
            Thread.sleep(20)
        }
    }

    // Fake keyboard: each reroll() randomly holds LEFT or RIGHT and sometimes taps jump.
    private class RandomKeyboard(private val rng: Random) : MockInput() {
        @Volatile private var left = false
        @Volatile private var right = false
        @Volatile private var jump = false

        fun reroll() {
            left = rng.nextBoolean()
            right = rng.nextBoolean()
            jump = rng.nextFloat() < 0.2f
        }

        override fun isKeyPressed(key: Int): Boolean = when (key) {
            Input.Keys.LEFT -> left
            Input.Keys.RIGHT -> right
            else -> false
        }

        override fun isKeyJustPressed(key: Int): Boolean =
            jump && (key == Input.Keys.SPACE || key == Input.Keys.UP)

        override fun justTouched(): Boolean = false

        override fun isPeripheralAvailable(peripheral: Input.Peripheral): Boolean = false
    }

    companion object {
        private const val SEED = 42L
        private const val MOVE_EPS = 0.01f
    }
}
