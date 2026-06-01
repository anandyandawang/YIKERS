package com.yikers.config

// Per-run feel + spawn knobs. MUTABLE so items/events/characters tweak live.
// [SEAM] roguelike layer mutates this; every system reads feel numbers here.
data class RunConfig(
    var jumpVelocity: Float = 50f,        // Box2D m/s on jump
    var horizontalSpeed: Float = 20f,     // |x-vel| while arrow held (old tilt feel)
    var gravityScale: Float = 1f,
    // YIKES stepped Box2D one 1/300s tick per render frame, so at 60fps its sim
    // ran at 60/300 = 0.2x realtime — that floaty feel. We step a fixed timestep
    // decoupled from fps, so to match the original we scale sim time by 0.2.
    var simTimeScale: Float = 0.2f,
    var doubleJumpEnabled: Boolean = false,
    var maxAirJumps: Int = 0,
    var boulderSpawnChance: Float = 1f,   // 0..1 on platform recycle
    var boulderSpeedMin: Float = 20f,
    var boulderSpeedMax: Float = 30f,
    var scrollAccelFactor: Float = 4f,
    var scoreScale: Int = 1,
)
