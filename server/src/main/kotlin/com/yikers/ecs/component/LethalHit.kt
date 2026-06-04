package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// A flag (not a structural change) so the contact listener stays safe mid-step.
data class LethalHit(var hit: Boolean = false) : Component<LethalHit> {
    override fun type() = LethalHit
    companion object : ComponentType<LethalHit>()
}
