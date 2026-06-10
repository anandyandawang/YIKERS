package com.yikers.unit

import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.RelayController
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Intent
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.component.augment.MoonBoots
import com.yikers.ecs.component.augment.SwiftBoots
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.JumpSystem
import com.yikers.ecs.system.MoveSystem
import com.yikers.support.HeadlessGdx
import com.yikers.support.TestWorld
import com.yikers.support.physicsWorld
import com.yikers.support.step
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// Stat traits: GrantsMoveSpeed scales Intent.vx, GrantsJumpBoost scales launch
// velocity. No gravity, so velocities read back exactly.
@HeadlessGdx
class AugmentTraitTest {

    @Test
    fun moveSpeedTraitMultipliesHorizontalVelocity() {
        val pw = physicsWorld(gravityScale = 0f)
        val cfg = RunConfig()
        val runState = RunState()
        val refs = Refs()
        val world = configureWorld {
            injectables { add(pw); add(cfg); add(runState); add(refs) }
            systems { add(MoveSystem()) }
        }
        TestWorld(pw, world, runState, refs, cfg).use {
            val factory = EntityFactory(world, pw, refs)
            val player = factory.spawnPlayer(x = GameConfig.WIDTH / 2f, y = 1f, controller = RelayController(0), slot = 0)
            with(world) {
                val body = player[Physics].body
                player[Intent].vx = cfg.horizontalSpeed

                world.step(1)
                assertEquals(cfg.horizontalSpeed, body.linearVelocity.x, EPS) { "no augment, no boost" }

                player[Augments].owned += SwiftBoots
                world.step(1)
                assertEquals(cfg.horizontalSpeed * SwiftBoots.moveSpeedMultiplier, body.linearVelocity.x, EPS)
            }
        }
    }

    @Test
    fun jumpBoostTraitMultipliesJumpVelocity() {
        val pw = physicsWorld(gravityScale = 0f)
        val cfg = RunConfig()
        val runState = RunState()
        val refs = Refs()
        val world = configureWorld {
            injectables { add(pw); add(cfg); add(runState); add(refs) }
            systems { add(JumpSystem()) }
        }
        TestWorld(pw, world, runState, refs, cfg).use {
            val factory = EntityFactory(world, pw, refs)
            val player = factory.spawnPlayer(x = GameConfig.WIDTH / 2f, y = 1f, controller = RelayController(0), slot = 0)
            with(world) {
                val body = player[Physics].body
                player[Intent].jump = true
                player[FootSensor].contacts = 1

                world.step(1)
                assertEquals(cfg.jumpVelocity, body.linearVelocity.y, EPS) { "no augment, stock jump" }

                player[Augments].owned += MoonBoots
                world.step(1)
                assertEquals(cfg.jumpVelocity * MoonBoots.jumpVelocityMultiplier, body.linearVelocity.y, EPS)
            }
        }
    }

    companion object {
        private const val EPS = 0.001f
    }
}
