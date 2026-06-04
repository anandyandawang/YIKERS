package com.yikers.ecs.component

import com.badlogic.gdx.physics.box2d.Body
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity

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
    // Living climbers that have foot-touched (landed on) this slab. The contact
    // listener appends here; PlatformSystem bridges the hole shut once every living
    // climber is present. A List (linear contains), NOT a Set: Fleks' Entity
    // hashCode crashes on RoboVM (see commit history), so no Entity may enter a
    // hash structure. Cleared by recycle when the slab is reused higher up.
    val touchedBy: MutableList<Entity> = mutableListOf()

    override fun type() = PlatformC
    companion object : ComponentType<PlatformC>()
}
