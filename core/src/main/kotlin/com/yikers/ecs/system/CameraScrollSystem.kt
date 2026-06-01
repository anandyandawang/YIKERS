package com.yikers.ecs.system

import com.badlogic.gdx.graphics.OrthographicCamera
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.ecs.resource.RunState
import kotlin.math.pow

// fps the original was tuned at: it advanced the camera once per frame at 60fps.
private const val REF_FPS = 60f

// Auto-scroll upward, accelerating over time once the first platform is cleared.
class CameraScrollSystem(
    private val cam: OrthographicCamera = inject(),
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    override fun onTick() {
        if (runState.dead || !runState.startCamera) return
        runState.totalTime = minOf(runState.totalTime + deltaTime, 60f)
        // stepPerRefFrame = px the original added each frame at 60fps. Scale by
        // dt*REF_FPS so the per-second scroll speed is identical at 60fps but
        // framerate-independent (no faster scroll on a high-fps display).
        val stepPerRefFrame = cfg.scrollAccelFactor * GameConfig.SCALING_FACTOR *
            (1.02.pow(runState.totalTime.toDouble()).toFloat() + 2f)
        cam.position.y += stepPerRefFrame * minOf(deltaTime, 0.25f) * REF_FPS
        cam.update()
    }
}
