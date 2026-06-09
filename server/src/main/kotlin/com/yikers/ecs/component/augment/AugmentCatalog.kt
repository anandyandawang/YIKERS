package com.yikers.ecs.component.augment

// Every augment that can be offered. Add new augments to `all` and they flow into
// the acquisition offer with no other change.
object AugmentCatalog {
    const val MAX_OWNED = 5
    const val OFFER_SIZE = 3

    val all: List<Augment> = listOf(DoubleJump)

    fun byId(id: String): Augment? = all.firstOrNull { it.id == id }
}
