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
import com.yikers.ecs.component.augment.with
import com.yikers.ecs.resource.RunState

// Owns the whole jump mechanic: ground jump + air jumps. Reads the per-frame
// Intent (from ControlSystem) and the climber's Augments. Air jumps are a
// parameter of jumping (same vy, different gate), so they live here, not in a
// separate system. Jump augments contribute via the GrantsAirJumps trait --
// adding one (e.g. a higher-tier TripleJump) needs no change here.
class JumpSystem(
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Controlled, Physics, FootSensor, Intent, Augments, JumpState).none(Dead) }) {
    override fun onTickEntity(entity: Entity) {
        if (runState.dead || runState.paused) return
        val body = entity[Physics].body
        val grounded = entity[FootSensor].contacts > 0
        val jumpState = entity[JumpState]
        if (grounded) jumpState.airJumpsUsed = 0

        if (!entity[Intent].jump) return
        if (grounded) {
            body.setLinearVelocity(body.linearVelocity.x, cfg.jumpVelocity)
        } else {
            // air jump: spend one if owned augments grant any (e.g. DoubleJump)
            val airJumps = entity[Augments].with<GrantsAirJumps>().sumOf { it.extraAirJumps }
            if (jumpState.airJumpsUsed < airJumps) {
                body.setLinearVelocity(body.linearVelocity.x, cfg.jumpVelocity)
                jumpState.airJumpsUsed++
            }
        }
    }
}
