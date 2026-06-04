package com.yikers.config

// Deterministic spawn lane (ball-center X, meters) per player slot — pure function
// of the slot, so it works with dynamic join. Slot 0 = center; rest spread by a
// golden-ratio walk.
fun laneX(slot: Int): Float {
    val r = GameConfig.BALL_RADIUS
    val minCx = GameConfig.WALL_THICKNESS + r
    val maxCx = GameConfig.WIDTH - GameConfig.WALL_THICKNESS - r
    if (slot <= 0) return GameConfig.WIDTH / 2f
    val frac = (slot * 0.61803398875f) % 1f
    return minCx + (maxCx - minCx) * frac
}
