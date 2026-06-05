package com.yikers.support

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.ControlContext
import com.yikers.control.Controller
import com.yikers.control.Move
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Intent
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.component.Physics
import com.yikers.ecs.resource.RunState
import kotlin.math.abs

// Test-only climber: hop through the gap above, land on solid, repeat. Mirrors
// the shape of the real bot's climb (target hole above + support hole below +
// apex check) so it ascends reliably, but is self-contained: pure geometry, no
// :bot, no boulder-dodging. The real bot's quality is proven over the socket in
// :e2e.
class ScriptedClimber : Controller {
    // Unused: ScriptedClimbSystem writes Intent directly. Satisfies the seam.
    override fun decide(ctx: ControlContext): Move = Move(0f, false)
}

// Reads the ECS for each scripted climber's holes, writes Intent.
class ScriptedClimbSystem(
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Controlled, Physics, FootSensor, Intent).none(Dead) }) {
    private val platforms = family { all(PlatformC) }
    private val gravityPxS2 = abs(GameConfig.GRAVITY * cfg.gravityScale)

    override fun onTickEntity(entity: Entity) {
        if (runState.dead) return
        if (entity[Controlled].controller !is ScriptedClimber) return
        val body = entity[Physics].body
        val px = body.position.x
        val py = body.position.y
        val vy = body.linearVelocity.y
        val grounded = entity[FootSensor].contacts > 0
        val speed = cfg.horizontalSpeed

        // Nearest non-bridged hole above (target), the one after it (next), and the
        // nearest hole below us (support) so we know where NOT to land.
        var targetY = Float.MAX_VALUE; var targetCx = px; var targetW = 0f
        var nextY = Float.MAX_VALUE; var nextCx = px; var nextW = 0f
        var supY = -Float.MAX_VALUE; var supCx = px; var supW = 0f
        platforms.forEach { e ->
            val p = e[PlatformC]
            val holeW = if (p.bridged) 0f else p.holeWidth
            val cx = if (p.bridged) px else p.holeX + p.holeWidth / 2f
            if (p.y > py) {
                if (p.y < targetY) {
                    nextY = targetY; nextCx = targetCx; nextW = targetW
                    targetY = p.y; targetCx = cx; targetW = holeW
                } else if (p.y < nextY) {
                    nextY = p.y; nextCx = cx; nextW = holeW
                }
            } else if (p.y > supY) {
                supY = p.y; supCx = cx; supW = holeW
            }
        }
        val targetPlatformY = if (targetY == Float.MAX_VALUE) py + GameConfig.PLATFORM_INTERVALS else targetY
        if (nextW != 0f && nextY == Float.MAX_VALUE) nextW = 0f
        if (supY == -Float.MAX_VALUE) supW = 0f

        val deadzone = GameConfig.BALL_RADIUS * DEADZONE_FRAC
        val dx = targetCx - px
        val aligned = targetW <= 0f || abs(dx) <= maxOf(deadzone, targetW * ALIGN_FRAC)

        val intent = entity[Intent]
        if (!grounded) {
            intent.vx = airborneSteer(px, py, vy, speed, deadzone, targetCx, targetW, targetPlatformY, supCx, supW)
            intent.jump = false
            return
        }
        // Grounded: climb; when idle-aligned, pre-aim at the next hole.
        intent.vx = when {
            abs(dx) > deadzone -> steer(dx, deadzone, speed)
            nextW > 0f -> steer(nextCx - px, deadzone, speed)
            else -> 0f
        }
        intent.jump = aligned
    }

    private fun steer(offset: Float, deadzone: Float, speed: Float): Float = when {
        offset > deadzone -> speed
        offset < -deadzone -> -speed
        else -> 0f
    }

    // Head for the gap while the hop can still reach it; once the apex won't clear
    // the slab, commit to landing on solid so we don't drop back through.
    private fun airborneSteer(
        px: Float, py: Float, vy: Float, speed: Float, deadzone: Float,
        targetCx: Float, targetW: Float, targetPlatformY: Float, supCx: Float, supW: Float,
    ): Float {
        val remainingRise = if (vy > 0f && gravityPxS2 > 0f) vy * vy / (2f * gravityPxS2) else 0f
        val apexY = py + remainingRise
        val canReachTarget = targetW > 0f && apexY >= targetPlatformY + GameConfig.PLATFORM_HEIGHT
        if (canReachTarget || supW <= 0f) return steer(targetCx - px, deadzone, speed)

        val r = GameConfig.BALL_RADIUS
        val overHole = abs(targetCx - supCx) < supW / 2f + r + LANDING_PAD
        val landX = if (!overHole) targetCx else {
            val leftLand = supCx - supW / 2f - r - LANDING_PAD
            val rightLand = supCx + supW / 2f + r + LANDING_PAD
            val leftOk = leftLand >= LEFT_BOUND
            val rightOk = rightLand <= RIGHT_BOUND
            when {
                leftOk && rightOk -> if (abs(leftLand - targetCx) <= abs(rightLand - targetCx)) leftLand else rightLand
                leftOk -> leftLand
                rightOk -> rightLand
                else -> targetCx
            }.coerceIn(LEFT_BOUND, RIGHT_BOUND)
        }
        return steer(landX - px, deadzone, speed)
    }

    companion object {
        private const val DEADZONE_FRAC = 0.5f
        private const val ALIGN_FRAC = 0.35f
        private const val LANDING_PAD = 0.06f
        private val LEFT_BOUND = GameConfig.WALL_THICKNESS + GameConfig.BALL_RADIUS
        private val RIGHT_BOUND = GameConfig.WIDTH - GameConfig.WALL_THICKNESS - GameConfig.BALL_RADIUS
    }
}
