package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.ecs.buildPlatformHalf
import com.yikers.ecs.component.BoulderC
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.event.Events
import com.yikers.ecs.event.PlatformRecycled
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.level.BoulderSpec
import com.yikers.level.LevelGenerator
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// Slab fell below the kill-line -> fresh slab NUM_PLATFORMS intervals up. The
// generator decides the new hole + whether a pooled boulder drops onto it.
class PlatformRecycleSystem(
    private val pw: PhysicsWorld = inject(),
    private val runState: RunState = inject(),
    private val refs: Refs = inject(),
    private val generator: LevelGenerator = inject(),
    private val events: Events = inject(),
) : IteratingSystem(family { all(PlatformC) }) {
    override fun onTick() {
        if (runState.dead) return
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val p = entity[PlatformC]
        val viewBottom = runState.scrollY // kill-line = view bottom
        if (p.y + GameConfig.PLATFORM_HEIGHT >= viewBottom) return

        recycle(entity, p, p.y + GameConfig.PLATFORM_INTERVALS * GameConfig.NUM_PLATFORMS)
        generator.boulderOnRecycle(p.y)?.let { dropBoulder(p, it) }
        events.emit(PlatformRecycled(entity))
    }

    private fun recycle(entity: Entity, p: PlatformC, newY: Float) {
        p.y = newY
        p.cleared = false
        p.bridged = false
        p.touchedBy.clear() // fresh slab: nobody landed yet
        val spec = generator.nextPlatform(newY)
        p.holeX = spec.holeX
        p.holeWidth = spec.holeWidth
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

    // Boulders are pooled, never spawned mid-run: reposition the oldest.
    private fun dropBoulder(p: PlatformC, spec: BoulderSpec) {
        if (refs.boulders.isEmpty()) return
        val be = refs.boulders[refs.nextBoulder % refs.boulders.size]
        refs.nextBoulder++
        val body = be[Physics].body
        val y = p.y + GameConfig.PLATFORM_HEIGHT + GameConfig.BOULDER_RADIUS + 0.04f
        body.setTransform(spec.x, y, 0f)
        body.setLinearVelocity(spec.speed, 0f)
        be[BoulderC].speed = spec.speed
    }
}
