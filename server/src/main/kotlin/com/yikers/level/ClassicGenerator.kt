package com.yikers.level

import com.badlogic.gdx.math.MathUtils
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig

// The original feel: uniform holes, chance-rolled boulders. Draws from
// MathUtils.random in the same order as the pre-seam code, so a seeded run
// reproduces the exact same layout.
class ClassicGenerator(private val cfg: RunConfig) : LevelGenerator {
    override fun nextPlatform(y: Float): PlatformSpec {
        val holeWidth = MathUtils.random(GameConfig.PLATFORM_HOLE_MIN, GameConfig.PLATFORM_HOLE_MAX)
        val holeX = MathUtils.random(
            GameConfig.PLATFORM_EDGE_MIN,
            GameConfig.WIDTH - holeWidth - GameConfig.PLATFORM_EDGE_MIN,
        )
        return PlatformSpec(holeX, holeWidth)
    }

    override fun boulderOnRecycle(platformY: Float): BoulderSpec? {
        if (MathUtils.random() >= cfg.boulderSpawnChance) return null
        val r = GameConfig.BOULDER_RADIUS
        val x = MathUtils.random(
            GameConfig.WALL_THICKNESS + r,
            GameConfig.WIDTH - GameConfig.WALL_THICKNESS - r,
        )
        val speed = MathUtils.random(cfg.boulderSpeedMin, cfg.boulderSpeedMax) *
            (if (MathUtils.randomBoolean()) 1f else -1f)
        return BoulderSpec(x, speed)
    }
}
