package com.yikers.integration

import com.github.quillraven.fleks.Entity
import com.yikers.config.GameConfig
import com.yikers.support.AutopilotController
import com.yikers.ecs.component.Physics
import com.yikers.support.HeadlessGdx
import com.yikers.support.SimHarness
import com.yikers.support.buildSim
import com.yikers.support.step
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// A climber below the kill-line dies, but the run ends only when none live.
@HeadlessGdx
class MultiplayerRunTest {

    @Test
    fun runEndsOnlyWhenAllClimbersDead() {
        buildSim(controllers = listOf(AutopilotController(), AutopilotController()), seed = SEED).use { h ->
            h.world.step(1) // settle one tick; both climbers alive
            assertFalse(h.runState.dead) { "run starts with both climbers alive" }

            val (a, b) = h.climbers
            dropBelowKillLine(h, a)
            h.world.step(1)
            assertFalse(h.runState.dead) { "run must continue while one climber lives" }

            dropBelowKillLine(h, b)
            h.world.step(1)
            assertTrue(h.runState.dead) { "run must end once every climber is dead" }
        }
    }

    private fun dropBelowKillLine(h: SimHarness, e: Entity) {
        with(h.world) {
            e[Physics].body.setTransform(GameConfig.WIDTH / 2f, h.runState.scrollY - 1f, 0f)
        }
    }

    companion object {
        private const val SEED = 42L
    }
}
