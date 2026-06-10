package com.yikers.ecs.component.augment

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// A pending pick-one choice on a climber. Present = offer overlay open client-side;
// stays across ticks until AugmentChoiceSystem resolves (pick/skip) and removes it.
data class AugmentOffer(val options: List<Augment>) : Component<AugmentOffer> {
    override fun type() = AugmentOffer
    companion object : ComponentType<AugmentOffer>()
}
