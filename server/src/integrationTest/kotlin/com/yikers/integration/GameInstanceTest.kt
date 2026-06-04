package com.yikers.integration

import com.yikers.config.GameConfig
import com.yikers.net.InputCommand
import com.yikers.net.SessionConfig
import com.yikers.sim.GameInstance
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// GameInstance is the embedded server: the same headless sim, driven through the
// public tick()/snapshot()/submitInput() seam instead of poking the Fleks world
// directly. These prove (a) a snapshot mirrors the sim and a bot climbs through it,
// and (b) a relayed human InputCommand actually moves the primary climber.
@HeadlessGdx
class GameInstanceTest {

    private val dt = 1f / 60f

    @Test
    fun botRunProducesSnapshotThatClimbs() {
        val inst = GameInstance(SessionConfig(humans = 0, bots = 1, seed = SEED))
        try {
            val start = inst.snapshot()
            assertEquals(GameConfig.NUM_PLATFORMS, start.platforms.size) {
                "snapshot must expose every platform"
            }
            assertTrue(start.entities.isNotEmpty()) { "snapshot must expose climber + boulders" }

            repeat(CLIMB_SECONDS * 60) { inst.tick(dt) }

            val snap = inst.snapshot()
            assertTrue(snap.score > 0) { "embedded bot should climb + score; score=${snap.score}" }
            assertFalse(snap.dead) { "should still be alive mid-climb" }
        } finally {
            inst.close()
        }
    }

    @Test
    fun relayedHumanInputMovesPrimary() {
        val inst = GameInstance(SessionConfig(humans = 1, bots = 0, seed = SEED))
        try {
            inst.tick(dt) // settle one tick
            val x0 = inst.snapshot().entities.first().x

            // Hold right for ~0.5s via the relay; MoveSystem enacts Intent.vx.
            repeat(30) {
                inst.applyInput(InputCommand(playerId = 0, vx = 4f, jump = false))
                inst.tick(dt)
            }
            val x1 = inst.snapshot().entities.first().x

            assertTrue(x1 > x0) { "relayed +vx must move the primary right; x0=$x0 x1=$x1" }
        } finally {
            inst.close()
        }
    }

    companion object {
        private const val SEED = 42L
        private const val CLIMB_SECONDS = 20
    }
}
