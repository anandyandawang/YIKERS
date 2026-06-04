package com.yikers.bot

import com.yikers.config.GameConfig
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

// Autopilot decision logic. Climbs by hopping up through each platform's gap and
// landing on the slab's solid top (not falling straight back through the hole),
// then repeats — also boulder-aware: dodges rolling boulders, won't jump into a
// hole a boulder is about to occupy, pre-aims at the hole after next, and climbs
// flat-out when the rising floor gets close. Reads self-state from `self`, the
// world from `view`. Pure (no engine deps) so it runs anywhere a client runs.
//
// This is the SAME algorithm the server used to run for bots; it just moved
// client-side. A bot is now a client (BotClient) that feeds this percept from
// snapshots and ships the result as an InputCommand.
class BotBrain {
    fun decide(self: BotSelf, v: BotView): BotMove {
        val deadzone = GameConfig.BALL_RADIUS * DEADZONE_FRAC
        val dx = v.targetHoleCenterX - self.x
        val aligned = v.targetHoleWidth <= 0f ||
            abs(dx) <= maxOf(deadzone, v.targetHoleWidth * ALIGN_FRAC)

        // Airborne: pure climbing control — rise through the gap, but once past
        // the apex steer onto solid so we land instead of dropping back through.
        if (!self.grounded) return BotMove(airborneSteer(self, v, deadzone), jump = false)

        // Grounded. Jump arc math runs in meters.
        val jumpPx = self.jumpVelocity
        val jumpSafe = aligned && jumpIsSafe(self, v, jumpPx, v.gravityPxS2)

        // 1. Kill-line pressure: punch through the hole now, accept boulder risk.
        if (v.distToKillLine < PANIC_DIST_PX) {
            return BotMove(steer(dx, deadzone, self.speed), jump = aligned)
        }

        // 2. Dodge the nearest boulder closing on our lane (still hop if safe).
        val threatVx = dodgeVx(self, v)
        if (threatVx != null) return BotMove(threatVx, jump = jumpSafe)

        // 3. Climb: steer to the hole; when idle-aligned, pre-aim at the next one.
        val vx = if (abs(dx) > deadzone) steer(dx, deadzone, self.speed) else driftVx(self, v, deadzone)
        return BotMove(vx, jump = jumpSafe)
    }

    // Full-speed toward a target offset, stopping inside the deadzone.
    private fun steer(offset: Float, deadzone: Float, speed: Float): Float = when {
        offset > deadzone -> speed
        offset < -deadzone -> -speed
        else -> 0f
    }

    // Airborne steering. While this jump can still reach the next gap, head for
    // it to pass through. Once it can't (apex won't clear the target slab — i.e.
    // we've gone as high as this hop goes), commit to landing on the slab just
    // below: aim at the hole above if that already lands us on solid, else divert
    // to the nearest solid strip so we don't drop back through the hole we came
    // up. Deciding by reachable-apex (not by falling/rising) leaves the whole
    // apex dwell to slide onto solid, which a descending-only check misses.
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

    // Aligned & grounded with no threat -> drift toward the hole-after-next so we
    // land already lined up, instead of dead-stopping in the deadzone.
    private fun driftVx(self: BotSelf, v: BotView, deadzone: Float): Float {
        if (v.nextHoleWidth <= 0f) return 0f
        return steer(v.nextHoleCenterX - self.x, deadzone, self.speed)
    }

