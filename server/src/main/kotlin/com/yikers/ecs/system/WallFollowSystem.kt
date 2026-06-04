package com.yikers.ecs.system

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.ecs.resource.Arena
import com.yikers.ecs.resource.RunState

// Keep the side walls centered on the kill-line (+ half a design height) so the ball
// can't escape sideways and boulders always have walls to bounce off. Walls are built
// HEIGHT*3 tall (see buildArena), so anchoring at scrollY + HEIGHT/2 spans
// [scrollY - HEIGHT, scrollY + 2*HEIGHT] = ~16m, covering any device's view above the
// kill-line. No per-client viewHeight needed — this is pure shared sim state.
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
