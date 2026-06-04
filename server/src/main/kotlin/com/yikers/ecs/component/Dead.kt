package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class Dead : Component<Dead> {
    override fun type() = Dead
    companion object : ComponentType<Dead>()
}
