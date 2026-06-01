package com.yikers.ecs.system

import com.badlogic.gdx.graphics.OrthographicCamera
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.ecs.resource.RunState
import kotlin.math.pow

// Auto-scroll upward, accelerating over time once the first platform is cleared.
class CameraScrollSystem(
    private val cam: OrthographicCamera = inject(),
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    override fun onTick() {
        if (runState.dead || !runState.startCamera) return
        runState.totalTime = minOf(runState.totalTime + deltaTime, 60f)
        // px/second scroll speed; * dt keeps scroll framerate-independent.
        val stepPerSecond = cfg.scrollAccelFactor * GameConfig.SCALING_FACTOR *
            (1.02.pow(runState.totalTime.toDouble()).toFloat() + 2f)
        cam.position.y += stepPerSecond * deltaTime
        cam.update()
    }
}
