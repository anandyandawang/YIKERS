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
    // OrthographicCamera is needed. Keep this 8-system list canonical: Gdx.gl is
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

        // Forced death: raise the kill-line (RunState.scrollY) above the player so
        // DeathSystem's viewBottom (scrollY - HEIGHT/2) sits above it; lone climber
        // -> Dead -> runState.dead.
        h.runState.scrollY = climbedY + GameConfig.HEIGHT
        h.world.update(DT)
        assertTrue(h.runState.dead) { "raising the kill-line above the player must end the run" }

        h.world.dispose()
        h.physicsWorld.dispose()
    }

    // Multiplayer rule ("after last player"): a cleared platform must NOT bridge
    // while a living climber is still below it (else it'd be sealed under solid
    // floor); it closes once the last climber is above.
    @Test
    fun holeStaysOpenUntilLastClimberPasses() {
        val cfg = RunConfig()
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()
        val pw = createWorld(gravity = vec2(0f, 0f)) // no gravity: bodies placed by hand

        val world = configureWorld {
            injectables {
                add(pw); add(cfg); add(runState); add(refs)
            }
            systems { add(PlatformSystem()) }
        }

        fun climber(y: Float): Entity = world.entity {
            val b = pw.body {
                type = BodyDef.BodyType.DynamicBody
                position.set(GameConfig.WIDTH / 2f, y)
                circle(radius = GameConfig.BALL_RADIUS) {}
            }
            it += Physics(b)
            it += Player()
        }

        val platY = 1.0f
        val platTop = platY + GameConfig.PLATFORM_HEIGHT
        val left = buildPlatformHalf(pw, 0f, 2.0f, platY)
        val right = buildPlatformHalf(pw, 3.0f, GameConfig.WIDTH, platY)
        val plat = world.entity { it += PlatformC(left, right, platY, 2.0f, 1.0f) }
        left.userData = plat
        right.userData = plat

        val leader = climber(platTop + 1.0f) // above the platform
        val laggard = climber(platY - 0.3f)  // still below the platform top
        refs.player = leader

        with(world) {
            world.update(DT)
            assertFalse(plat[PlatformC].bridged) {
                "hole must stay open while a climber is below it"
            }

            // last climber clears it -> everyone above -> bridge.
            laggard[Physics].body.setTransform(GameConfig.WIDTH / 2f, platTop + 1.0f, 0f)
            world.update(DT)
            assertTrue(plat[PlatformC].bridged) {
                "hole must bridge once the last climber is above it"
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
