package com.yikers.ecs.component.augment

// One extra mid-air jump. Template: a data object implementing Augment + traits.
data object DoubleJump : Augment, GrantsAirJumps {
    override val id = "double_jump"
    override val displayName = "Double Jump"
    override val desc = "jump again in mid-air"
    override val extraAirJumps = 1
}
