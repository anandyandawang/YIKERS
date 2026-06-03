package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.Intent
import com.yikers.ecs.component.Physics
import com.yikers.ecs.resource.RunState

// Enacts the horizontal slice of Intent: sets the body's x-velocity, preserving
// vy (gravity + JumpSystem own the vertical axis). Base locomotion doer -- reads
// Intent, never input, so a human, a bot, or a script all move the same way.
class MoveSystem(
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Physics, Intent).none(Dead) }) {
    override fun onTickEntity(entity: Entity) {
        if (runState.dead || runState.paused) return
        val body = entity[Physics].body
        body.setLinearVelocity(entity[Intent].vx, body.linearVelocity.y)
    }
}
