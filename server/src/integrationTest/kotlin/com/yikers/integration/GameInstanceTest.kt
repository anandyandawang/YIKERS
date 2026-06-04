package com.yikers.integration

import com.yikers.bot.BotAgent
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.net.EntitySnap
import com.yikers.net.InputCommand
import com.yikers.net.LocalHost
import com.yikers.net.Participant
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// GameInstance is the embedded server, driven through the LocalHost seam exactly
// like a real client. These prove (a) a BOT CLIENT — an ordinary GameSession
// reading snapshots and submitting InputCommand — climbs and scores, and (b) a
// relayed human InputCommand moves that client's climber. The instance never knows
// which client is a bot; both go through addPlayer() + applyInput().
@HeadlessGdx
class GameInstanceTest {

    private val dt = 1f / 60f

    @Test
    fun botClientProducesSnapshotThatClimbs() {
        val host = LocalHost()
        val room = host.open(SessionConfig(seed = SEED))
        val inst = host.instance(room)
        try {
            val start = inst.snapshot()
            assertEquals(GameConfig.NUM_PLATFORMS, start.platforms.size) {
                "snapshot must expose every platform"
            }
            assertTrue(start.entities.isNotEmpty()) { "snapshot must expose the boulder pool" }

            // A bot is just a client: a Participant pairing a session with a BotAgent.
            // join -> read snapshot -> decide -> submit, identical to a human client.
            val bot = Participant(host.join(room), BotAgent(RunConfig()))
            inst.tick(dt) // spawn the bot's ball (addPlayer queued the spawn)

            repeat(CLIMB_SECONDS * 60) {
                bot.pump(dt)
                inst.tick(dt)
            }

            val snap = inst.snapshot()
            assertTrue(snap.score > 0) { "bot client should climb + score; score=${snap.score}" }
            assertFalse(snap.dead) { "should still be alive mid-climb" }
        } finally {
            host.close(room)
        }
    }

    @Test
    fun relayedHumanInputMovesClimber() {
        val host = LocalHost()
        val room = host.open(SessionConfig(seed = SEED))
        val inst = host.instance(room)
        try {
            val session = host.join(room)
            inst.tick(dt) // spawn slot 0's ball
            val x0 = playerBall(inst.snapshot()).x

            // Hold right for ~0.5s via the relay; MoveSystem enacts Intent.vx.
            repeat(30) {
                inst.applyInput(InputCommand(playerId = session.playerId, vx = 4f, jump = false))
                inst.tick(dt)
            }
            val x1 = playerBall(inst.snapshot()).x

            assertTrue(x1 > x0) { "relayed +vx must move the climber right; x0=$x0 x1=$x1" }
        } finally {
            host.close(room)
        }
    }

    // The single player ball, found by its slot id on the wire.
    private fun playerBall(snap: WorldSnapshot): EntitySnap =
        snap.entities.first { it.playerId >= 0 }

    companion object {
        private const val SEED = 42L
        private const val CLIMB_SECONDS = 20
    }
}
