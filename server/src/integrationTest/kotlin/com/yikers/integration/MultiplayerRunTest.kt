package com.yikers.integration

import com.github.quillraven.fleks.Entity
import com.yikers.config.GameConfig
import com.yikers.control.BotController
import com.yikers.ecs.component.Physics
import com.yikers.support.HeadlessGdx
import com.yikers.support.SimHarness
import com.yikers.support.buildSim
import com.yikers.support.step
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// DeathSystem run-end semantics with a multi-climber roster: a climber that falls
// below the rising kill-line is marked Dead, but the RUN ends only once no living
// climber remains. Two bots; we drop them below the kill-line one at a time.
@HeadlessGdx
class MultiplayerRunTest {

    @Test
    fun runEndsOnlyWhenAllClimbersDead() {
        buildSim(controllers = listOf(BotController(), BotController()), seed = SEED).use { h ->
            h.world.step(1) // settle one tick; both climbers alive
            assertFalse(h.runState.dead) { "run starts with both climbers alive" }

            val (a, b) = h.climbers
            // Drop climber A below the kill-line -> A dies, run keeps going.
            dropBelowKillLine(h, a)
            h.world.step(1)
            assertFalse(h.runState.dead) { "run must continue while one climber lives" }

            // Drop the last climber -> no living climber remains -> run ends.
            dropBelowKillLine(h, b)
            h.world.step(1)
            assertTrue(h.runState.dead) { "run must end once every climber is dead" }
        }
    }

    // Teleport a climber a meter under the kill-line (view bottom); DeathSystem
    // reaps anything with ballY < scrollY on the next tick.
    private fun dropBelowKillLine(h: SimHarness, e: Entity) {
        with(h.world) {
            e[Physics].body.setTransform(GameConfig.WIDTH / 2f, h.runState.scrollY - 1f, 0f)
        }
    }

    companion object {
        private const val SEED = 42L
    }
}
