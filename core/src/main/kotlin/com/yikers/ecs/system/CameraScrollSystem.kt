package com.yikers.ecs.system

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.ecs.resource.RunState
import kotlin.math.pow

// Auto-scroll upward, accelerating over time once the first platform is cleared.
// Advances the domain kill-line RunState.scrollY; the render cam follows it.
class CameraScrollSystem(
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    override fun onTick() {
        if (runState.dead || !runState.startCamera) return
        runState.totalTime = minOf(runState.totalTime + deltaTime, 60f)
        // px/second scroll speed; * dt keeps scroll framerate-independent.
        val stepPerSecond = cfg.scrollAccelFactor * GameConfig.SCALING_FACTOR *
            (1.02.pow(runState.totalTime.toDouble()).toFloat() + 2f)
        runState.scrollY += stepPerSecond * deltaTime
    }
}
