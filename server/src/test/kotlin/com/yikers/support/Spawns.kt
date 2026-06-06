package com.yikers.support

import com.badlogic.gdx.physics.box2d.BodyDef
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.yikers.config.GameConfig
import com.yikers.ecs.buildPlatformHalf
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.component.Player
import ktx.box2d.body
import ktx.box2d.circle
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// A slab with a fixed hole + body->entity userData. Like spawnPlatform, no RNG.
fun World.spawnTestPlatform(
    pw: PhysicsWorld,
    y: Float,
    holeX: Float,
    holeWidth: Float,
): Entity {
    val left = buildPlatformHalf(pw, 0f, holeX, y)
    val right = buildPlatformHalf(pw, holeX + holeWidth, GameConfig.WIDTH, y)
    val plat = entity { it += PlatformC(left, right, y, holeX, holeWidth) }
    left.userData = plat
    right.userData = plat
    return plat
}

// A bare climber ball (Physics + Player); no foot sensor, for hand-poked tests.
fun World.spawnTestClimber(pw: PhysicsWorld, x: Float, y: Float): Entity {
    val b = pw.body {
        type = BodyDef.BodyType.DynamicBody
        position.set(x, y)
        circle(radius = GameConfig.BALL_RADIUS) {}
    }
    return entity {
        it += Physics(b)
        it += Player(slot = 0)
    }
}
