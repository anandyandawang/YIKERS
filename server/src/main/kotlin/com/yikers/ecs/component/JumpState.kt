package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class JumpState(
    var airJumpsUsed: Int = 0,
) : Component<JumpState> {
    override fun type() = JumpState
    companion object : ComponentType<JumpState>()
}
