package com.yikers.ecs.component.augment

// Root of the augment catalog + the capability contracts. Concrete augments live
// one-per-file in this package (e.g. DoubleJump.kt); a sealed type only needs the
// same PACKAGE, not the same file. This file holds only the sealed root and the
// capability traits -- bounded by the number of EFFECT KINDS, not by catalog size.
//
// Rule: an augment is pure DATA (the traits it implements + their params). The
// mechanic that honors a capability lives in a system (or the contact listener)
// and queries owned.filterIsInstance<Trait>(); no system names a concrete
// augment, and augment files never touch the engine.
sealed interface Augment

// Grants extra mid-air jumps. ControlSystem sums extraAirJumps over owned.
interface GrantsAirJumps {
    val extraAirJumps: Int
}
