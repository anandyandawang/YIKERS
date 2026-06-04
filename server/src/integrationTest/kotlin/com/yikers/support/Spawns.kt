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

// A slab at height y with a hole spanning [holeX, holeX + holeWidth]: builds both
// solid halves, the PlatformC entity, and the body -> entity userData wiring the
// contact listener follows. Mirrors EntityFactory.spawnPlatform minus the random
// hole. Collapses the copy-pasted slab setup the bridging tests used to repeat.
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

// A bare dynamic climber ball (Physics + Player) centered at (x, y). No foot
// sensor / controller: for PlatformSystem tests that poke touchedBy by hand.
fun World.spawnTestClimber(pw: PhysicsWorld, x: Float, y: Float): Entity {
    val b = pw.body {
        type = BodyDef.BodyType.DynamicBody
        position.set(x, y)
        circle(radius = GameConfig.BALL_RADIUS) {}
    }
    return entity {
        it += Physics(b)
        it += Player()
    }
}
