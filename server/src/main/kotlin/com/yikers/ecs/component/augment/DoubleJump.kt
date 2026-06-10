package com.yikers.ecs.component.augment

import com.yikers.net.AugmentId

// One extra mid-air jump. Template: a data object implementing Augment + traits.
data object DoubleJump : Augment, GrantsAirJumps {
    override val id = AugmentId.DOUBLE_JUMP
    override val extraAirJumps = 1
}
