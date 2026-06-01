package com.yikers.ecs.system

import com.badlogic.gdx.graphics.OrthographicCamera
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.P2M
import com.yikers.ecs.resource.Arena

// Keep the side walls centered on the camera so the ball can't escape sideways
// and boulders always have walls to bounce off.
class WallFollowSystem(
    private val cam: OrthographicCamera = inject(),
    private val arena: Arena = inject(),
) : IntervalSystem() {
    override fun onTick() {
        val y = cam.position.y * P2M
        arena.leftWall.setTransform(arena.leftWall.position.x, y, 0f)
        arena.rightWall.setTransform(arena.rightWall.position.x, y, 0f)
    }
}
