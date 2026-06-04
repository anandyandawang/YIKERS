package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class BoulderC(
    var speed: Float = 0f,
) : Component<BoulderC> {
    override fun type() = BoulderC
    companion object : ComponentType<BoulderC>()
}
