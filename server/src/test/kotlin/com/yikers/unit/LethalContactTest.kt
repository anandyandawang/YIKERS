package com.yikers.unit

import com.badlogic.gdx.physics.box2d.BodyDef
import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.RelayController
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.Lethal
import com.yikers.ecs.event.Events
import com.yikers.ecs.event.PlayerDied
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.DeathSystem
import com.yikers.ecs.system.PhysicsStepSystem
import com.yikers.ecs.system.TransformSyncSystem
import com.yikers.physics.PlayContactListener
import com.yikers.support.HeadlessGdx
import com.yikers.support.TestWorld
import com.yikers.support.physicsWorld
import com.yikers.support.stepUntil
import ktx.box2d.body
import ktx.box2d.box
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// The full lethal path: ball touches a Lethal entity -> contact listener emits
// LethalContact -> DeathSystem marks Dead + emits PlayerDied. No flush system
// here on purpose, so the queue can be asserted after the run.
@HeadlessGdx
class LethalContactTest {

    @Test
    fun touchingLethalMarksDeadAndEmitsPlayerDied() {
        val pw = physicsWorld(gravityScale = 1f)
        val cfg = RunConfig()
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()
        val events = Events()
        val world = configureWorld {
            injectables { add(pw); add(runState); add(events) }
            systems {
                add(PhysicsStepSystem())
                add(TransformSyncSystem())
                add(DeathSystem())
            }
        }
        TestWorld(pw, world, runState, refs, cfg).use {
            val hazardBody = pw.body {
                type = BodyDef.BodyType.StaticBody
                position.set(GameConfig.WIDTH / 2f, 1f)
                box(width = 1f, height = 0.3f) {}
            }
            val hazard = world.entity { it += Lethal() }
            hazardBody.userData = hazard

            val factory = EntityFactory(world, pw, cfg, refs)
            val player = factory.spawnPlayer(
                x = GameConfig.WIDTH / 2f - GameConfig.BALL_RADIUS,
                y = 2f,
                controller = RelayController(0),
                slot = 0,
            )
            refs.player = player

            pw.setContactListener(PlayContactListener(world, events))

            val died = world.stepUntil(3f) { with(world) { player.getOrNull(Dead) != null } }
            assertTrue(died) { "falling onto a Lethal entity must mark the climber Dead" }
            assertTrue(events.queue.filterIsInstance<PlayerDied>().any { it.player == player }) {
                "a lethal death must emit PlayerDied"
            }
        }
    }
}
