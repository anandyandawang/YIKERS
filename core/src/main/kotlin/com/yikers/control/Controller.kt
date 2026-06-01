package com.yikers.control

// One climber's intended action for a single frame.
data class Move(val vx: Float, val jump: Boolean)

// Per-tick snapshot of one climber's own state (proprioception), filled by the
// ControlSystem and handed to its Controller. Generic on purpose: both humans
// and bots may read it. World sensing the bot needs (holes, boulders, camera)
// lives in its own BotView, not here, so the shared interface stays lean.
class ControlContext {
    var playerX = 0f          // m, ball center
    var playerY = 0f          // m, ball center
    var grounded = false
    var speed = 0f            // m/s (horizontalSpeed)
    var jumpVelocity = 0f     // m/s
}

// Drives one climber. Human impl reads Gdx.input; bot impl reads world state.
// One Controller instance per climber, so X players + Y bots can coexist.
interface Controller {
    fun decide(ctx: ControlContext): Move
}
