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

fun World.step(frames: Int, dt: Float = DT) = repeat(frames) { update(dt) }

fun World.stepSeconds(seconds: Float, dt: Float = DT) = step((seconds / dt).toInt(), dt)

// Tick until predicate holds or maxSeconds elapses; returns true if it tripped.
inline fun World.stepUntil(maxSeconds: Float, dt: Float = DT, predicate: () -> Boolean): Boolean {
    val maxFrames = (maxSeconds / dt).toInt()
    repeat(maxFrames) {
        if (predicate()) return true
        update(dt)
    }
    return predicate()
}

// Box2D world at gravity * gravityScale (0 = float, for hand-poked tests).
fun physicsWorld(gravityScale: Float = 1f): PhysicsWorld =
    createWorld(gravity = vec2(0f, GameConfig.GRAVITY * gravityScale))

fun Entity.bodyY(world: World): Float = with(world) { this@bodyY[Physics].body.position.y }

// Bundles both worlds + resources a test pokes; close() disposes both.
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
