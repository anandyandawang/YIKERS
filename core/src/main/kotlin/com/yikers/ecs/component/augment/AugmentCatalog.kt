package com.yikers.ecs.component.augment

import com.badlogic.gdx.math.MathUtils

// The draftable augment catalog -- every augment the acquisition draft can offer.
// Explicit list (not reflection) keeps it RoboVM-safe and the order stable: when
// you add an augment file in this package, add its object here too. The draft
// offers any a climber doesn't already own, so a catalog of one is fine -- it just
// offers fewer cards until the catalog grows.
val augmentCatalog: List<Augment> = listOf(
    DoubleJump,
)

// Display name for the draft UI, derived from the type name so a new augment needs
// no extra field (a data object reports its own name, e.g. DoubleJump).
val Augment.label: String get() = this::class.simpleName ?: "augment"

// Roll up to [size] augments the climber doesn't already own, in random order.
// Empty when the catalog is exhausted. MathUtils so a seeded run reproduces offers.
fun rollAugmentOffer(owned: Set<Augment>, size: Int): List<Augment> {
    val pool = augmentCatalog.filterNot { it in owned }.toMutableList()
    val picks = ArrayList<Augment>(size)
    repeat(minOf(size, pool.size)) { picks += pool.removeAt(MathUtils.random(pool.size - 1)) }
    return picks
}
