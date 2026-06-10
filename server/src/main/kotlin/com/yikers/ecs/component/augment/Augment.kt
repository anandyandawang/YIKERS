package com.yikers.ecs.component.augment

import com.yikers.net.AugmentId

// Sealed root + capability traits. An augment is pure DATA; a system queries
// augments.with<Trait>() to honor a capability, never naming a concrete augment.
sealed interface Augment {
    val id: AugmentId   // wire identity (offers/snapshots); display copy lives on it
}

interface GrantsAirJumps {
    val extraAirJumps: Int
}

interface GrantsMoveSpeed {
    val moveSpeedMultiplier: Float
}

interface GrantsJumpBoost {
    val jumpVelocityMultiplier: Float
}

inline fun <reified T> Augments.with(): List<T> = owned.filterIsInstance<T>()
