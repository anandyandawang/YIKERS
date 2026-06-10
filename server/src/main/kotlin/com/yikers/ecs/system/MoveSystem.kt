package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.Intent
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.component.augment.GrantsMoveSpeed
import com.yikers.ecs.component.augment.with
import com.yikers.ecs.resource.RunState

// Sets x-velocity from Intent.vx, preserving vy. Reads Intent, never input.
// GrantsMoveSpeed augments scale vx multiplicatively.
class MoveSystem(
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Physics, Intent).none(Dead) }) {
    override fun onTickEntity(entity: Entity) {
        if (runState.dead) return
        val body = entity[Physics].body
        val boost = entity.getOrNull(Augments)?.with<GrantsMoveSpeed>()
            ?.fold(1f) { acc, t -> acc * t.moveSpeedMultiplier } ?: 1f
        body.setLinearVelocity(entity[Intent].vx * boost, body.linearVelocity.y)
    }
}
