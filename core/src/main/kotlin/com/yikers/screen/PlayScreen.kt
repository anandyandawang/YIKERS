package com.yikers.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import com.yikers.YikersGame
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.config.RunConfig
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.buildArena
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.BoulderSystem
import com.yikers.ecs.system.CameraScrollSystem
import com.yikers.ecs.system.DeathSystem
import com.yikers.ecs.system.HudSystem
import com.yikers.ecs.system.InputSystem
import com.yikers.ecs.system.PhysicsStepSystem
import com.yikers.ecs.system.PlatformSystem
import com.yikers.ecs.system.RenderSystem
import com.yikers.ecs.system.TransformSyncSystem
import com.yikers.ecs.system.WallFollowSystem
import com.yikers.physics.PlayContactListener
import ktx.app.KtxScreen
import ktx.box2d.createWorld
import ktx.math.vec2
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// Owns one run: builds Box2D + Fleks worlds on show, tears them down on hide.
class PlayScreen(private val game: YikersGame) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConfig.WIDTH, GameConfig.HEIGHT, camera)

    private lateinit var physicsWorld: PhysicsWorld
    private lateinit var world: World
    private lateinit var runState: RunState
    private var built = false

    override fun show() {
        if (built) teardown()
        newRun()
    }

    private fun newRun() {
        val cfg = RunConfig()
        runState = RunState().apply { highScore = Prefs.highScore }
        val refs = Refs()

        camera.position.set(GameConfig.WIDTH / 2f, GameConfig.HEIGHT / 2f, 0f)
        camera.update()

        physicsWorld = createWorld(gravity = vec2(0f, GameConfig.GRAVITY * cfg.gravityScale))
        val arena = buildArena(physicsWorld)

        world = configureWorld {
            injectables {
                add(physicsWorld)
                add(camera)
                add(cfg)
                add(runState)
                add(arena)
                add(refs)
                add(game.batch)
                add(game.shape)
                add(game.font)
            }
            systems {
                add(InputSystem())
                add(WallFollowSystem())
                add(PhysicsStepSystem())
                add(TransformSyncSystem())
                add(BoulderSystem())
                add(PlatformSystem())
                add(CameraScrollSystem())
                add(DeathSystem())
                add(RenderSystem())
                add(HudSystem())
            }
        }

        val factory = EntityFactory(world, physicsWorld, cfg, refs)
        factory.spawnPlayer(GameConfig.WIDTH / 2f - GameConfig.BALL_RADIUS, GameConfig.GROUND_HEIGHT)
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnPlatform(GameConfig.GROUND_HEIGHT + i * GameConfig.PLATFORM_INTERVALS)
        }
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnBoulder(GameConfig.WIDTH / 2f - GameConfig.BOULDER_RADIUS, -300f - i * 60f)
        }

        physicsWorld.setContactListener(PlayContactListener(world, runState))
        built = true
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0.10f, 0.12f, 0.16f, 1f)
        viewport.apply()
        world.update(delta)

        if (runState.dead &&
            (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.justTouched())
        ) {
            game.setScreen<MenuScreen>()
        }
    }

    override fun resize(width: Int, height: Int) {
        // PlayScreen drives camera.y for scrolling — don't recenter.
        viewport.update(width, height, false)
    }

    override fun hide() = teardown()

    override fun dispose() = teardown()

    private fun teardown() {
        if (!built) return
        world.dispose()
        physicsWorld.dispose()
        built = false
    }
}
