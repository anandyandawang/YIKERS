package com.yikers.component

import com.yikers.net.EntitySnap
import com.yikers.net.InputCommand
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.sim.GameInstance
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Drives GameInstance directly: relayed input moves the right climber.
// (A real bot climbing over the socket lives in :e2e NetworkBotTest.)
@HeadlessGdx
class GameInstanceTest {

    private val dt = 1f / 60f

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
    }
}
