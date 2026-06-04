package com.yikers.ecs.system

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.ecs.resource.Arena
import com.yikers.ecs.resource.RunState

// Keep the side walls centered on the visible view (kill-line + half view-height)
// so the ball can't escape sideways and boulders always have walls to bounce off.
// Walls are built tall enough (see buildArena) to span any device's view height.
class WallFollowSystem(
    private val arena: Arena = inject(),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    override fun onTick() {
        val y = runState.scrollY + runState.viewHeight / 2f
        arena.leftWall.setTransform(arena.leftWall.position.x, y, 0f)
        arena.rightWall.setTransform(arena.rightWall.position.x, y, 0f)
    }
}
