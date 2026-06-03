package com.yikers

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.BotController
import com.badlogic.gdx.physics.box2d.BodyDef
import com.github.quillraven.fleks.Entity
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.buildArena
import com.yikers.ecs.buildPlatformHalf
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.component.Player
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.BoulderSystem
import com.yikers.ecs.system.ControlSystem
import com.yikers.ecs.system.DeathSystem
import com.yikers.ecs.system.JumpSystem
import com.yikers.ecs.system.MoveSystem
import com.yikers.ecs.system.PhysicsStepSystem
import com.yikers.ecs.system.PlatformSystem
import com.yikers.ecs.system.ScrollSystem
import com.yikers.ecs.system.TransformSyncSystem
import com.yikers.ecs.system.WallFollowSystem
import com.yikers.physics.PlayContactListener
import ktx.box2d.body
import ktx.box2d.circle
import ktx.box2d.createWorld
import ktx.math.vec2
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// End-to-end sim smoke test, no OpenGL. Boots the same Fleks + Box2D worlds a
// real run uses, minus the GL-bound RenderSystem, drives a lone bot, and checks
// it climbs then can die. Proves the sim runs off-device (the path CI reuses for
// an eventual iOS build). World runs in meters; kill-line is RunState.scrollY.
class HeadlessRunIntegrationTest {

    // Everything a run needs that the test pokes after build.
    private class Harness(
        val physicsWorld: PhysicsWorld,
        val world: World,
        val runState: RunState,
        val refs: Refs,
    )

