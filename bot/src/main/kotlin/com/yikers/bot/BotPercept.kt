package com.yikers.bot

import com.yikers.config.GameConfig

class BotSelf {
    var x = 0f
    var y = 0f
    var vy = 0f            // m/s, up = +
    var grounded = false
    var speed = 0f         // horizontalSpeed
    var jumpVelocity = 0f
}

// Bot percept: holes above, support slab below, kill-line, boulders.
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
    var boulderCount = 0
    val boulderX = FloatArray(GameConfig.NUM_PLATFORMS)
    val boulderY = FloatArray(GameConfig.NUM_PLATFORMS)
    val boulderVx = FloatArray(GameConfig.NUM_PLATFORMS)
    val boulderVy = FloatArray(GameConfig.NUM_PLATFORMS)
}

data class BotMove(val vx: Float, val jump: Boolean)
