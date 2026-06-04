package com.yikers.ecs.component

import com.badlogic.gdx.physics.box2d.Body
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// contacts > 0 = grounded.
data class FootSensor(
    val footBody: Body,
    var contacts: Int = 0,
) : Component<FootSensor> {
    override fun type() = FootSensor
    companion object : ComponentType<FootSensor>()
}
