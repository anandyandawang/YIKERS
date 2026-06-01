package com.yikers.ecs.system

import com.badlogic.gdx.graphics.OrthographicCamera
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.M2P
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.ecs.component.Physics
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState

// Death when the ball falls below the rising camera or hits a lethal hazard.
class DeathSystem(
    private val cam: OrthographicCamera = inject(),
    private val runState: RunState = inject(),
    private val refs: Refs = inject(),
) : IntervalSystem() {
    override fun onTick() {
        if (runState.dead) return
        val player = refs.player ?: return
        val ballY = player[Physics].body.position.y * M2P
        val camBottom = cam.position.y - GameConfig.HEIGHT / 2f
        if (runState.lethalHit || ballY < camBottom) {
            runState.dead = true
            if (runState.score > runState.highScore) {
                runState.highScore = runState.score
                Prefs.highScore = runState.score
            }
        }
    }
}
