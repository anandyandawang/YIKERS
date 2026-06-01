package com.yikers.ecs.system

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.POSITION_ITERS
import com.yikers.TIME_STEP
import com.yikers.VELOCITY_ITERS
import com.yikers.ecs.resource.RunState
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// Fixed-timestep Box2D stepping decoupled from render dt.
class PhysicsStepSystem(
    private val pw: PhysicsWorld = inject(),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    private var acc = 0f

    override fun onTick() {
        if (runState.dead) return
        acc += deltaTime
        while (acc >= TIME_STEP) {
            pw.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS)
            acc -= TIME_STEP
        }
    }
}
