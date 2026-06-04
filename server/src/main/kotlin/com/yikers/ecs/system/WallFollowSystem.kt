package com.yikers.ecs.system

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.ecs.resource.Arena
import com.yikers.ecs.resource.RunState

// Re-center the HEIGHT*3-tall side walls on scrollY + HEIGHT/2 each tick, so they
// span any device's view above the kill-line. Pure shared sim state, no viewHeight.
class WallFollowSystem(
    private val arena: Arena = inject(),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    override fun onTick() {
        val y = runState.scrollY + GameConfig.HEIGHT * 0.5f
        arena.leftWall.setTransform(arena.leftWall.position.x, y, 0f)
        arena.rightWall.setTransform(arena.rightWall.position.x, y, 0f)
    }
}
