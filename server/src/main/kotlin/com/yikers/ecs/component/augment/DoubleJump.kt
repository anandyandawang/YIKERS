package com.yikers.ecs.component.augment

// One extra mid-air jump. Template: a data object implementing Augment + traits.
data object DoubleJump : Augment, GrantsAirJumps {
    override val extraAirJumps = 1
}
