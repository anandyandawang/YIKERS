package com.yikers.ecs.component.augment

import com.yikers.net.AugmentId

// Flat stat boosts. Same-trait augments stack multiplicatively (speed/jump) or
// additively (air jumps) — the honoring system folds over all owned traits.

data object AirJets : Augment, GrantsAirJumps {
    override val id = AugmentId.AIR_JETS
    override val extraAirJumps = 2
}

data object SwiftBoots : Augment, GrantsMoveSpeed {
    override val id = AugmentId.SWIFT_BOOTS
    override val moveSpeedMultiplier = 1.30f
}

data object LongStride : Augment, GrantsMoveSpeed {
    override val id = AugmentId.LONG_STRIDE
    override val moveSpeedMultiplier = 1.15f
}

data object MoonBoots : Augment, GrantsJumpBoost {
    override val id = AugmentId.MOON_BOOTS
    override val jumpVelocityMultiplier = 1.25f
}

data object SpringLegs : Augment, GrantsJumpBoost {
    override val id = AugmentId.SPRING_LEGS
    override val jumpVelocityMultiplier = 1.12f
}

data object Adrenaline : Augment, GrantsMoveSpeed, GrantsJumpBoost {
    override val id = AugmentId.ADRENALINE
    override val moveSpeedMultiplier = 1.10f
    override val jumpVelocityMultiplier = 1.08f
}
