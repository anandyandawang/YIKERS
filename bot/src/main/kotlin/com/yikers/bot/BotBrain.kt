package com.yikers.bot

import com.yikers.config.GameConfig
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

// Autopilot: hop through each gap, land on solid, repeat. Boulder-aware. Pure.
class BotBrain {
    fun decide(self: BotSelf, v: BotView): BotMove {
        val deadzone = GameConfig.BALL_RADIUS * DEADZONE_FRAC
        val dx = v.targetHoleCenterX - self.x
        val aligned = v.targetHoleWidth <= 0f ||
            abs(dx) <= maxOf(deadzone, v.targetHoleWidth * ALIGN_FRAC)

        if (!self.grounded) return BotMove(airborneSteer(self, v, deadzone), jump = false)

        val jumpPx = self.jumpVelocity
        val jumpSafe = aligned && jumpIsSafe(self, v, jumpPx, v.gravityPxS2)

        // 1. Kill-line pressure: punch through the hole now, accept boulder risk.
        if (v.distToKillLine < PANIC_DIST_PX) {
            return BotMove(steer(dx, deadzone, self.speed), jump = aligned)
        }
        // 2. Dodge the nearest boulder closing on our lane (still hop if safe).
        val threatVx = dodgeVx(self, v)
        if (threatVx != null) return BotMove(threatVx, jump = jumpSafe)
        // 3. Climb; when idle-aligned, pre-aim at the next hole.
        val vx = if (abs(dx) > deadzone) steer(dx, deadzone, self.speed) else driftVx(self, v, deadzone)
        return BotMove(vx, jump = jumpSafe)
    }

    private fun steer(offset: Float, deadzone: Float, speed: Float): Float = when {
        offset > deadzone -> speed
        offset < -deadzone -> -speed
        else -> 0f
    }

    // Head for the next gap while the hop can still reach it; once the apex won't
    // clear the slab, commit to landing on solid so we don't drop back through.
    private fun airborneSteer(self: BotSelf, v: BotView, deadzone: Float): Float {
        val goalX = v.targetHoleCenterX
        val g = v.gravityPxS2
        val remainingRise = if (self.vy > 0f && g > 0f) self.vy * self.vy / (2f * g) else 0f
        val apexY = self.y + remainingRise
        val canReachTarget = v.targetHoleWidth > 0f &&
            apexY >= v.targetPlatformY + GameConfig.PLATFORM_HEIGHT
        if (canReachTarget || v.supportHoleWidth <= 0f) {
            return steer(goalX - self.x, deadzone, self.speed)
        }
        val r = GameConfig.BALL_RADIUS
        val overHole = abs(goalX - v.supportHoleCenterX) < v.supportHoleWidth / 2f + r + LANDING_PAD
        val landX = if (!overHole) goalX else {
            val leftLand = v.supportHoleCenterX - v.supportHoleWidth / 2f - r - LANDING_PAD
            val rightLand = v.supportHoleCenterX + v.supportHoleWidth / 2f + r + LANDING_PAD
            val leftOk = leftLand >= LEFT_BOUND_PX
            val rightOk = rightLand <= RIGHT_BOUND_PX
            when {
                leftOk && rightOk -> if (abs(leftLand - goalX) <= abs(rightLand - goalX)) leftLand else rightLand
                leftOk -> leftLand
                rightOk -> rightLand
                else -> goalX
            }.coerceIn(LEFT_BOUND_PX, RIGHT_BOUND_PX)
        }
        return steer(landX - self.x, deadzone, self.speed)
    }

    private fun driftVx(self: BotSelf, v: BotView, deadzone: Float): Float {
        if (v.nextHoleWidth <= 0f) return 0f
        return steer(v.nextHoleCenterX - self.x, deadzone, self.speed)
    }

