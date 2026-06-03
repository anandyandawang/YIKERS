package com.yikers.ecs.component

// Root of the augment catalog + the capability contracts. Concrete augments live
// one-per-file (e.g. DoubleJump.kt) -- a sealed type only needs the same PACKAGE,
// not the same file -- so this file never holds the catalog. It keeps only the
// sealed root, the capability traits, and their folds, all bounded by the number
// of EFFECT KINDS, not by how many augments exist.
sealed interface Augment

// Capability traits. An augment implements only the ones it grants, so no augment
// carries fields it does not use. New effect kind = new trait (+ a fold below,
// and a query in whichever mechanic system consumes it).
interface GrantsAirJumps {
    val extraAirJumps: Int
}

// Capability folds: turn a climber's owned augments into one derived number that
// the mechanic systems read. One per capability, so this list tracks effect kinds
// (handful), not the catalog.
val Augments.airJumpBudget: Int
    get() = owned.filterIsInstance<GrantsAirJumps>().sumOf { it.extraAirJumps }
