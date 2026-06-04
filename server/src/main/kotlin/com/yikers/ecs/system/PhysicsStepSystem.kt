package com.yikers.ecs.system

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.POSITION_ITERS
import com.yikers.TIME_STEP
import com.yikers.VELOCITY_ITERS
import com.yikers.ecs.resource.RunState
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

class PhysicsStepSystem(
    private val pw: PhysicsWorld = inject(),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    private var acc = 0f

    override fun onTick() {
        if (runState.dead) return
        // Clamp frame time: the first play frame is huge (lazy loads, worst iOS cold
        // start); unclamped, the catch-up burst stalls the thread + iOS watchdog kills it.
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
