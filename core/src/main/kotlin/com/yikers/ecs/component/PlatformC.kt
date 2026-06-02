package com.yikers.ecs.component

import com.badlogic.gdx.physics.box2d.Body
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Two solid halves with a gap. Bodies are var: recycle rebuilds them.
data class PlatformC(
    var leftBody: Body,
    var rightBody: Body,
    var y: Float,
    var holeX: Float,
    var holeWidth: Float,
    var cleared: Boolean = false,
    var bridged: Boolean = false, // physics gap already closed (one-shot, after clear)
) : Component<PlatformC> {
    override fun type() = PlatformC
    companion object : ComponentType<PlatformC>()
}
