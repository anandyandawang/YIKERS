package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.BotController
import com.yikers.control.BotView
import com.yikers.control.ControlContext
import com.yikers.ecs.component.BoulderC
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Intent
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.resource.RunState
import kotlin.math.abs

// Per-climber control: each entity's Controller decides its move this frame and
// we record it as Intent -- nothing more. This is the sole port to the
// Controllers (human input / bot AI); mechanic systems (MoveSystem, JumpSystem,
// ...) read Intent downstream and enact it, so ControlSystem makes no physics
// writes and holds no per-augment branches. ctx carries the climber's own state;
// a bot's world percept goes into its BotView.
class ControlSystem(
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Controlled, Physics, FootSensor, Intent).none(Dead) }) {
    private val platforms = family { all(PlatformC) }
    private val boulders = family { all(BoulderC, Physics) }
    private val ctx = ControlContext()
    // gravityScale is fixed for a run, so cache the m/s^2 magnitude once.
    private val gravityPxS2 = abs(GameConfig.GRAVITY * cfg.gravityScale)

    override fun onTickEntity(entity: Entity) {
        if (runState.dead || runState.paused) return
        val body = entity[Physics].body
        val grounded = entity[FootSensor].contacts > 0

        ctx.playerX = body.position.x
        ctx.playerY = body.position.y
        ctx.grounded = grounded
        ctx.speed = cfg.horizontalSpeed
        ctx.jumpVelocity = cfg.jumpVelocity

        val controller = entity[Controlled].controller
        if (controller is BotController) {
            controller.view.playerVy = body.linearVelocity.y
            fillView(controller.view, ctx.playerX, ctx.playerY)
        }

        val move = controller.decide(ctx)
        val intent = entity[Intent]
        intent.vx = move.vx
        intent.jump = move.jump
    }

    // Project the world into the bot's percept: the two lowest holes above it,
    // the slab just below it (its landing surface), distance to the killing
    // floor, gravity, and a snapshot of every boulder. Fallbacks keep the old
    // behavior: no platform above -> width 0 (bot hops); none below -> ground.
    private fun fillView(view: BotView, px: Float, py: Float) {
        var firstY = Float.MAX_VALUE
        var firstCx = GameConfig.WIDTH / 2f
        var firstW = 0f
        var secondY = Float.MAX_VALUE
        var secondCx = GameConfig.WIDTH / 2f
        var secondW = 0f
        var supY = -Float.MAX_VALUE   // highest platform at/below the ball
        var supCx = GameConfig.WIDTH / 2f
        var supW = 0f
        platforms.forEach { e ->
            val p = e[PlatformC]
            // A bridged platform is solid in the physics world, so the bot treats
            // it as having no hole. Key off bridged (not cleared): with several
            // climbers a hole stays open — and passable — until the last one is up.
            val holeW = if (p.bridged) 0f else p.holeWidth
            val cx = if (p.bridged) GameConfig.WIDTH / 2f else p.holeX + p.holeWidth / 2f
            if (p.y > py) {
                if (p.y < firstY) {
                    secondY = firstY; secondCx = firstCx; secondW = firstW
                    firstY = p.y; firstCx = cx; firstW = holeW
                } else if (p.y < secondY) {
                    secondY = p.y; secondCx = cx; secondW = holeW
                }
            } else if (p.y > supY) {
                supY = p.y; supCx = cx; supW = holeW
            }
        }
        view.targetHoleCenterX = firstCx
        view.targetHoleWidth = firstW
        view.targetPlatformY =
            if (firstY == Float.MAX_VALUE) py + GameConfig.PLATFORM_INTERVALS else firstY
        view.nextHoleCenterX = secondCx
        view.nextHoleWidth = if (secondY == Float.MAX_VALUE) 0f else secondW
        view.supportHoleCenterX = supCx
        view.supportHoleWidth = if (supY == -Float.MAX_VALUE) 0f else supW

        view.distToKillLine = py - runState.scrollY
        view.gravityPxS2 = gravityPxS2

        var n = 0
        boulders.forEach { e ->
            if (n >= view.boulderX.size) return@forEach
            val b = e[Physics].body
            view.boulderX[n] = b.position.x
            view.boulderY[n] = b.position.y
            view.boulderVx[n] = b.linearVelocity.x
            view.boulderVy[n] = b.linearVelocity.y
            n++
        }
        view.boulderCount = n
    }
}
