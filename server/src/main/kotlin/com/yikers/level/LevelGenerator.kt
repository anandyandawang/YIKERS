package com.yikers.level

// ALL layout randomness lives behind this seam: swap the generator for a new
// biome/difficulty curve, sim systems stay untouched.
interface LevelGenerator {
    fun nextPlatform(y: Float): PlatformSpec

    // null = no boulder on this slab.
    fun boulderOnRecycle(platformY: Float): BoulderSpec?
}

data class PlatformSpec(val holeX: Float, val holeWidth: Float)

data class BoulderSpec(val x: Float, val speed: Float)