    // Nearest boulder closing on our lane -> step away (flip at a wall). null = none.
    private fun dodgeVx(self: BotSelf, v: BotView): Float? {
        var bestTtc = Float.MAX_VALUE
        var bestVx = 0f
        for (i in 0 until v.boulderCount) {
            if (abs(self.y - v.boulderY[i]) >= DANGER_BAND_PX) continue
            val rx = v.boulderX[i] - self.x
            val bvx = v.boulderVx[i]
            if (rx * bvx >= 0f) continue
            if (abs(rx) >= DANGER_RADIUS_PX * 3f) continue
            val ttc = abs(rx) / maxOf(0.01f, abs(bvx))
            if (ttc <= REACT_HORIZON_S && ttc < bestTtc) {
                bestTtc = ttc
                bestVx = bvx
            }
        }
        if (bestTtc == Float.MAX_VALUE) return null
        var dir = -sign(bestVx)
        if (dir == 0f) dir = 1f
        if (dir < 0f && self.x - LEFT_BOUND_PX < DANGER_RADIUS_PX) dir = 1f
        if (dir > 0f && RIGHT_BOUND_PX - self.x < DANGER_RADIUS_PX) dir = -1f
        return dir * self.speed
    }

    // false if a boulder will sit over the target hole on arrival, or hit us mid-ascent.
    private fun jumpIsSafe(self: BotSelf, v: BotView, jumpPx: Float, g: Float): Boolean {
        if (v.boulderCount == 0 || g <= 0f) return true
        val dh = (v.targetPlatformY - self.y).coerceAtLeast(0f)
        val disc = jumpPx * jumpPx - 2f * g * dh
        val arrivalT = if (disc <= 0f) jumpPx / g else (jumpPx - sqrt(disc)) / g
        if (arrivalT <= 0f) return true
        for (i in 0 until v.boulderCount) {
            if (v.targetHoleWidth > 0f &&
                abs(v.boulderY[i] - v.targetPlatformY) < DANGER_BAND_PX &&
                abs(projX(v.boulderX[i], v.boulderVx[i], arrivalT) - v.targetHoleCenterX) <
                v.targetHoleWidth / 2f + GameConfig.BOULDER_RADIUS + JUMP_BOULDER_PAD
            ) return false
            var s = 0.25f
            while (s <= 1.0001f) {
                val t = s * arrivalT
                val selfY = self.y + jumpPx * t - 0.5f * g * t * t
                val bx = projX(v.boulderX[i], v.boulderVx[i], t)
                val by = v.boulderY[i] + v.boulderVy[i] * t - 0.5f * g * t * t
                val ddx = self.x - bx
                val ddy = selfY - by
                if (ddx * ddx + ddy * ddy < DANGER_RADIUS_PX * DANGER_RADIUS_PX) return false
                s += 0.25f
            }
        }
        return true
    }

    // Boulder x after time t, folded into the wall-bounce span (triangle wave).
    private fun projX(x0: Float, vx: Float, t: Float): Float {
        val span = B_RIGHT_BOUND_PX - B_LEFT_BOUND_PX
        if (span <= 0f) return x0
        val period = 2f * span
        var m = (x0 + vx * t - B_LEFT_BOUND_PX) % period
        if (m < 0f) m += period
        return B_LEFT_BOUND_PX + if (m <= span) m else period - m
    }

    private companion object {
        const val DEADZONE_FRAC = 0.5f
        const val ALIGN_FRAC = 0.35f
        const val LANDING_PAD = 0.06f
        const val REACT_HORIZON_S = 0.9f
        const val DANGER_BAND_PX = 0.90f
        const val DANGER_RADIUS_PX = GameConfig.BALL_RADIUS + GameConfig.BOULDER_RADIUS + 0.14f
        const val JUMP_BOULDER_PAD = 0.08f
        const val PANIC_DIST_PX = 1.30f
        const val LEFT_BOUND_PX = GameConfig.WALL_THICKNESS + GameConfig.BALL_RADIUS
        const val RIGHT_BOUND_PX = GameConfig.WIDTH - GameConfig.WALL_THICKNESS - GameConfig.BALL_RADIUS
        const val B_LEFT_BOUND_PX = GameConfig.WALL_THICKNESS + GameConfig.BOULDER_RADIUS
        const val B_RIGHT_BOUND_PX = GameConfig.WIDTH - GameConfig.WALL_THICKNESS - GameConfig.BOULDER_RADIUS
    }
}
