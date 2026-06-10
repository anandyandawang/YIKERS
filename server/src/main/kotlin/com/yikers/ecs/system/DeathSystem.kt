package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.Physics
import com.yikers.ecs.event.Events
import com.yikers.ecs.event.LethalContact
import com.yikers.ecs.event.PlayerDied
import com.yikers.ecs.event.RunEnded
import com.yikers.ecs.resource.RunState

// Below the kill-line or lethal touch -> Dead. Run ends once none live.
class DeathSystem(
    private val runState: RunState = inject(),
    private val events: Events = inject(),
) : IteratingSystem(family { all(Controlled, Physics).none(Dead) }) {
    override fun onTickEntity(entity: Entity) {
        val ballY = entity[Physics].body.position.y
        val viewBottom = runState.scrollY // kill-line = view bottom
        if (ballY < viewBottom) kill(entity)
    }

    override fun onTick() {
        if (runState.dead) return
        events.each<LethalContact> { kill(it.victim) }
        super.onTick()
        // started gate: an empty family before anyone joined is "waiting", not dead.
        if (runState.started && family.numEntities == 0) {
            runState.dead = true
            if (runState.score > runState.highScore) {
                runState.highScore = runState.score
            }
            events.emit(RunEnded(runState.score))
        }
    }

    // Idempotent: two hazards may hit the same ball in one tick.
    private fun kill(entity: Entity) {
        if (entity.getOrNull(Dead) != null) return
        entity.configure { it += Dead() }
        events.emit(PlayerDied(entity))
    }
}
