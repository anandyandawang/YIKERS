package com.yikers.ecs.system

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.POSITION_ITERS
import com.yikers.TIME_STEP
import com.yikers.VELOCITY_ITERS
import com.yikers.ecs.resource.RunState
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// Fixed-timestep Box2D stepping decoupled from render dt (clamp avoids spiral).
class PhysicsStepSystem(
    private val pw: PhysicsWorld = inject(),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    private var acc = 0f

    override fun onTick() {
        if (runState.dead) return
        // Clamp frame time before accumulating. The first play frame (Box2D
        // native + ShapeRenderer shader load lazily on menu->play) can be huge,
        // esp. on iOS cold start; unclamped that's hundreds of 1/300s steps in
        // one tick -> the catch-up burst blocks the main thread and the iOS
        // watchdog kills the app (looks like a crash on tap). Cap so a hitch
        // costs a bounded burst instead of spiraling.
        acc += minOf(deltaTime, MAX_FRAME_TIME)
        while (acc >= TIME_STEP) {
            pw.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS)
            acc -= TIME_STEP
        }
    }

    companion object {
        // Worst-case 0.25s -> 75 steps at 1/300; bounded, never spirals.
        private const val MAX_FRAME_TIME = 0.25f
    }
}
