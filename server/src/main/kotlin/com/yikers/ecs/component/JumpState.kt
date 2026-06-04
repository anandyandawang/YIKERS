package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Per-climber jump runtime. airJumpsUsed = mid-air jumps spent since last
// grounded; JumpSystem zeroes it on landing and spends it against the air-jump
// budget from the climber's augments. Lives here, not on Augments -- Augments is
// the generic capability list, so each mechanic keeps its own runtime.
data class JumpState(
    var airJumpsUsed: Int = 0,
) : Component<JumpState> {
    override fun type() = JumpState
    companion object : ComponentType<JumpState>()
}
