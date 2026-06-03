package com.yikers.ecs.component

// Roguelite augment catalog. Each augment is a sealed variant carrying only its
// own data, and implements capability traits for the effects it grants. Core
// systems query by trait (e.g. GrantsAirJumps), never by concrete augment, so
// adding an augment touches only this file -- no per-augment branches scattered
// across the mechanic systems.
sealed interface Augment

// Capability traits. An augment implements only the ones relevant to it, so no
// augment carries fields it does not use. New effect kind = new trait + a query
// in whichever mechanic system consumes it.
interface GrantsAirJumps {
    val extraAirJumps: Int
}

data object DoubleJump : Augment, GrantsAirJumps {
    override val extraAirJumps = 1
}

// Derived per-climber budgets, folded from the owned augments. Lives next to the
// catalog so the effect math stays out of the mechanic systems; ControlSystem
// just reads airJumpBudget.
val Augments.airJumpBudget: Int
    get() = owned.filterIsInstance<GrantsAirJumps>().sumOf { it.extraAirJumps }
