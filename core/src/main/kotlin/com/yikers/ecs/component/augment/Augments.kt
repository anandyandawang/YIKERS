package com.yikers.ecs.component.augment

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Which augments a climber owns. Combos = several in the set. Pure capability
// list -- mechanic runtime (e.g. the jump's airJumpsUsed) lives in that
// mechanic's own component (JumpState), never here, so Augments never accretes
// per-mechanic fields. Empty owned = inert.
data class Augments(
    val owned: MutableSet<Augment> = mutableSetOf(),
) : Component<Augments> {
    override fun type() = Augments
    companion object : ComponentType<Augments>() {
        // Max augments a climber may own at once. A draft pick at this cap must
        // swap one out (DraftOverlay enforces it). Lives here: it's a rule of the
        // owned set, not of the draft that happens to apply it.
        const val MAX = 5
    }
}
