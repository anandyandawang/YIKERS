package com.yikers.integration

import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.RelayController
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Intent
import com.yikers.ecs.component.JumpState
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.component.augment.DoubleJump
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.JumpSystem
import com.yikers.support.HeadlessGdx
import com.yikers.support.TestWorld
import com.yikers.support.physicsWorld
import com.yikers.support.step
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// JumpSystem mechanic: a grounded climber launches at cfg.jumpVelocity; mid-air it
// jumps again only if its augments grant air jumps. Proves the GrantsAirJumps gate
// + JumpState budget via the real DoubleJump augment. No gravity: the body holds
// whatever velocity JumpSystem sets, so each phase reads cleanly.
@HeadlessGdx
class JumpAugmentTest {

    @Test
    fun groundJumpLaunchesAndAirJumpNeedsAugment() {
        val pw = physicsWorld(gravityScale = 0f)
        val cfg = RunConfig()
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()
        val world = configureWorld {
            injectables { add(pw); add(cfg); add(runState); add(refs) }
            systems { add(JumpSystem()) }
        }
        TestWorld(pw, world, runState, refs, cfg).use { tw ->
            val factory = EntityFactory(world, pw, cfg, refs)
            val player = factory.spawnPlayer(x = GameConfig.WIDTH / 2f, y = 1f, controller = RelayController(0))
            refs.player = player

            with(world) {
                val body = player[Physics].body
                player[Intent].jump = true // bot intent is irrelevant; force jump each tick

                // Grounded + jump intent -> launch at cfg.jumpVelocity.
                player[FootSensor].contacts = 1
                world.step(1)
                assertEquals(cfg.jumpVelocity, body.linearVelocity.y, EPS) { "ground jump must launch" }

                // Airborne, no augment -> no second jump.
                player[FootSensor].contacts = 0
                body.setLinearVelocity(0f, 0f)
                world.step(1)
                assertEquals(0f, body.linearVelocity.y, EPS) { "no air jump without an augment" }

                // Grant DoubleJump -> exactly one air jump, then capped by the budget.
                player[Augments].owned += DoubleJump
                body.setLinearVelocity(0f, 0f)
                world.step(1)
                assertEquals(cfg.jumpVelocity, body.linearVelocity.y, EPS) { "DoubleJump grants one air jump" }
                assertEquals(1, player[JumpState].airJumpsUsed) { "the air jump must be spent" }

                body.setLinearVelocity(0f, 0f)
                world.step(1)
                assertEquals(0f, body.linearVelocity.y, EPS) { "second air jump must be denied past budget" }
            }
        }
    }

    companion object {
        private const val EPS = 0.001f
    }
}
