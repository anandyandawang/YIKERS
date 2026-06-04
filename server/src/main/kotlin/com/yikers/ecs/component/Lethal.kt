package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Marker: touch = death. [SEAM] future enemies.
class Lethal : Component<Lethal> {
    override fun type() = Lethal
    companion object : ComponentType<Lethal>()
}
