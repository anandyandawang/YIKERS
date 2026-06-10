package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.RunConfig
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Intent
import com.yikers.ecs.component.JumpState
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.component.augment.GrantsAirJumps
import com.yikers.ecs.component.augment.GrantsJumpBoost
import com.yikers.ecs.component.augment.with
import com.yikers.ecs.resource.RunState

// Ground + air jumps from Intent + Augments. Air jumps gate on GrantsAirJumps;
// GrantsJumpBoost augments scale launch velocity multiplicatively.
class JumpSystem(
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Controlled, Physics, FootSensor, Intent, Augments, JumpState).none(Dead) }) {
    override fun onTickEntity(entity: Entity) {
        if (runState.dead) return
        val body = entity[Physics].body
        val grounded = entity[FootSensor].contacts > 0
        val jumpState = entity[JumpState]
        if (grounded) jumpState.airJumpsUsed = 0

        if (!entity[Intent].jump) return
        val jumpVelocity = cfg.jumpVelocity * entity[Augments].with<GrantsJumpBoost>()
            .fold(1f) { acc, t -> acc * t.jumpVelocityMultiplier }
        if (grounded) {
            body.setLinearVelocity(body.linearVelocity.x, jumpVelocity)
        } else {
            val airJumps = entity[Augments].with<GrantsAirJumps>().sumOf { it.extraAirJumps }
            if (jumpState.airJumpsUsed < airJumps) {
                body.setLinearVelocity(body.linearVelocity.x, jumpVelocity)
                jumpState.airJumpsUsed++
            }
        }
    }
}
