package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Roguelite augments a climber owns. Combos = several in the set; systems read
// membership. New augment = new enum entry + its own check in some system.
enum class Augment { DOUBLE_JUMP }

// owned = capability (which augments this climber has). airJumpsUsed =
// transient runtime; ControlSystem zeroes it every grounded frame. Empty
// owned = inert.
data class Augments(
    val owned: MutableSet<Augment> = mutableSetOf(),
    var airJumpsUsed: Int = 0,
) : Component<Augments> {
    override fun type() = Augments
    companion object : ComponentType<Augments>()
}
