package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.M2P
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.ControlContext
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.resource.RunState

// Per-climber control: each entity's Controller decides its move this frame.
// Humans read input, bots read world state. Original feel preserved: held arrow
// = horizontal velocity (keeps x-momentum on jump), jump gated on foot contact.
class ControlSystem(
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Controlled, Physics, FootSensor).none(Dead) }) {
    private val platforms = family { all(PlatformC) }
    private val ctx = ControlContext()

    override fun onTickEntity(entity: Entity) {
        if (runState.dead) return
        val body = entity[Physics].body
        val grounded = entity[FootSensor].contacts > 0

        ctx.playerX = body.position.x * M2P
        ctx.playerY = body.position.y * M2P
        ctx.grounded = grounded
        ctx.speed = cfg.horizontalSpeed
        ctx.jumpVelocity = cfg.jumpVelocity
        fillTarget(ctx)

        val move = entity[Controlled].controller.decide(ctx)
        body.setLinearVelocity(move.vx, body.linearVelocity.y)
        if (move.jump && grounded) {
            body.setLinearVelocity(body.linearVelocity.x, cfg.jumpVelocity)
        }
    }

    // The gap in the lowest platform above the climber -> ctx target fields.
    // Fallback: screen center, width 0 (so a bot with no platform above hops).
    private fun fillTarget(ctx: ControlContext) {
        var bestY = Float.MAX_VALUE
        var bestCenter = GameConfig.WIDTH / 2f
        var bestWidth = 0f
        platforms.forEach { e ->
            val p = e[PlatformC]
            if (p.y > ctx.playerY && p.y < bestY) {
                bestY = p.y
                bestCenter = p.holeX + p.holeWidth / 2f
                bestWidth = p.holeWidth
            }
        }
        ctx.targetHoleCenterX = bestCenter
        ctx.targetHoleWidth = bestWidth
    }
}
