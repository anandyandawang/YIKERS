package com.yikers.bot

import com.yikers.config.GameConfig
import kotlin.math.abs

// Cap on other players the bot tracks (>= maxPlayers - 1 on the default 8-seat server).
private const val MAX_OTHER_PLAYERS = 8

class BotSelf {
    var x = 0f
    var y = 0f
    var vy = 0f            // m/s, up = +
    var grounded = false
    var speed = 0f         // horizontalSpeed
    var jumpVelocity = 0f
}

// One family of moving obstacles: positions + key-matched frame-diff velocities.
// A teleport (recycle/respawn step > MAX_PLAUSIBLE_STEP) clamps velocity to zero.
class ObstacleTrack(cap: Int) {
    var count = 0
        private set
    val x = FloatArray(cap)
    val y = FloatArray(cap)
    val vx = FloatArray(cap)
    val vy = FloatArray(cap)

    private val prevX = HashMap<Int, Float>()
    private val prevY = HashMap<Int, Float>()
    private val nextX = HashMap<Int, Float>()
    private val nextY = HashMap<Int, Float>()

    fun begin() {
        count = 0
        nextX.clear()
        nextY.clear()
    }

    fun add(key: Int, sx: Float, sy: Float, advanced: Boolean, frameDt: Float) {
        nextX[key] = sx
        nextY[key] = sy
        if (count >= x.size) return
        x[count] = sx
        y[count] = sy
        if (advanced) {
            val px = prevX[key]
            val py = prevY[key]
            if (frameDt > 0f && px != null && py != null) {
                val teleported = abs(sx - px) > MAX_PLAUSIBLE_STEP || abs(sy - py) > MAX_PLAUSIBLE_STEP
                vx[count] = if (teleported) 0f else (sx - px) / frameDt
                vy[count] = if (teleported) 0f else (sy - py) / frameDt
            } else {
                vx[count] = 0f
                vy[count] = 0f
            }
        }
        count++
    }

    fun end(advanced: Boolean) {
        if (!advanced) return
        prevX.clear(); prevX.putAll(nextX)
        prevY.clear(); prevY.putAll(nextY)
    }

    private companion object {
        const val MAX_PLAUSIBLE_STEP = 0.5f // m/frame; past this = a teleport
    }
}

// Bot percept: holes above, support slab below, kill-line, moving obstacles.
class BotView {
    var targetHoleCenterX = 0f
    var targetHoleWidth = 0f     // 0 => no platform above
    var targetPlatformY = 0f
    var nextHoleCenterX = 0f
    var nextHoleWidth = 0f
    var supportHoleCenterX = 0f
    var supportHoleWidth = 0f    // 0 => ground / no hole below
    var distToKillLine = 0f      // playerY - killLine
    var gravityPxS2 = 0f         // positive magnitude

    val boulders = ObstacleTrack(GameConfig.NUM_PLATFORMS)
    // Other players: obstacles too (they collide), but climbers -> measured velocity.
    val others = ObstacleTrack(MAX_OTHER_PLAYERS)
}

data class BotMove(val vx: Float, val jump: Boolean)
