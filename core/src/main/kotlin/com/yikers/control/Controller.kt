package com.yikers.control

// One climber's intended action for a single frame.
data class Move(val vx: Float, val jump: Boolean)

// Per-tick snapshot the ControlSystem fills for each climber, then hands to its
// Controller. Humans ignore most of it; bots steer off targetHoleCenterX.
class ControlContext {
    var playerX = 0f
    var playerY = 0f
    var grounded = false
    var speed = 0f
    var jumpVelocity = 0f
    var targetHoleCenterX = 0f
    var targetHoleWidth = 0f
}

// Drives one climber. Human impl reads Gdx.input; bot impl reads world state.
// One Controller instance per climber, so X players + Y bots can coexist.
interface Controller {
    fun decide(ctx: ControlContext): Move
}
