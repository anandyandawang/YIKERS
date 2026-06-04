package com.yikers.ecs.component

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.yikers.net.ShapeKind

data class RenderShape(
    val kind: ShapeKind,
    val color: Color,
) : Component<RenderShape> {
    override fun type() = RenderShape
    companion object : ComponentType<RenderShape>()
}
