package com.yikers.ecs.component

import com.badlogic.gdx.physics.box2d.Body
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity

data class PlatformC(
    var leftBody: Body,
    var rightBody: Body,
    var y: Float,
    var holeX: Float,
    var holeWidth: Float,
    var cleared: Boolean = false,
    var bridged: Boolean = false,
) : Component<PlatformC> {
    // Climbers that foot-landed on this slab. List NOT Set: Fleks' Entity hashCode
    // crashes on RoboVM, so no Entity may enter a hash structure.
    val touchedBy: MutableList<Entity> = mutableListOf()

    override fun type() = PlatformC
    companion object : ComponentType<PlatformC>()
}
