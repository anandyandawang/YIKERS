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

// Slop so a climber resting on top still reads as "above" the slab.
private const val BRIDGE_CLEARANCE = 0.05f

class PlatformSystem(
    private val pw: PhysicsWorld = inject(),
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
    private val refs: Refs = inject(),
) : IteratingSystem(family { all(PlatformC) }) {
    private val livePlayers = family { all(Player, Physics).none(Dead) }

    override fun onTick() {
        if (runState.dead) return
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val p = entity[PlatformC]

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

        // Seal only once every living climber landed AND is above now. Landed ->
        // halves snap at rest height (no eject); above is live so a fall re-opens it.
        if (!p.bridged && allLiveClimbersClear(p)) bridge(entity, p)
        if (p.bridged) easeGap(p)

        val viewBottom = runState.scrollY // kill-line = view bottom
        if (p.y + GameConfig.PLATFORM_HEIGHT < viewBottom) {
            recycle(entity, p, p.y + GameConfig.PLATFORM_INTERVALS * GameConfig.NUM_PLATFORMS)
            if (MathUtils.random() < cfg.boulderSpawnChance) dropBoulder(p)
        }
    }

    private fun allLiveClimbersClear(p: PlatformC): Boolean {
        val top = p.y + GameConfig.PLATFORM_HEIGHT
        var any = false
        var allClear = true
        livePlayers.forEach { e ->
            any = true
            val landed = e in p.touchedBy
            val above = e[Physics].body.position.y - GameConfig.BALL_RADIUS > top - BRIDGE_CLEARANCE
            if (!landed || !above) allClear = false
        }
        return any && allClear
    }

    // Collapse both halves to the hole center -> reads solid.
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
        p.touchedBy.clear() // fresh slab: nobody landed yet
        p.holeWidth = MathUtils.random(GameConfig.PLATFORM_HOLE_MIN, GameConfig.PLATFORM_HOLE_MAX)
        p.holeX = MathUtils.random(
            GameConfig.PLATFORM_EDGE_MIN,
            GameConfig.WIDTH - p.holeWidth - GameConfig.PLATFORM_EDGE_MIN,
        )
        // fixtures can't resize -> rebuild both halves (safe: after step).
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
