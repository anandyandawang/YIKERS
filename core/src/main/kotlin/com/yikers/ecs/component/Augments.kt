package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Which augments a climber owns + jump runtime. Combos = several in the set.
// owned = capability; airJumpsUsed = transient runtime, ControlSystem zeroes it
// every grounded frame. Empty owned = inert.
data class Augments(
    val owned: MutableSet<Augment> = mutableSetOf(),
    var airJumpsUsed: Int = 0,
) : Component<Augments> {
    override fun type() = Augments
    companion object : ComponentType<Augments>()
}
