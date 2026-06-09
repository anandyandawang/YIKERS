package com.yikers.ecs.component.augment

// Sealed root + capability traits. An augment is pure DATA; a system queries
// augments.with<Trait>() to honor a capability, never naming a concrete augment.
// id/displayName/desc feed the acquisition offer UI.
sealed interface Augment {
    val id: String
    val displayName: String
    val desc: String
}

interface GrantsAirJumps {
    val extraAirJumps: Int
}

inline fun <reified T> Augments.with(): List<T> = owned.filterIsInstance<T>()
