package com.yikers.bot

import com.yikers.config.GameConfig

// What a bot reads about ITSELF (proprioception), rebuilt client-side from
// snapshots. Mirrors the server's old ControlContext, but the bot no longer runs
// on the server — it's an ordinary client, so it derives this from the wire.
class BotSelf {
    var x = 0f             // m, ball center
    var y = 0f             // m, ball center
    var vy = 0f            // m/s, signed (up = +); the bot's own climb/fall
    var grounded = false   // inferred: resting on a solid top with ~0 vertical speed
    var speed = 0f         // m/s (horizontalSpeed)
    var jumpVelocity = 0f  // m/s
}

// The bot's "eyes": the slice of world state it steers off, the client-side analog
// of the server's old BotView. Same fields (so the decision logic is unchanged);
// only the filler moved from ECS projection to snapshot reconstruction. Boulder
// arrays are sized once to the pool and reused.
class BotView {
    var targetHoleCenterX = 0f   // m, center of the hole in the next platform up
    var targetHoleWidth = 0f     // m; 0 => no platform above (just hop)
    var targetPlatformY = 0f     // m, Y of that platform
    var nextHoleCenterX = 0f     // m, hole one platform further up (lookahead)
    var nextHoleWidth = 0f       // m; 0 => unknown
    // The slab just below the ball — the surface it must land on. 0 width => the
    // ground (solid everywhere), so there is no hole to fall back through.
    var supportHoleCenterX = 0f  // m
    var supportHoleWidth = 0f    // m; 0 => ground / no hole below
    var distToKillLine = 0f      // m, playerY - killLine; small => near death
    var gravityPxS2 = 0f         // m/s^2, positive magnitude
    var boulderCount = 0
    val boulderX = FloatArray(GameConfig.NUM_PLATFORMS)   // m, center
    val boulderY = FloatArray(GameConfig.NUM_PLATFORMS)   // m, center
    val boulderVx = FloatArray(GameConfig.NUM_PLATFORMS)  // m/s, signed
    val boulderVy = FloatArray(GameConfig.NUM_PLATFORMS)  // m/s, signed
}

// The bot's intended action for one frame — packed into an InputCommand by BotClient.
data class BotMove(val vx: Float, val jump: Boolean)
