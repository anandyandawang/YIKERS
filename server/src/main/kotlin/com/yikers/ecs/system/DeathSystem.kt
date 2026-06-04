package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.LethalHit
import com.yikers.ecs.component.Physics
import com.yikers.ecs.resource.RunState

// Below the kill-line or hazard-hit -> Dead. Run ends once none live.
class DeathSystem(
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Controlled, Physics, LethalHit).none(Dead) }) {
    override fun onTickEntity(entity: Entity) {
        val ballY = entity[Physics].body.position.y
        val viewBottom = runState.scrollY // kill-line = view bottom
        if (entity[LethalHit].hit || ballY < viewBottom) {
            entity.configure { it += Dead() }
        }
    }

    override fun onTick() {
        if (runState.dead) return
        super.onTick()
        // started gate: an empty family before anyone joined is "waiting", not dead.
        if (runState.started && family.numEntities == 0) {
            runState.dead = true
            if (runState.score > runState.highScore) {
                runState.highScore = runState.score
            }
        }
    }
}
