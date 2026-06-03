package com.yikers.ecs.component.augment

// First augment. One extra mid-air jump. Owning it makes ControlSystem allow a
// second jump while airborne. New augments follow this shape: a data object/class
// implementing Augment + the capability traits for whatever it grants, in its own
// file in this package.
data object DoubleJump : Augment, GrantsAirJumps {
    override val extraAirJumps = 1
}
