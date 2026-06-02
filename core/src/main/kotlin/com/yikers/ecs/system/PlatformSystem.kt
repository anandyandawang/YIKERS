package com.yikers.ecs.system

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.ecs.buildPlatformHalf
import com.yikers.ecs.component.BoulderC
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.component.Player
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// Score on the primary climber's platform clears. Bridge a hole shut only once
// EVERY living climber has cleared it, so none is sealed underneath. Recycle
// platforms that scrolled below the kill-line and randomly drop a boulder on them.
class PlatformSystem(
    private val pw: PhysicsWorld = inject(),
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
    private val refs: Refs = inject(),
) : IteratingSystem(family { all(PlatformC) }) {
    // Every living climber (mirror of ControlSystem's percept set).
    private val livePlayers = family { all(Player, Physics).none(Dead) }
    private var lowestLiveY = Float.MAX_VALUE

    override fun onTick() {
        if (runState.dead) return
        // Lowest living climber drives hole-closing: a platform stays open until
        // even the last one is above it.
        lowestLiveY = Float.MAX_VALUE
        livePlayers.forEach { e ->
            val y = e[Physics].body.position.y
            if (y < lowestLiveY) lowestLiveY = y
        }
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val p = entity[PlatformC]

        // Scoring tracks the primary climber only.
        val primary = refs.player
        if (primary != null && !p.cleared) {
            val primaryY = primary[Physics].body.position.y
            if (primaryY > p.y + GameConfig.PLATFORM_HEIGHT) {
                p.cleared = true
                val gained = if (runState.lastPlatformY == 0f) {
                    1
                } else {
                    ((p.y - runState.lastPlatformY) / GameConfig.PLATFORM_INTERVALS).toInt().coerceAtLeast(1)
                }
                runState.score += gained * cfg.scoreScale
                runState.lastPlatformY = p.y
            }
        }

        // Close only once EVERY living climber has cleared it (matches YIKES'
        // bridge), so none is sealed below: snap the physics solid once, then ease
        // the rendered gap shut from both ends.
        if (!p.bridged && lowestLiveY != Float.MAX_VALUE &&
            lowestLiveY > p.y + GameConfig.PLATFORM_HEIGHT
        ) {
            bridge(entity, p)
        }
        if (p.bridged) easeGap(p)

        val viewBottom = runState.scrollY - GameConfig.HEIGHT / 2f
        if (p.y + GameConfig.PLATFORM_HEIGHT < viewBottom) {
            recycle(entity, p, p.y + GameConfig.PLATFORM_INTERVALS * GameConfig.NUM_PLATFORMS)
            if (MathUtils.random() < cfg.boulderSpawnChance) dropBoulder(p)
        }
    }

    // One-shot: collapse both halves to the hole's centre so the platform reads
    // solid. The meeting point is where the rendered both-ends tween lands too, so
    // physics and render end up consistent — physics just gets there instantly.
    private fun bridge(entity: Entity, p: PlatformC) {
        val center = p.holeX + p.holeWidth / 2f
        pw.destroyBody(p.leftBody)
        pw.destroyBody(p.rightBody)
        val left = buildPlatformHalf(pw, 0f, center, p.y)
        val right = buildPlatformHalf(pw, center, GameConfig.WIDTH, p.y)
        left.userData = entity
        right.userData = entity
        p.leftBody = left
        p.rightBody = right
        p.bridged = true
    }

    // Ease the rendered gap shut from both ends (halves grow toward its center).
    private fun easeGap(p: PlatformC) {
        if (p.holeWidth <= 0f) return
        val center = p.holeX + p.holeWidth / 2f
        p.holeWidth -= p.holeWidth * (GameConfig.PLATFORM_CLOSE_SPEED * deltaTime).coerceAtMost(1f)
        if (p.holeWidth < 0.02f) p.holeWidth = 0f
        p.holeX = center - p.holeWidth / 2f
    }

    private fun recycle(entity: Entity, p: PlatformC, newY: Float) {
        p.y = newY
        p.cleared = false
        p.bridged = false
        p.holeWidth = MathUtils.random(GameConfig.PLATFORM_HOLE_MIN, GameConfig.PLATFORM_HOLE_MAX)
        p.holeX = MathUtils.random(
            GameConfig.PLATFORM_EDGE_MIN,
            GameConfig.WIDTH - p.holeWidth - GameConfig.PLATFORM_EDGE_MIN,
        )
        // fixtures can't resize, so rebuild the two halves (safe: runs after step).
        pw.destroyBody(p.leftBody)
        pw.destroyBody(p.rightBody)
        val left = buildPlatformHalf(pw, 0f, p.holeX, newY)
        val right = buildPlatformHalf(pw, p.holeX + p.holeWidth, GameConfig.WIDTH, newY)
        left.userData = entity
        right.userData = entity
        p.leftBody = left
        p.rightBody = right
    }

    private fun dropBoulder(p: PlatformC) {
        if (refs.boulders.isEmpty()) return
        val be = refs.boulders[refs.nextBoulder % refs.boulders.size]
        refs.nextBoulder++
        val body = be[Physics].body
        val r = GameConfig.BOULDER_RADIUS
        val x = MathUtils.random(GameConfig.WALL_THICKNESS + r, GameConfig.WIDTH - GameConfig.WALL_THICKNESS - r)
        val y = p.y + GameConfig.PLATFORM_HEIGHT + r + 0.04f
        body.setTransform(x, y, 0f)
        val speed = MathUtils.random(cfg.boulderSpeedMin, cfg.boulderSpeedMax) *
            (if (MathUtils.randomBoolean()) 1f else -1f)
        body.setLinearVelocity(speed, 0f)
        be[BoulderC].speed = speed
    }
}
