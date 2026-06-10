package com.yikers.net

import kotlinx.serialization.Serializable

// One player's intent for a tick. No Gdx types -> serializes to CBOR.
@Serializable
data class InputCommand(
    val slot: Int,
    val vx: Float,
    val jump: Boolean,
    // One-shot answer to a pending augment offer; defaults = no answer.
    val pick: AugmentId? = null,    // chosen augment from the offer
    val drop: AugmentId? = null,    // owned augment to swap out when at MAX_AUGMENTS
    val skipOffer: Boolean = false, // decline the offer
)
