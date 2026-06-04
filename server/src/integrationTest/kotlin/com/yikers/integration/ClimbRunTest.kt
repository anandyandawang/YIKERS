package com.yikers.integration

import com.yikers.config.GameConfig
import com.yikers.support.HeadlessGdx
import com.yikers.support.buildSim
import com.yikers.support.step
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Full-run smoke: a lone bot climbs + scores; raising the kill-line ends the run.
@HeadlessGdx
class ClimbRunTest {

    @Test
    fun botClimbsScoresThenDiesAtKillLine() {
        buildSim(seed = SEED).use { h ->
            val startY = h.playerY()

            h.world.step(CLIMB_SECONDS * 60)

            val climbedY = h.playerY()
            assertTrue(h.runState.score > 0) {
                "bot should clear at least one platform; score=${h.runState.score}"
            }
            assertTrue(climbedY > startY + CLIMB_MARGIN_M) {
                "bot should rise; startY=$startY climbedY=$climbedY"
            }
            assertFalse(h.runState.dead) { "should still be alive mid-climb" }

            h.runState.scrollY = climbedY + GameConfig.HEIGHT
            h.world.step(1)
            assertTrue(h.runState.dead) { "raising the kill-line above the player must end the run" }
        }
    }

    companion object {
        private const val SEED = 42L
        private const val CLIMB_SECONDS = 20
        private const val CLIMB_MARGIN_M = 1f
    }
}
