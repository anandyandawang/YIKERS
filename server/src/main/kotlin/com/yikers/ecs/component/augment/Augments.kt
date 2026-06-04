package com.yikers.ecs.component.augment

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Augments a climber owns (empty = inert). Per-mechanic runtime lives elsewhere.
data class Augments(
    val owned: MutableSet<Augment> = mutableSetOf(),
) : Component<Augments> {
    override fun type() = Augments
    companion object : ComponentType<Augments>()
}
