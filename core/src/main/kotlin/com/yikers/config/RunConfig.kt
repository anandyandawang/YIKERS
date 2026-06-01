package com.yikers.config

// Per-run feel + spawn knobs. MUTABLE so items/events/characters tweak live.
// [SEAM] roguelike layer mutates this; every system reads feel numbers here.
data class RunConfig(
    var jumpVelocity: Float = 50f,        // Box2D m/s on jump
    var horizontalSpeed: Float = 20f,     // |x-vel| while arrow held (old tilt feel)
    var gravityScale: Float = 1f,
    var doubleJumpEnabled: Boolean = false,
    var maxAirJumps: Int = 0,
    var boulderSpawnChance: Float = 1f,   // 0..1 on platform recycle
    var boulderSpeedMin: Float = 20f,
    var boulderSpeedMax: Float = 30f,
    var scrollAccelFactor: Float = 4f,
    var scoreScale: Int = 1,
)
