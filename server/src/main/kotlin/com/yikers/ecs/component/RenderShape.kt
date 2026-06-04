package com.yikers.ecs.component

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.yikers.net.ShapeKind

// How to draw the entity (asset-free shapes). ShapeKind lives in shared (net) so
// the client's snapshot renderer reads the same enum.
data class RenderShape(
    val kind: ShapeKind,
    val color: Color,
) : Component<RenderShape> {
    override fun type() = RenderShape
    companion object : ComponentType<RenderShape>()
}
