package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Marker: touching player = death. [SEAM] future enemies add this too.
class Lethal : Component<Lethal> {
    override fun type() = Lethal
    companion object : ComponentType<Lethal>()
}
