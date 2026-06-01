package com.yikers.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import com.yikers.YikersGame
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.config.RunConfig
import com.yikers.control.BotController
import com.yikers.control.Controller
import com.yikers.control.HumanController
import com.yikers.control.Palette
import com.yikers.control.Roster
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.buildArena
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.BoulderSystem
import com.yikers.ecs.system.CameraScrollSystem
import com.yikers.ecs.system.ControlSystem
import com.yikers.ecs.system.DeathSystem
import com.yikers.ecs.system.HudSystem
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

    // HUD draws in its own pixel space (480x800) — the world cam is meters now,
    // so the font would render ~100x too big through it. Stays screen-fixed.
    private val hudCamera = OrthographicCamera()
    private val hudViewport = FitViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX, hudCamera)

    private lateinit var physicsWorld: PhysicsWorld
    private lateinit var world: World
    private lateinit var runState: RunState
    private var built = false
    private var deadElapsed = 0f

    override fun show() {
        if (built) teardown()
        newRun()
    }

    private fun newRun() {
        deadElapsed = 0f
        val cfg = RunConfig()
        runState = RunState().apply { highScore = Prefs.highScore }
        val refs = Refs()

        camera.position.set(GameConfig.WIDTH / 2f, GameConfig.HEIGHT / 2f, 0f)
        camera.update()
        hudCamera.position.set(GameConfig.WIDTH_PX / 2f, GameConfig.HEIGHT_PX / 2f, 0f)
        hudCamera.update()

        physicsWorld = createWorld(gravity = vec2(0f, GameConfig.GRAVITY * cfg.gravityScale))
        val arena = buildArena(physicsWorld)

        world = configureWorld {
            injectables {
                add(physicsWorld)
                add(camera)
                add("hud", hudCamera)
                add(cfg)
                add(runState)
                add(arena)
                add(refs)
                add(game.batch)
                add(game.shape)
                add(game.font)
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
                add(RenderSystem())
                add(HudSystem())
            }
        }

        val factory = EntityFactory(world, physicsWorld, cfg, refs)
        spawnRoster(factory, refs)
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnPlatform(GameConfig.GROUND_HEIGHT + i * GameConfig.PLATFORM_INTERVALS)
        }
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnBoulder(GameConfig.WIDTH / 2f - GameConfig.BOULDER_RADIUS, -3.0f - i * 0.6f)
        }

        physicsWorld.setContactListener(PlayContactListener(world, runState))
        built = true
    }

    // Spawn Roster.humans humans + Roster.bots bots, spread across the floor in
    // distinct colors. First spawned = primary (PlatformSystem scores off it).
    private fun spawnRoster(factory: EntityFactory, refs: Refs) {
        val controllers: List<Controller> =
            List(Roster.humans) { HumanController() } + List(Roster.bots) { BotController() }
        val roster = controllers.ifEmpty {
            listOf(if (Roster.handsFree) BotController() else HumanController())
        }
        val n = roster.size

        val r = GameConfig.BALL_RADIUS
        val minCx = GameConfig.WALL_THICKNESS + r
        val maxCx = GameConfig.WIDTH - GameConfig.WALL_THICKNESS - r
        roster.forEachIndexed { i, controller ->
            val cx = if (n == 1) GameConfig.WIDTH / 2f
            else minCx + (maxCx - minCx) * (i.toFloat() / (n - 1))
            val e = factory.spawnPlayer(
                x = cx - r,
                y = GameConfig.GROUND_HEIGHT,
                controller = controller,
                color = Palette.distinct(i, n),
                group = (-(i + 1)).toShort(),
            )
            if (i == 0) refs.player = e
        }
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0.10f, 0.12f, 0.16f, 1f)
        viewport.apply()
        world.update(delta)

        if (runState.dead) handleGameOver(delta)
    }

    // Hands-free run returns to the menu (which auto-starts again) after a beat;
    // otherwise wait for a key/tap, as before.
    private fun handleGameOver(delta: Float) {
        if (Roster.handsFree) {
            deadElapsed += delta
            if (deadElapsed >= AUTO_RESTART_DELAY) game.setScreen<MenuScreen>()
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.justTouched()) {
            game.setScreen<MenuScreen>()
        }
    }

    override fun resize(width: Int, height: Int) {
        // PlayScreen drives camera.y for scrolling — don't recenter the world cam.
        viewport.update(width, height, false)
        hudViewport.update(width, height, true)
    }

    override fun hide() = teardown()

    override fun dispose() = teardown()

    private fun teardown() {
        if (!built) return
        world.dispose()
        physicsWorld.dispose()
        built = false
    }

    companion object {
        private const val AUTO_RESTART_DELAY = 1.5f
    }
}
