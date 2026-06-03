package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Per-frame decided action for one climber: produced by ControlSystem (from the
// Controller) and read by the mechanic systems (JumpSystem, ...). Splitting the
// decide step from the apply step lets each movement mechanic live in its own
// system. Grows additively as mechanics are added (e.g. a dash flag).
data class Intent(
    var jump: Boolean = false,
) : Component<Intent> {
    override fun type() = Intent
    companion object : ComponentType<Intent>()
}
