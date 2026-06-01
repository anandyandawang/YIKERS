package com.yikers.config

// Fixed game constants. Sizes in px. Tweaked by playtest, not by run.
object GameConfig {
    const val WIDTH = 480f
    const val HEIGHT = 800f
    const val TITLE = "YIKERS"

    const val PLATFORM_INTERVALS = 190f
    const val NUM_PLATFORMS = 6

    const val GRAVITY = -20f           // realtime gravity; YIKES -500 ran at 0.2x sim (-500 * 0.2^2)
    const val SCALING_FACTOR = 0.17f   // ball-derived scroll scale (from YIKES)

    // entity sizes (px) — replace YIKES texture-derived sizes. TUNE for feel.
    const val BALL_RADIUS = 24f
    const val FOOT_WIDTH = 30f
    const val FOOT_HEIGHT = 8f
    const val PLATFORM_HEIGHT = 24f
    const val PLATFORM_HOLE_MIN = 120f
    const val PLATFORM_HOLE_MAX = 180f
    const val PLATFORM_EDGE_MIN = 20f  // keep both halves wider than this
    const val WALL_THICKNESS = 16f
    const val GROUND_HEIGHT = 40f
    const val BOULDER_RADIUS = 36f    // 1.5x ball, matches YIKES Ø75/Ø50 ratio
}
