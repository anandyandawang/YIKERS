package com.yikers.net

import kotlinx.serialization.Serializable

// One human player's intent for a single tick. Pure data: resolved on the client
// from keys/tilt/touch, then relayed across the GameSession seam. No Gdx types, so
// it serializes straight to CBOR for the LAN socket transport.
@Serializable
data class InputCommand(
    val playerId: Int,
    val vx: Float,
    val jump: Boolean,
)