    // Mirror of PlayScreen.newRun() MINUS RenderSystem and the GL ShapeRenderer
    // injectable. The sim is camera-free (kill-line = RunState.scrollY), so no
    // OrthographicCamera is needed. Keep this 10-system list canonical: Gdx.gl is
    // null headless, so adding a GL system here NPEs at once.
    private fun buildHeadlessWorld(): Harness {
        val cfg = RunConfig()
        // Big highScore => DeathSystem never writes Prefs (Gdx.app file) at run-end.
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()

        val physicsWorld = createWorld(gravity = vec2(0f, GameConfig.GRAVITY * cfg.gravityScale))
        val arena = buildArena(physicsWorld)

        val world = configureWorld {
            injectables {
                add(physicsWorld)
                add(cfg)
                add(runState)
                add(arena)
                add(refs)
                // NO camera / ShapeRenderer: RenderSystem excluded, sim is camera-free.
            }
            systems {
                add(ControlSystem())
                add(MoveSystem())
                add(JumpSystem())
                add(WallFollowSystem())
                add(PhysicsStepSystem())
                add(TransformSyncSystem())
                add(BoulderSystem())
                add(PlatformSystem())
                add(ScrollSystem())
                add(DeathSystem())
                // NO RenderSystem.
            }
        }

        val factory = EntityFactory(world, physicsWorld, cfg, refs)
        // One bot as primary, same spawn path/args as PlayScreen.spawnRoster (n==1).
        val player = factory.spawnPlayer(
            x = GameConfig.WIDTH / 2f - GameConfig.BALL_RADIUS,
            y = GameConfig.GROUND_HEIGHT,
            controller = BotController(),
        )
        refs.player = player

        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnPlatform(GameConfig.GROUND_HEIGHT + i * GameConfig.PLATFORM_INTERVALS)
        }
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnBoulder(GameConfig.WIDTH / 2f - GameConfig.BOULDER_RADIUS, -3.0f - i * 0.6f)
        }

        physicsWorld.setContactListener(PlayContactListener(world))
        return Harness(physicsWorld, world, runState, refs)
    }

    // Player ball center Y, in meters.
    private fun World.playerY(h: Harness): Float =
        with(this) { h.refs.player!![Physics].body.position.y }

    @Test
    fun botClimbsAndCanDie() {
        MathUtils.random.setSeed(SEED) // reproducible platform/boulder layout

        val h = buildHeadlessWorld()
        val startY = h.world.playerY(h)

        // Climb phase: ~20s sim at 60fps; PhysicsStepSystem sub-steps at 1/300.
        // Any thrown exception fails the test.
        repeat(CLIMB_SECONDS * 60) { h.world.update(DT) }

        val climbedY = h.world.playerY(h)
        assertTrue(h.runState.score > 0) {
            "bot should clear at least one platform; score=${h.runState.score}"
        }
        assertTrue(climbedY > startY + CLIMB_MARGIN_M) {
            "bot should rise; startY=$startY climbedY=$climbedY"
        }
        assertFalse(h.runState.dead) { "should still be alive mid-climb" }

        // Forced death: raise the kill-line above the player. scrollY IS the view
        // bottom now, so DeathSystem marks the lone climber Dead -> run ends.
        h.runState.scrollY = climbedY + GameConfig.HEIGHT
        h.world.update(DT)
        assertTrue(h.runState.dead) { "raising the kill-line above the player must end the run" }

        h.world.dispose()
        h.physicsWorld.dispose()
    }

    // Multiplayer rule ("after last player"): a platform must NOT bridge while a
    // living climber still hasn't landed on it (else it'd be sealed under solid
    // floor / eject a ball straddling the hole); it closes once the last one has
    // foot contact. Both climbers sit above the slab here so only the landed flag
    // gates; the real contact-listener path is covered by footContactBridgesAfterLanding.
    @Test
    fun holeStaysOpenUntilLastClimberLands() {
        val cfg = RunConfig()
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()
        val pw = createWorld(gravity = vec2(0f, 0f)) // no gravity: state poked by hand

        val world = configureWorld {
            injectables {
                add(pw); add(cfg); add(runState); add(refs)
            }
            systems { add(PlatformSystem()) }
        }

        val platY = 1.0f
        val platTop = platY + GameConfig.PLATFORM_HEIGHT

        fun climber(): Entity = world.entity {
            val b = pw.body {
                type = BodyDef.BodyType.DynamicBody
                position.set(GameConfig.WIDTH / 2f, platTop + 1f) // above the slab
                circle(radius = GameConfig.BALL_RADIUS) {}
            }
            it += Physics(b)
            it += Player()
        }

        val left = buildPlatformHalf(pw, 0f, 2.0f, platY)
        val right = buildPlatformHalf(pw, 3.0f, GameConfig.WIDTH, platY)
        val plat = world.entity { it += PlatformC(left, right, platY, 2.0f, 1.0f) }
        left.userData = plat
        right.userData = plat

        val leader = climber()
        val laggard = climber()
        refs.player = leader

        with(world) {
            // Only the leader has landed; the laggard hasn't -> stay open.
            plat[PlatformC].touchedBy += leader
            world.update(DT)
            assertFalse(plat[PlatformC].bridged) {
                "hole must stay open until every living climber has landed"
            }

            // Last climber lands -> all living have foot contact -> bridge.
            plat[PlatformC].touchedBy += laggard
            world.update(DT)
            assertTrue(plat[PlatformC].bridged) {
                "hole must bridge once the last climber has landed"
            }
        }

        world.dispose()
        pw.dispose()
    }

    // Regression for the fell-back trap: a climber that landed on the slab (so it
    // is in touchedBy) but then dropped BACK below it must re-block the bridge,
    // even when another climber is the last to arrive. Else the slab would seal
    // over the climber beneath it.
    @Test
    fun holeStaysOpenIfALandedClimberFellBackBelow() {
        val cfg = RunConfig()
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()
        val pw = createWorld(gravity = vec2(0f, 0f)) // no gravity: positions set by hand

        val world = configureWorld {
            injectables {
                add(pw); add(cfg); add(runState); add(refs)
            }
            systems { add(PlatformSystem()) }
        }

        val platY = 1.0f
        val platTop = platY + GameConfig.PLATFORM_HEIGHT

        fun climber(y: Float): Entity = world.entity {
            val b = pw.body {
                type = BodyDef.BodyType.DynamicBody
                position.set(GameConfig.WIDTH / 2f, y)
                circle(radius = GameConfig.BALL_RADIUS) {}
            }
            it += Physics(b)
            it += Player()
        }

        val left = buildPlatformHalf(pw, 0f, 2.0f, platY)
        val right = buildPlatformHalf(pw, 3.0f, GameConfig.WIDTH, platY)
        val plat = world.entity { it += PlatformC(left, right, platY, 2.0f, 1.0f) }
        left.userData = plat
        right.userData = plat

        // A landed earlier then fell back to the previous platform (below the top).
        val fallen = climber(platY - 0.4f)
        // B is the last climber, now resting above the slab.
        val last = climber(platTop + 1f)
        refs.player = last

        with(world) {
            // Both have landed at some point -> both in touchedBy.
            plat[PlatformC].touchedBy += fallen
            plat[PlatformC].touchedBy += last
            world.update(DT)
            assertFalse(plat[PlatformC].bridged) {
                "must not seal over a climber that fell back below it"
            }

            // The fallen climber climbs back above -> now safe to seal.
            fallen[Physics].body.setTransform(GameConfig.WIDTH / 2f, platTop + 1f, 0f)
            world.update(DT)
            assertTrue(plat[PlatformC].bridged) {
                "bridges once the fallen climber is back above the slab"
            }
        }

        world.dispose()
        pw.dispose()
    }

    // The trigger is foot contact, not position: a climber that flew up through the
    // hole and is now far above — but never stood on the slab — must NOT seal it.
    @Test
    fun holeNeedsFootContactNotJustPosition() {
        val cfg = RunConfig()
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()
        val pw = createWorld(gravity = vec2(0f, 0f))

        val world = configureWorld {
            injectables {
                add(pw); add(cfg); add(runState); add(refs)
            }
            systems { add(PlatformSystem()) }
        }

        val platY = 1.0f
        val platTop = platY + GameConfig.PLATFORM_HEIGHT
        val left = buildPlatformHalf(pw, 0f, 2.0f, platY)
        val right = buildPlatformHalf(pw, 3.0f, GameConfig.WIDTH, platY)
        val plat = world.entity { it += PlatformC(left, right, platY, 2.0f, 1.0f) }
        left.userData = plat
        right.userData = plat

        val flyer = world.entity {
            val b = pw.body {
                type = BodyDef.BodyType.DynamicBody
                position.set(GameConfig.WIDTH / 2f, platTop + 5f) // well above, never landed
                circle(radius = GameConfig.BALL_RADIUS) {}
            }
            it += Physics(b)
            it += Player()
        }
        refs.player = flyer

        with(world) {
            world.update(DT)
            assertFalse(plat[PlatformC].bridged) {
                "being above the slab is not enough; needs foot contact"
            }

            plat[PlatformC].touchedBy += flyer
            world.update(DT)
            assertTrue(plat[PlatformC].bridged) {
                "bridges once the climber has landed on it"
            }
        }

        world.dispose()
        pw.dispose()
    }

    // End-to-end: a real ball dropped onto a slab's solid half makes the contact
    // listener record the landing, and PlatformSystem then seals the hole. Proves
    // the listener -> touchedBy -> bridge wiring the unit tests poke by hand.
    @Test
    fun footContactBridgesAfterLanding() {
        val cfg = RunConfig()
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()
        val pw = createWorld(gravity = vec2(0f, GameConfig.GRAVITY * cfg.gravityScale))

        val world = configureWorld {
            injectables {
                add(pw); add(cfg); add(runState); add(refs)
            }
            systems {
                add(PhysicsStepSystem())
                add(TransformSyncSystem())
                add(PlatformSystem())
            }
        }

        // Slab at y=1.0 with its hole on the right; the ball lands on the wide
        // solid-left half.
        val platY = 1.0f
        val left = buildPlatformHalf(pw, 0f, 3.0f, platY)
        val right = buildPlatformHalf(pw, 4.0f, GameConfig.WIDTH, platY)
        val plat = world.entity { it += PlatformC(left, right, platY, 3.0f, 1.0f) }
        left.userData = plat
        right.userData = plat

        val factory = EntityFactory(world, pw, cfg, refs)
        // Spawn over the solid-left half, just above the slab top, then let it fall.
        val player = factory.spawnPlayer(x = 0.5f, y = 1.5f, controller = BotController())
        refs.player = player

        pw.setContactListener(PlayContactListener(world))

        repeat(120) { world.update(DT) } // ~2s: fall, land, register, bridge

        with(world) {
            assertTrue(player in plat[PlatformC].touchedBy) {
                "landing must record the climber on the platform it stood on"
            }
            assertTrue(plat[PlatformC].bridged) {
                "platform must bridge once its only living climber has landed"
            }
        }

        world.dispose()
        pw.dispose()
    }

    companion object {
        private const val SEED = 42L
        private const val DT = 1f / 60f
        private const val CLIMB_SECONDS = 20
        private const val CLIMB_MARGIN_M = 1f

        // Boot a no-op headless app ONCE: sets Gdx.app/files + loads gdx native.
        // Box2D native loads lazily on the first createWorld. Gdx.gl stays null.
        @BeforeAll
        @JvmStatic
        fun boot() {
            if (Gdx.app == null) {
                HeadlessApplication(object : ApplicationAdapter() {}, HeadlessApplicationConfiguration())
            }
        }
    }
}
