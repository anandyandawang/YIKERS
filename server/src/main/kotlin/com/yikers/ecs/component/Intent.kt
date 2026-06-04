package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class Intent(
    var vx: Float = 0f,
    var jump: Boolean = false,
) : Component<Intent> {
    override fun type() = Intent
    companion object : ComponentType<Intent>()
}
