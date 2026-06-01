package com.yikers

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.BotController
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.buildArena
import com.yikers.ecs.component.Physics
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.BoulderSystem
import com.yikers.ecs.system.CameraScrollSystem
import com.yikers.ecs.system.ControlSystem
import com.yikers.ecs.system.DeathSystem
import com.yikers.ecs.system.PhysicsStepSystem
import com.yikers.ecs.system.PlatformSystem
import com.yikers.ecs.system.TransformSyncSystem
import com.yikers.ecs.system.WallFollowSystem
import com.yikers.physics.PlayContactListener
import ktx.box2d.createWorld
import ktx.math.vec2
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// End-to-end sim smoke test, no OpenGL. Boots the same Fleks + Box2D worlds a
// real run uses, minus the two GL-bound render systems, drives a lone bot, and
// checks it climbs then can die. Proves the sim runs off-device (the path CI
// reuses for an eventual iOS build).
class HeadlessRunIntegrationTest {

    // Everything a run needs that the test pokes after build.
    private class Harness(
        val physicsWorld: PhysicsWorld,
        val world: World,
        val camera: OrthographicCamera,
        val runState: RunState,
        val refs: Refs,
    )

    // Mirror of PlayScreen.newRun() MINUS RenderSystem/HudSystem and the three GL
    // injectables (SpriteBatch/ShapeRenderer/BitmapFont). Keep this 8-system list
    // canonical: Gdx.gl is null headless, so adding a GL system here NPEs at once.
    private fun buildHeadlessWorld(): Harness {
        val cfg = RunConfig()
        // Big highScore => DeathSystem never writes Prefs (Gdx.app file) at run-end.
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()

        val camera = OrthographicCamera().apply {
            position.set(GameConfig.WIDTH / 2f, GameConfig.HEIGHT / 2f, 0f)
            update()
        }

        val physicsWorld = createWorld(gravity = vec2(0f, GameConfig.GRAVITY * cfg.gravityScale))
        val arena = buildArena(physicsWorld)

        val world = configureWorld {
            injectables {
                add(physicsWorld)
                add(camera)
                add(cfg)
                add(runState)
                add(arena)
                add(refs)
                // NO batch/shape/font: render systems excluded.
            }
            systems {
                add(ControlSystem())
                add(WallFollowSystem())
                add(PhysicsStepSystem())
                add(TransformSyncSystem())
                add(BoulderSystem())
                add(PlatformSystem())
                add(CameraScrollSystem())
                add(DeathSystem())
                // NO RenderSystem / HudSystem.
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
            factory.spawnBoulder(GameConfig.WIDTH / 2f - GameConfig.BOULDER_RADIUS, -300f - i * 60f)
        }

        physicsWorld.setContactListener(PlayContactListener(world, runState))
        return Harness(physicsWorld, world, camera, runState, refs)
    }

    // Player ball center, in pixels.
    private fun World.playerY(harness: Harness): Float =
        with(this) { harness.refs.player!![Physics].body.position.y } * M2P

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
        assertTrue(climbedY > startY + CLIMB_MARGIN_PX) {
            "bot should rise; startY=$startY climbedY=$climbedY"
        }
        assertFalse(h.runState.dead) { "should still be alive mid-climb" }

        // Forced death: raise camera above player so DeathSystem's kill floor
        // (cam.y - HEIGHT/2) sits above it; lone climber -> Dead -> runState.dead.
        h.camera.position.y = climbedY + GameConfig.HEIGHT
        h.camera.update()
        h.world.update(DT)
        assertTrue(h.runState.dead) { "raising camera above player must end the run" }

        h.world.dispose()
        h.physicsWorld.dispose()
    }

    companion object {
        private const val SEED = 42L
        private const val DT = 1f / 60f
        private const val CLIMB_SECONDS = 20
        private const val CLIMB_MARGIN_PX = 100f

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
