package com.yikers.ecs.component.augment

import com.yikers.net.AugmentId

// Every augment that can roll in an offer. Add a data object here (plus its
// AugmentId in shared) and it enters the acquisition pool; AugmentChoiceSystem
// resolves wire ids back to behavior through this.
object AugmentCatalog {
    val ALL: List<Augment> = listOf(
        DoubleJump, AirJets, SwiftBoots, LongStride, MoonBoots, SpringLegs, Adrenaline,
    )

    private val byId = ALL.associateBy { it.id }

    fun byId(id: AugmentId): Augment? = byId[id]
}
