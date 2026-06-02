package com.yikers.ecs.component

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

// Set by the contact listener when this climber's ball touches a Lethal body.
// DeathSystem reads it next tick and marks the climber Dead. A mutable flag
// (not a side set) so the listener stays structural-change-free during the
// Box2D step, matching the FootSensor.contacts pattern.
//
// TODO: when a real damage source ships, generalize to HitPoints(hp: Float,
// max: Float). A DamageSystem drains hp over time for entities in a hazard;
// DeathSystem then checks hp <= 0 instead of a one-shot bool.
data class LethalHit(var hit: Boolean = false) : Component<LethalHit> {
    override fun type() = LethalHit
    companion object : ComponentType<LethalHit>()
}
