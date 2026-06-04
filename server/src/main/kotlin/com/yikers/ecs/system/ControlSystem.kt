package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.RunConfig
import com.yikers.control.ControlContext
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Intent
import com.yikers.ecs.component.Physics
import com.yikers.ecs.resource.RunState

// Per-climber control: each entity's Controller decides its move this frame and we
// record it as Intent -- nothing more. Every player is driven by a RelayController
// now (a client's relayed InputCommand); the server no longer knows or cares
// whether that client is a human or a bot. Mechanic systems (MoveSystem,
// JumpSystem, ...) read Intent downstream and enact it, so ControlSystem makes no
// physics writes. ctx carries the climber's own state for the relay seam.
class ControlSystem(
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Controlled, Physics, FootSensor, Intent).none(Dead) }) {
    private val ctx = ControlContext()

    override fun onTickEntity(entity: Entity) {
        if (runState.dead) return
        val body = entity[Physics].body

        ctx.playerX = body.position.x
        ctx.playerY = body.position.y
        ctx.grounded = entity[FootSensor].contacts > 0
        ctx.speed = cfg.horizontalSpeed
        ctx.jumpVelocity = cfg.jumpVelocity

        val move = entity[Controlled].controller.decide(ctx)
        val intent = entity[Intent]
        intent.vx = move.vx
        intent.jump = move.jump
    }
}
