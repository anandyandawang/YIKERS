package com.yikers.ecs

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.Controller
import com.yikers.ecs.component.BoulderC
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Intent
import com.yikers.ecs.component.JumpState
import com.yikers.ecs.component.LethalHit
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.component.Player
import com.yikers.ecs.component.RenderShape
import com.yikers.ecs.component.Transform
import com.yikers.net.ShapeKind
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.resource.Arena
import com.yikers.ecs.resource.Refs
import ktx.box2d.body
import ktx.box2d.box
import ktx.box2d.circle
import ktx.box2d.filter
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

const val UD_BALL = "ball"
const val UD_FOOT = "foot"

// groupIndex shared by ball + its foot so they never collide with each other.
private const val PLAYER_GROUP: Short = -1

// Static slab [xStart, xEnd] at y. Width exact: a coerced-up width overruns into
// the visible hole -> invisible wall.
fun buildPlatformHalf(pw: PhysicsWorld, xStart: Float, xEnd: Float, y: Float): Body {
    val w = xEnd - xStart
    val h = GameConfig.PLATFORM_HEIGHT
    return pw.body {
        type = BodyDef.BodyType.StaticBody
        position.set(xStart + w / 2f, y + h / 2f)
        box(width = w, height = h) {}
    }
}

fun buildArena(pw: PhysicsWorld): Arena {
    fun staticBox(cx: Float, cy: Float, w: Float, h: Float): Body = pw.body {
        type = BodyDef.BodyType.StaticBody
        position.set(cx, cy)
        box(width = w, height = h) { friction = 0f }
    }
    val ground = staticBox(GameConfig.WIDTH / 2f, GameConfig.GROUND_HEIGHT / 2f, GameConfig.WIDTH, GameConfig.GROUND_HEIGHT)
    // Walls 3x design height; WallFollowSystem recenters them each tick.
    val wallH = GameConfig.HEIGHT * 3f
    val left = staticBox(GameConfig.WALL_THICKNESS / 2f, wallH / 2f, GameConfig.WALL_THICKNESS, wallH)
    val right = staticBox(GameConfig.WIDTH - GameConfig.WALL_THICKNESS / 2f, wallH / 2f, GameConfig.WALL_THICKNESS, wallH)
    return Arena(ground, left, right)
}

class EntityFactory(
    private val world: World,
    private val pw: PhysicsWorld,
    private val cfg: RunConfig,
    private val refs: Refs,
) {
    fun spawnPlayer(
        x: Float,
        y: Float,
        controller: Controller,
        color: Color = Color.CORAL,
        group: Short = PLAYER_GROUP,
        slot: Int = -1,
    ): Entity {
        val r = GameConfig.BALL_RADIUS
        val ballBody = pw.body {
            type = BodyDef.BodyType.DynamicBody
            position.set(x + r, y + r)
            // Never sleep: a nudged ball can doze off mid-air (Box2D stops gravity
            // for sleepers) and a vx=0 relay won't wake it -> stuck climber.
            allowSleep = false
            circle(radius = r) {
                density = 500f
                friction = 10f
                userData = UD_BALL
                filter { groupIndex = group }
            }
        }
        val footBody = pw.body {
            type = BodyDef.BodyType.DynamicBody
            gravityScale = 0f
            fixedRotation = true
            allowSleep = false
            position.set(ballBody.position.x, ballBody.position.y - r)
            box(width = GameConfig.FOOT_WIDTH, height = GameConfig.FOOT_HEIGHT) {
                isSensor = true
                userData = UD_FOOT
                filter { groupIndex = group }
            }
        }
        val entity = world.entity {
            it += Physics(ballBody)
            it += FootSensor(footBody)
            it += Transform(position = Vector2(x + r, y + r), size = Vector2(r * 2f, r * 2f))
            it += RenderShape(ShapeKind.CIRCLE, color)
            it += Controlled(controller)
            it += Intent()
            it += JumpState()
            it += LethalHit()
            it += Player(slot)
            it += Augments()              // inert: none owned yet
        }
        ballBody.userData = entity
        footBody.userData = entity
        return entity
    }

    fun spawnBoulder(x: Float, y: Float): Entity {
        val r = GameConfig.BOULDER_RADIUS
        val body = pw.body {
            type = BodyDef.BodyType.DynamicBody
            position.set(x + r, y + r)
            circle(radius = r) {
                // very high density => quasi-kinematic (boulder keeps its course).
                density = 9999f
                friction = 0f
            }
        }
        val entity = world.entity {
            it += Physics(body)
            it += Transform(position = Vector2(x + r, y + r), size = Vector2(r * 2f, r * 2f))
            it += RenderShape(ShapeKind.CIRCLE, Color.LIGHT_GRAY)
            it += BoulderC()
            // not Lethal: boulder just knocks the player around. Death = fall below.
        }
        body.userData = entity
        refs.boulders += entity
        return entity
    }

    fun spawnPlatform(y: Float): Entity {
        val holeWidth = MathUtils.random(GameConfig.PLATFORM_HOLE_MIN, GameConfig.PLATFORM_HOLE_MAX)
        val holeX = MathUtils.random(
            GameConfig.PLATFORM_EDGE_MIN,
            GameConfig.WIDTH - holeWidth - GameConfig.PLATFORM_EDGE_MIN,
        )
        val left = buildPlatformHalf(pw, 0f, holeX, y)
        val right = buildPlatformHalf(pw, holeX + holeWidth, GameConfig.WIDTH, y)
        val entity = world.entity {
            it += PlatformC(left, right, y, holeX, holeWidth)
        }
        left.userData = entity
        right.userData = entity
        return entity
    }
}
