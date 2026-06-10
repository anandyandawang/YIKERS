package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.component.Player
import com.yikers.ecs.rebuildPlatformBodies
import com.yikers.ecs.resource.RunState
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// Slop so a climber resting on top still reads as "above" the slab.
private const val BRIDGE_CLEARANCE = 0.05f

// Seal only once every living climber landed AND is above now. Landed ->
// halves snap at rest height (no eject); above is live so a fall re-opens it.
class PlatformBridgeSystem(
    private val pw: PhysicsWorld = inject(),
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(PlatformC) }) {
    private val livePlayers = family { all(Player, Physics).none(Dead) }

    override fun onTick() {
        if (runState.dead) return
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val p = entity[PlatformC]
        if (!p.bridged && allLiveClimbersClear(p)) bridge(entity, p)
        if (p.bridged) easeGap(p)
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
        rebuildPlatformBodies(pw, entity, p, holeX = center, holeWidth = 0f)
        p.bridged = true
    }

    private fun easeGap(p: PlatformC) {
        if (p.holeWidth <= 0f) return
        val center = p.holeX + p.holeWidth / 2f
        p.holeWidth -= p.holeWidth * (GameConfig.PLATFORM_CLOSE_SPEED * deltaTime).coerceAtMost(1f)
        if (p.holeWidth < 0.02f) p.holeWidth = 0f
        p.holeX = center - p.holeWidth / 2f
    }
}
