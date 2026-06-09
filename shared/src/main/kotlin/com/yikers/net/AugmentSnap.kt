package com.yikers.net

import kotlinx.serialization.Serializable

// One augment as the client sees it: id + display text. Used inside the AugmentOffer
// event.
@Serializable
data class AugmentSnap(val id: String, val name: String, val desc: String)
