package com.yikers.net

import kotlinx.serialization.Serializable

// One player's intent for a tick. No Gdx types -> serializes to CBOR.
@Serializable
data class InputCommand(
    val slot: Int,
    val vx: Float,
    val jump: Boolean,
)
