package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.yikers.ecs.component.augment.Augment

// A climber's open augment offer: up to Draft.OFFER_SIZE unowned augments to choose
// from. Present == this climber still has to pick; removed once resolved (took an
// augment, swapped, or skipped). pendingPick holds a chosen augment waiting for a
// swap target, set only when the climber is already at the augment cap.
data class DraftOffer(
    val options: List<Augment>,
    var pendingPick: Augment? = null,
) : Component<DraftOffer> {
    override fun type() = DraftOffer
    companion object : ComponentType<DraftOffer>()
}
