package com.yikers.ecs.component.augment

// Root of the augment catalog + the capability contracts. Concrete augments live
// one-per-file in this package (e.g. DoubleJump.kt); a sealed type only needs the
// same PACKAGE, not the same file. This file holds only the sealed root and the
// capability traits -- bounded by the number of EFFECT KINDS, not by catalog size.
//
// Rule: an augment is pure DATA (the traits it implements + their params). The
// mechanic that honors a capability lives in a system (or the contact listener)
// and queries augments.with<Trait>(); no system names a concrete augment, and
// augment files never touch the engine.
sealed interface Augment

// Grants extra mid-air jumps. JumpSystem sums extraAirJumps over the owned ones.
interface GrantsAirJumps {
    val extraAirJumps: Int
}

// Owned augments implementing capability trait T. Lets a mechanic system pull
// just the augments it cares about, e.g. augments.with<GrantsAirJumps>().
inline fun <reified T> Augments.with(): List<T> = owned.filterIsInstance<T>()
