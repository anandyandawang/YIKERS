package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Marker: this climber is out. ControlSystem skips it; the run ends once no
// living climber remains.
class Dead : Component<Dead> {
    override fun type() = Dead
    companion object : ComponentType<Dead>()
}
