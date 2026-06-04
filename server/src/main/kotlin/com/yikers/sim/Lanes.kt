package com.yikers.sim

import com.yikers.config.GameConfig

// Deterministic spawn lane (ball-center X, meters) per player slot — pure function
// of the slot, so it works with dynamic join. Slot 0 = center, then alternate
// outward (1 right, 2 left, 3 right, ...) at >diameter spacing so climbers never
// spawn overlapping. Clamped to the play area for large counts.
fun laneX(slot: Int): Float {
    val r = GameConfig.BALL_RADIUS
    val minCx = GameConfig.WALL_THICKNESS + r
    val maxCx = GameConfig.WIDTH - GameConfig.WALL_THICKNESS - r
    val spacing = r * 2f + LANE_GAP
    val step = (slot + 1) / 2                       // 0,1,1,2,2,3,3,...
    val dir = if (slot % 2 == 0) -1f else 1f        // slot 0: step 0, dir moot
    return (GameConfig.WIDTH / 2f + dir * step * spacing).coerceIn(minCx, maxCx)
}

private const val LANE_GAP = 0.12f   // gap between adjacent spawn balls, meters
