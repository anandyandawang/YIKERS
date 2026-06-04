package com.yikers.config

// Deterministic spawn lane (ball-center X, meters) for a player slot. Pure function
// of the slot alone — no roster size needed, so it works with dynamic join. The
// server spawns slot N's ball here so climbers start spread out. Slot 0 takes
// center; the rest spread via a golden-ratio walk so lanes stay well separated for
// any count.
fun laneX(slot: Int): Float {
    val r = GameConfig.BALL_RADIUS
    val minCx = GameConfig.WALL_THICKNESS + r
    val maxCx = GameConfig.WIDTH - GameConfig.WALL_THICKNESS - r
    if (slot <= 0) return GameConfig.WIDTH / 2f
    val frac = (slot * 0.61803398875f) % 1f   // golden-ratio low-discrepancy spread
    return minCx + (maxCx - minCx) * frac
}
