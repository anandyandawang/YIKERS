package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.yikers.control.Controller

data class Controlled(val controller: Controller) : Component<Controlled> {
    override fun type() = Controlled
    companion object : ComponentType<Controlled>()
}
