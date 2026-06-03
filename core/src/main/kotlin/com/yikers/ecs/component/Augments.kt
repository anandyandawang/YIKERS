package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Roguelite augments a climber owns. Combos = several in the set; systems read
// membership. Each augment carries its own effect numbers (e.g. bonusAirJumps),
// so strength is per-augment and stacks per-climber -- no global knob. New
// augment = new enum entry + its own check in some system.
enum class Augment(val bonusAirJumps: Int = 0) {
    DOUBLE_JUMP(bonusAirJumps = 1),   // one extra mid-air jump
}

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
