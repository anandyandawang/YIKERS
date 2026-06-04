package com.yikers.control

data class Move(val vx: Float, val jump: Boolean)

// One climber's own state (proprioception), filled by ControlSystem. Bot world
// sensing lives in BotView.
class ControlContext {
    var playerX = 0f          // m, ball center
    var playerY = 0f          // m, ball center
    var grounded = false
    var speed = 0f            // m/s (horizontalSpeed); also the tilt accelX scale
    var jumpVelocity = 0f     // m/s
}

interface Controller {
    fun decide(ctx: ControlContext): Move
}
