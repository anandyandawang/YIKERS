package com.yikers.integration

import com.yikers.bot.BotAgent
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.net.EntitySnap
import com.yikers.net.InputCommand
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.sim.GameInstance
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Drives GameInstance directly: bot climbs+scores; relayed input moves climber.
@HeadlessGdx
class GameInstanceTest {

    private val dt = 1f / 60f

    @Test
    fun botClientProducesSnapshotThatClimbs() {
        val inst = GameInstance(SessionConfig(seed = SEED))
        try {
            val start = inst.snapshot()
            assertEquals(GameConfig.NUM_PLATFORMS, start.platforms.size) {
                "snapshot must expose every platform"
            }
            assertTrue(start.entities.isNotEmpty()) { "snapshot must expose the boulder pool" }

            val pid = inst.addPlayer()
            val bot = BotAgent(RunConfig())
            inst.tick(dt) // spawn the bot's ball

            repeat(CLIMB_SECONDS * 60) {
                inst.applyInput(bot.decide(inst.snapshot(), pid, dt).copy(playerId = pid))
                inst.tick(dt)
            }

            val snap = inst.snapshot()
            assertTrue(snap.score > 0) { "bot client should climb + score; score=${snap.score}" }
            assertFalse(snap.dead) { "should still be alive mid-climb" }
        } finally {
            inst.close()
        }
    }

    @Test
    fun relayedHumanInputMovesClimber() {
        val inst = GameInstance(SessionConfig(seed = SEED))
        try {
            val pid = inst.addPlayer()
            inst.tick(dt) // spawn slot 0's ball
            val x0 = playerBall(inst.snapshot()).x

            repeat(30) {
                inst.applyInput(InputCommand(playerId = pid, vx = 4f, jump = false))
                inst.tick(dt)
            }
            val x1 = playerBall(inst.snapshot()).x

            assertTrue(x1 > x0) { "relayed +vx must move the climber right; x0=$x0 x1=$x1" }
        } finally {
            inst.close()
        }
    }

    private fun playerBall(snap: WorldSnapshot): EntitySnap =
        snap.entities.first { it.playerId >= 0 }

    companion object {
        private const val SEED = 42L
        private const val CLIMB_SECONDS = 20
    }
}
