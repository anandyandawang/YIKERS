package com.yikers.config

import kotlinx.serialization.Serializable

// Per-run feel + spawn knobs. MUTABLE so items/events/characters tweak live.
// [SEAM] roguelike layer mutates this; every system reads feel numbers here.
@Serializable
data class RunConfig(
    // FEEL SCALE: physics runs at true 1.0x realtime. YIKES ran its sim at 0.2x
    // realtime (one 1/300s Box2D step per frame @60fps), so to keep the same
    // floaty trajectories we pre-scale the original numbers via Newton's time-
    // rescale symmetry: velocities x0.2, accelerations (gravity) x0.2^2 = 0.04.
    // Any new velocity/accel must use this same scale.
    var jumpVelocity: Float = 10f,        // YIKES 50 * 0.2
    var horizontalSpeed: Float = 4f,      // YIKES 20 * 0.2; key speed + tilt accelX scale
    var gravityScale: Float = 1f,
    var boulderSpawnChance: Float = 1f,   // 0..1 on platform recycle
    var boulderSpeedMin: Float = 4f,      // YIKES 20 * 0.2
    var boulderSpeedMax: Float = 6f,      // YIKES 30 * 0.2
    var scrollAccelFactor: Float = 240f,  // scroll scale; px->m fold lives in GameConfig.SCALING_FACTOR
    var scoreScale: Int = 1,
)
