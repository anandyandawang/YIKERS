package com.yikers.net

// One human player's intent for a single tick. Pure data: resolved on the client
// from keys/tilt/touch, then relayed across the GameSession seam. No Gdx types, so
// it is already wire-ready for a future socket transport.
data class InputCommand(
    val playerId: Int,
    val vx: Float,
    val jump: Boolean,
)
