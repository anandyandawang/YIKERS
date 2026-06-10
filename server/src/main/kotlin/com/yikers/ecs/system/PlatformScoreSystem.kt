package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.event.Events
import com.yikers.ecs.event.PlatformCleared
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState

// Primary climber passes a slab -> cleared + score (gain scales with slabs skipped).
class PlatformScoreSystem(
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
    private val refs: Refs = inject(),
    private val events: Events = inject(),
) : IteratingSystem(family { all(PlatformC) }) {
    override fun onTick() {
        if (runState.dead) return
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val p = entity[PlatformC]
        if (p.cleared) return
        val primary = refs.player ?: return
        val primaryY = primary[Physics].body.position.y
        if (primaryY <= p.y + GameConfig.PLATFORM_HEIGHT) return

        p.cleared = true
        val gained = if (runState.lastPlatformY == 0f) {
            1
        } else {
            ((p.y - runState.lastPlatformY) / GameConfig.PLATFORM_INTERVALS).toInt().coerceAtLeast(1)
        }
        val scored = gained * cfg.scoreScale
        runState.score += scored
        runState.lastPlatformY = p.y
        events.emit(PlatformCleared(entity, scored))
    }
}
