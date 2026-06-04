package com.yikers.support

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.bot.BotBrain
import com.yikers.bot.BotSelf
import com.yikers.bot.BotView
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.ControlContext
import com.yikers.control.Controller
import com.yikers.control.Move
import com.yikers.ecs.component.BoulderC
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Intent
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.component.Physics
import com.yikers.ecs.resource.RunState
import kotlin.math.abs

// Test autopilot: drives the headless sim through the ECS into the shared BotBrain,
// keeping the algorithm single-sourced with production.
class AutopilotController : Controller {
    val self = BotSelf()
    val view = BotView()

    // Unused: AutopilotSystem writes Intent directly. Satisfies the Controller seam.
    override fun decide(ctx: ControlContext): Move = Move(0f, false)
}

// Fills each autopilot climber's BotView from the ECS, writes BotBrain's decision.
class AutopilotSystem(
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Controlled, Physics, FootSensor, Intent).none(Dead) }) {
    private val platforms = family { all(PlatformC) }
    private val boulders = family { all(BoulderC, Physics) }
    private val brain = BotBrain()
    private val gravityPxS2 = abs(GameConfig.GRAVITY * cfg.gravityScale)

    override fun onTickEntity(entity: Entity) {
        if (runState.dead) return
        val controller = entity[Controlled].controller as? AutopilotController ?: return
        val body = entity[Physics].body
        val self = controller.self
        self.x = body.position.x
        self.y = body.position.y
        self.vy = body.linearVelocity.y
        self.grounded = entity[FootSensor].contacts > 0
        self.speed = cfg.horizontalSpeed
        self.jumpVelocity = cfg.jumpVelocity
        fillView(controller.view, self.x, self.y)

        val move = brain.decide(self, controller.view)
        val intent = entity[Intent]
        intent.vx = move.vx
        intent.jump = move.jump
    }

    private fun fillView(view: BotView, px: Float, py: Float) {
        var firstY = Float.MAX_VALUE
        var firstCx = GameConfig.WIDTH / 2f
        var firstW = 0f
        var secondY = Float.MAX_VALUE
        var secondCx = GameConfig.WIDTH / 2f
        var secondW = 0f
        var supY = -Float.MAX_VALUE
        var supCx = GameConfig.WIDTH / 2f
        var supW = 0f
        platforms.forEach { e ->
            val p = e[PlatformC]
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
