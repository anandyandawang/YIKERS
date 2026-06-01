package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.Physics
import com.yikers.ecs.resource.RunState

// Per-climber death: a climber that falls below the rising camera or touches a
// hazard is marked Dead. The run ends only once no living climber remains.
class DeathSystem(
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Controlled, Physics).none(Dead) }) {
    override fun onTickEntity(entity: Entity) {
        val ballY = entity[Physics].body.position.y
        val camBottom = runState.scrollY - GameConfig.HEIGHT / 2f
        if (entity in runState.lethalHits || ballY < camBottom) {
            entity.configure { it += Dead() }
        }
    }

    override fun onTick() {
        if (runState.dead) return
        super.onTick() // mark fallen/hit climbers Dead, dropping them from family
        if (family.numEntities == 0) {
            runState.dead = true
            if (runState.score > runState.highScore) {
                runState.highScore = runState.score
                Prefs.highScore = runState.score
            }
        }
    }
}
