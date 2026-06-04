package com.yikers.ecs.component.augment

// Sealed root + capability traits. An augment is pure DATA; a system queries
// augments.with<Trait>() to honor a capability, never naming a concrete augment.
sealed interface Augment

interface GrantsAirJumps {
    val extraAirJumps: Int
}

inline fun <reified T> Augments.with(): List<T> = owned.filterIsInstance<T>()