    // Nearest boulder on our lane that is closing within the reaction horizon ->
    // step away from it (flip if that side is against a wall). null = no threat.
    private fun dodgeVx(self: BotSelf, v: BotView): Float? {
        var bestTtc = Float.MAX_VALUE
        var bestVx = 0f
        for (i in 0 until v.boulderCount) {
            if (abs(self.y - v.boulderY[i]) >= DANGER_BAND_PX) continue
            val rx = v.boulderX[i] - self.x
            val bvx = v.boulderVx[i]
            if (rx * bvx >= 0f) continue                  // not moving toward us
            if (abs(rx) >= DANGER_RADIUS_PX * 3f) continue
            val ttc = abs(rx) / maxOf(0.01f, abs(bvx))
            if (ttc <= REACT_HORIZON_S && ttc < bestTtc) {
                bestTtc = ttc
                bestVx = bvx
            }
        }
        if (bestTtc == Float.MAX_VALUE) return null
        var dir = -sign(bestVx)                           // step opposite its travel
        if (dir == 0f) dir = 1f
        if (dir < 0f && self.x - LEFT_BOUND_PX < DANGER_RADIUS_PX) dir = 1f
        if (dir > 0f && RIGHT_BOUND_PX - self.x < DANGER_RADIUS_PX) dir = -1f
        return dir * self.speed
    }

    // false if a boulder will sit in/over the target hole when we arrive, or
    // would intersect us during the ascent.
    private fun jumpIsSafe(self: BotSelf, v: BotView, jumpPx: Float, g: Float): Boolean {
        if (v.boulderCount == 0 || g <= 0f) return true
        val dh = (v.targetPlatformY - self.y).coerceAtLeast(0f)
        val disc = jumpPx * jumpPx - 2f * g * dh
        val arrivalT = if (disc <= 0f) jumpPx / g else (jumpPx - sqrt(disc)) / g  // first crossing, else apex
        if (arrivalT <= 0f) return true
        for (i in 0 until v.boulderCount) {
            // (a) boulder in/over the hole at arrival?
            if (v.targetHoleWidth > 0f &&
                abs(v.boulderY[i] - v.targetPlatformY) < DANGER_BAND_PX &&
                abs(projX(v.boulderX[i], v.boulderVx[i], arrivalT) - v.targetHoleCenterX) <
                v.targetHoleWidth / 2f + GameConfig.BOULDER_RADIUS + JUMP_BOULDER_PAD
            ) return false
            // (b) collision during ascent (sample points along the arc).
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

    // Boulder x after time t, reflected into the wall-bounce span via a
    // triangle-wave fold (exact for any number of bounces).
    private fun projX(x0: Float, vx: Float, t: Float): Float {
        val span = B_RIGHT_BOUND_PX - B_LEFT_BOUND_PX
        if (span <= 0f) return x0
        val period = 2f * span
        var m = (x0 + vx * t - B_LEFT_BOUND_PX) % period
        if (m < 0f) m += period
        return B_LEFT_BOUND_PX + if (m <= span) m else period - m
    }

    private companion object {
        // alignment
        const val DEADZONE_FRAC = 0.5f      // * BALL_RADIUS = 0.12m: ignore tiny offsets
        const val ALIGN_FRAC = 0.35f        // hole-width fraction that counts as lined up
        const val LANDING_PAD = 0.06f       // extra m onto solid when picking a landing spot
        // boulder dodging
        const val REACT_HORIZON_S = 0.9f    // only react to boulders arriving within this
        const val DANGER_BAND_PX = 0.90f    // vertical |dy| (m) a boulder can threaten on our lane
        const val DANGER_RADIUS_PX = GameConfig.BALL_RADIUS + GameConfig.BOULDER_RADIUS + 0.14f  // touch dist + slack (m)
        const val JUMP_BOULDER_PAD = 0.08f  // extra hole clearance (m) for the jump gate
        // kill-line pressure
        const val PANIC_DIST_PX = 1.30f     // floor this close (m) => prioritize climbing

        // ball-center x play-area (m)
        const val LEFT_BOUND_PX = GameConfig.WALL_THICKNESS + GameConfig.BALL_RADIUS
        const val RIGHT_BOUND_PX = GameConfig.WIDTH - GameConfig.WALL_THICKNESS - GameConfig.BALL_RADIUS
        // boulder-center wall-bounce bounds (m); must match BoulderSystem
        const val B_LEFT_BOUND_PX = GameConfig.WALL_THICKNESS + GameConfig.BOULDER_RADIUS
        const val B_RIGHT_BOUND_PX = GameConfig.WIDTH - GameConfig.WALL_THICKNESS - GameConfig.BOULDER_RADIUS
    }
}
