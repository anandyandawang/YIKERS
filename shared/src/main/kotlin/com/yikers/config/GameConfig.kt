package com.yikers.config

// Fixed game constants. Sizes in meters. Tweaked by playtest, not by run.
object GameConfig {
    const val WIDTH = 4.8f
    const val HEIGHT = 8.0f
    const val PPM = 100f                 // px per meter; HUD + window design scale
    const val WIDTH_PX = WIDTH * PPM     // 480 px design width
    const val HEIGHT_PX = HEIGHT * PPM   // 800 px design height
    const val TITLE = "YIKERS"

    const val PLATFORM_INTERVALS = 1.9f
    const val NUM_PLATFORMS = 6

    const val SIM_HZ = 60               // fixed sim tick rate; 1 snapshot tick = 1/SIM_HZ s

    const val GRAVITY = -20f            // realtime gravity (m/s^2); YIKES -500 ran at 0.2x sim (-500 * 0.2^2)
    const val SCALING_FACTOR = 0.0017f  // ball-derived scroll scale (from YIKES); px->m /100 folded in

    // entity sizes (meters) — replace YIKES texture-derived sizes. TUNE for feel.
    const val BALL_RADIUS = 0.24f
    const val FOOT_WIDTH = 0.30f
    const val FOOT_HEIGHT = 0.08f
    const val PLATFORM_HEIGHT = 0.24f
    const val PLATFORM_HOLE_MIN = 1.2f
    const val PLATFORM_HOLE_MAX = 1.8f
    const val PLATFORM_EDGE_MIN = 0.20f  // keep both halves wider than this
    const val PLATFORM_CLOSE_SPEED = 6f  // gap ease-shut rate /s (~YIKES /10 per frame at 60fps)
    const val WALL_THICKNESS = 0.16f
    const val GROUND_HEIGHT = 0.40f
    const val BOULDER_RADIUS = 0.36f    // 1.5x ball, matches YIKES Ø75/Ø50 ratio
}
