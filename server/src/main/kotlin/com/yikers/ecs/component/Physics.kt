package com.yikers.ecs.component

import com.badlogic.gdx.physics.box2d.Body
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Link entity -> Box2D body.
data class Physics(val body: Body) : Component<Physics> {
    override fun type() = Physics
    companion object : ComponentType<Physics>()
}
