package com.yikers.ecs.component

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Render data in px. position = body center. Synced from Box2D each tick.
data class Transform(
    val position: Vector2 = Vector2(),
    val size: Vector2 = Vector2(),
    var rotation: Float = 0f,
) : Component<Transform> {
    override fun type() = Transform
    companion object : ComponentType<Transform>()
}
