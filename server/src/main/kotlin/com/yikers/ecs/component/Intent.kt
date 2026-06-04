package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Per-frame decided action for one climber: produced by ControlSystem (from the
// Controller) and read by the mechanic systems that enact it -- MoveSystem reads
// vx, JumpSystem reads jump. Splitting decide from enact lets each movement
// mechanic live in its own system. Grows additively (e.g. a dash flag).
data class Intent(
    var vx: Float = 0f,
    var jump: Boolean = false,
) : Component<Intent> {
    override fun type() = Intent
    companion object : ComponentType<Intent>()
}
