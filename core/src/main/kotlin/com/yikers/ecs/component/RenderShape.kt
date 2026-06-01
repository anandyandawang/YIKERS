package com.yikers.ecs.component

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

enum class ShapeKind { CIRCLE, RECT }

// How to draw the entity (asset-free shapes).
data class RenderShape(
    val kind: ShapeKind,
    val color: Color,
) : Component<RenderShape> {
    override fun type() = RenderShape
    companion object : ComponentType<RenderShape>()
}
