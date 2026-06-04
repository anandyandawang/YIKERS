package com.yikers.ecs.system

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.Player
import com.yikers.ecs.resource.RunState
import kotlin.math.pow

// Holds the kill-line still until a climber reaches platform 1, then scrolls up.
class ScrollSystem(
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    private val climbers = world.family { all(Player, Physics, FootSensor).none(Dead) }

    override fun onTick() {
        if (runState.dead) return
        if (!runState.startCamera) {
            if (!anyClimberOnPlatform()) return
            runState.startCamera = true
        }
        runState.totalTime = minOf(runState.totalTime + deltaTime, 60f)
        val stepPerSecond = cfg.scrollAccelFactor * GameConfig.SCALING_FACTOR *
            (1.02.pow(runState.totalTime.toDouble()).toFloat() + 2f)
        runState.scrollY += stepPerSecond * deltaTime
    }

    // Grounded above the spawn floor (2*radius margin excludes the ground).
    private fun anyClimberOnPlatform(): Boolean {
        val floorTop = GameConfig.GROUND_HEIGHT + GameConfig.BALL_RADIUS * 2f
        var found = false
        climbers.forEach { e ->
            if (!found && e[FootSensor].contacts > 0 && e[Physics].body.position.y > floorTop) {
                found = true
            }
        }
        return found
    }
}
