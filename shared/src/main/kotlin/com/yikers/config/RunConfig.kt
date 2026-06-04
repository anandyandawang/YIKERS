package com.yikers.config

import kotlinx.serialization.Serializable

// Per-run feel + spawn knobs. MUTABLE. [SEAM] roguelike layer mutates this.
@Serializable
data class RunConfig(
    // FEEL SCALE: YIKES ran 0.2x realtime. Pre-scale its numbers: velocities x0.2,
    // accelerations x0.04. Any new velocity/accel must use this scale.
    var jumpVelocity: Float = 10f,        // YIKES 50 * 0.2
    var horizontalSpeed: Float = 4f,      // YIKES 20 * 0.2; key speed + tilt accelX scale
    var gravityScale: Float = 1f,
    var boulderSpawnChance: Float = 1f,   // 0..1 on platform recycle
    var boulderSpeedMin: Float = 4f,      // YIKES 20 * 0.2
    var boulderSpeedMax: Float = 6f,      // YIKES 30 * 0.2
    var scrollAccelFactor: Float = 240f,  // scroll scale; px->m fold lives in GameConfig.SCALING_FACTOR
    var scoreScale: Int = 1,
)
