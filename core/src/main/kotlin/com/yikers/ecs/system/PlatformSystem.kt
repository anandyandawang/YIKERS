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
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// Score on platform clear, then bridge its hole shut so the player can't fall
// back through; recycle platforms that scrolled below the kill-line and randomly
// drop a boulder on them.
class PlatformSystem(
    private val pw: PhysicsWorld = inject(),
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
    private val refs: Refs = inject(),
) : IteratingSystem(family { all(PlatformC) }) {
    override fun onTickEntity(entity: Entity) {
        if (runState.dead) return
        val p = entity[PlatformC]
        val player = refs.player ?: return
        val ballY = player[Physics].body.position.y

        if (!p.cleared && ballY > p.y + GameConfig.PLATFORM_HEIGHT) {
            p.cleared = true
            val gained = if (runState.lastPlatformY == 0f) {
                1
            } else {
                ((p.y - runState.lastPlatformY) / GameConfig.PLATFORM_INTERVALS).toInt().coerceAtLeast(1)
            }
            runState.score += gained * cfg.scoreScale
            runState.lastPlatformY = p.y
            runState.startCamera = true
        }

        // Cleared platforms close their hole (matches YIKES): snap the physics
        // solid once, then ease the rendered gap shut.
        if (p.cleared) closeHole(entity, p)

        val viewBottom = runState.scrollY - GameConfig.HEIGHT / 2f
        if (p.y + GameConfig.PLATFORM_HEIGHT < viewBottom) {
            recycle(entity, p, p.y + GameConfig.PLATFORM_INTERVALS * GameConfig.NUM_PLATFORMS)
            if (MathUtils.random() < cfg.boulderSpawnChance) dropBoulder(p)
        }
    }

    // One-shot extends the right half over the gap so the platform is solid; the
    // rendered holeWidth then eases to 0 for the "sliding shut" look.
    private fun closeHole(entity: Entity, p: PlatformC) {
        if (!p.bridged) {
            pw.destroyBody(p.rightBody)
            val right = buildPlatformHalf(pw, p.holeX, GameConfig.WIDTH, p.y)
            right.userData = entity
            p.rightBody = right
            p.bridged = true
        }
        if (p.holeWidth > 0f) {
            p.holeWidth -= p.holeWidth * (GameConfig.PLATFORM_CLOSE_SPEED * deltaTime).coerceAtMost(1f)
            if (p.holeWidth < 0.02f) p.holeWidth = 0f
        }
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
