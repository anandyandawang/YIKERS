package com.yikers.ecs.component

// First augment. One extra mid-air jump. Owning it makes ControlSystem allow a
// second jump while airborne (see Augments.airJumpBudget). New augments follow
// this shape: a data object/class implementing Augment + the capability traits
// for whatever it grants, in its own file.
data object DoubleJump : Augment, GrantsAirJumps {
    override val extraAirJumps = 1
}
