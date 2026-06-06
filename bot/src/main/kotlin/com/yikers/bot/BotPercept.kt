package com.yikers.bot

import com.yikers.config.GameConfig

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

    // Other players: obstacles too (they collide), but climbers -> measured velocity.
    var otherCount = 0
    val otherX = FloatArray(MAX_OTHER_PLAYERS)
    val otherY = FloatArray(MAX_OTHER_PLAYERS)
    val otherVx = FloatArray(MAX_OTHER_PLAYERS)
    val otherVy = FloatArray(MAX_OTHER_PLAYERS)
}

data class BotMove(val vx: Float, val jump: Boolean)
