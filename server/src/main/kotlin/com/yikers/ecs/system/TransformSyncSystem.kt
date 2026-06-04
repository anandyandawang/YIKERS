package com.yikers.ecs.system

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.yikers.config.GameConfig
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.Transform

class TransformSyncSystem :
    IteratingSystem(family { all(Physics, Transform) }) {
    override fun onTickEntity(entity: Entity) {
        val body = entity[Physics].body
        val t = entity[Transform]
        t.position.set(body.position.x, body.position.y)
        t.rotation = body.angle * MathUtils.radiansToDegrees

        entity.getOrNull(FootSensor)?.let { fs ->
            fs.footBody.setTransform(
                body.position.x,
                body.position.y - GameConfig.BALL_RADIUS,
                0f,
            )
        }
    }
}
