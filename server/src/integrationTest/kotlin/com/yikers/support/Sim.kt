package com.yikers.support

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.ecs.component.Physics
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import ktx.box2d.createWorld
import ktx.math.vec2
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// One real frame at 60fps. PhysicsStepSystem sub-steps this at 1/300 internally.
const val DT: Float = 1f / 60f

// Advance the sim `frames` fixed 1/60s ticks (what a 60fps run does each frame).
fun World.step(frames: Int, dt: Float = DT) = repeat(frames) { update(dt) }

// Advance whole seconds of sim time at 60fps.
fun World.stepSeconds(seconds: Float, dt: Float = DT) = step((seconds / dt).toInt(), dt)

// Tick until `predicate` holds or `maxSeconds` elapses. Returns true if it
// tripped (false = timed out), so tests assert on the result instead of hanging.
inline fun World.stepUntil(maxSeconds: Float, dt: Float = DT, predicate: () -> Boolean): Boolean {
    val maxFrames = (maxSeconds / dt).toInt()
    repeat(maxFrames) {
        if (predicate()) return true
        update(dt)
    }
    return predicate()
}

// Box2D world at realtime gravity scaled by `gravityScale` (0 = float, for
// hand-poked tests that set positions/velocities directly).
fun physicsWorld(gravityScale: Float = 1f): PhysicsWorld =
    createWorld(gravity = vec2(0f, GameConfig.GRAVITY * gravityScale))

// Ball-body center Y (meters) of any climber entity.
fun Entity.bodyY(world: World): Float = with(world) { this@bodyY[Physics].body.position.y }

// Bundles the two worlds + shared resources a test pokes, disposing both on close
// so tests can `buildXxx().use { ... }`. The full-run mirror is SimHarness; this
// wraps the lower-level configureWorld {} setups (bridging, jump, boot tests).
class TestWorld(
    val physicsWorld: PhysicsWorld,
    val world: World,
    val runState: RunState,
    val refs: Refs,
    val cfg: RunConfig,
) : AutoCloseable {
    override fun close() {
        world.dispose()
        physicsWorld.dispose()
    }
}
