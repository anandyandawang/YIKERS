package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.P2M
import com.yikers.config.GameConfig
import com.yikers.ecs.component.BoulderC
import com.yikers.ecs.component.Physics
import com.yikers.ecs.resource.RunState

// Boulders roll, bounce off the side walls.
class BoulderSystem(
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(BoulderC, Physics) }) {
    override fun onTickEntity(entity: Entity) {
        if (runState.dead) return
        val body = entity[Physics].body
        val r = GameConfig.BOULDER_RADIUS * P2M
        val leftBound = GameConfig.WALL_THICKNESS * P2M + r
        val rightBound = (GameConfig.WIDTH - GameConfig.WALL_THICKNESS) * P2M - r
        val v = body.linearVelocity
        if (v.x < 0f && body.position.x <= leftBound) {
            body.setLinearVelocity(-v.x, v.y)
        } else if (v.x > 0f && body.position.x >= rightBound) {
            body.setLinearVelocity(-v.x, v.y)
        }
    }
}
