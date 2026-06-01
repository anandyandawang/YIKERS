package com.yikers.control

import com.yikers.config.GameConfig
import kotlin.math.abs

// Autopilot. Slide toward the gap in the next platform above, and only hop once
// roughly lined up under it (so it goes through the hole instead of bonking the
// slab). Blind to boulders for now.
class BotController : Controller {
    override fun decide(ctx: ControlContext): Move {
        val dx = ctx.targetHoleCenterX - ctx.playerX
        val deadzone = GameConfig.BALL_RADIUS * 0.5f
        val vx = when {
            dx > deadzone -> ctx.speed
            dx < -deadzone -> -ctx.speed
            else -> 0f
        }
        // No known gap above -> just hop. Otherwise wait until aligned with it.
        val alignWindow = maxOf(deadzone, ctx.targetHoleWidth * 0.35f)
        val aligned = ctx.targetHoleWidth <= 0f || abs(dx) <= alignWindow
        return Move(vx, jump = ctx.grounded && aligned)
    }
}
