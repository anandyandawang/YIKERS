package com.yikers.support

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.Controller
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.buildArena
import com.yikers.ecs.component.Physics
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.BoulderSystem
import com.yikers.ecs.system.DeathSystem
import com.yikers.ecs.system.JumpSystem
import com.yikers.ecs.system.MoveSystem
import com.yikers.ecs.system.PhysicsStepSystem
import com.yikers.ecs.system.PlatformSystem
import com.yikers.ecs.system.ScrollSystem
import com.yikers.ecs.system.TransformSyncSystem
import com.yikers.ecs.system.WallFollowSystem
import com.yikers.physics.PlayContactListener
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// A full headless run: the same Fleks + Box2D worlds PlayScreen.newRun() builds,
// MINUS the GL-bound RenderSystem and its camera/ShapeRenderer injectables (the
// sim is camera-free; the kill-line is RunState.scrollY). `use {}` it -> close()
// disposes both worlds. This is the canonical replacement for the old inline
// Harness + buildHeadlessWorld().
class SimHarness(
    val physicsWorld: PhysicsWorld,
    val world: World,
    val runState: RunState,
    val refs: Refs,
    val cfg: RunConfig,
    val player: Entity,
    val climbers: List<Entity>,
) : AutoCloseable {
    val startCamera: Boolean get() = runState.startCamera
    val scrollY: Float get() = runState.scrollY

    // Primary climber ball center Y, in meters.
    fun playerY(): Float = with(world) { player[Physics].body.position.y }

    override fun close() {
        world.dispose()
        physicsWorld.dispose()
    }
}

// Build a full headless run. seed != null => reproducible platform/boulder layout.
// Default roster is a lone autopilot climber (test-side stand-in for a bot client).
fun buildSim(
    controllers: List<Controller> = listOf(AutopilotController()),
    seed: Long? = null,
    spawnPlatforms: Boolean = true,
    spawnBoulders: Boolean = true,
): SimHarness {
    seed?.let { MathUtils.random.setSeed(it) }

    val cfg = RunConfig()
    // Huge highScore => DeathSystem never writes Prefs (Gdx.app file) at run-end.
    val runState = RunState().apply { highScore = Int.MAX_VALUE }
    val refs = Refs()

    val pw = physicsWorld(cfg.gravityScale)
    val arena = buildArena(pw)

    val world = configureWorld {
        injectables {
            add(pw)
            add(cfg)
            add(runState)
            add(arena)
            add(refs)
            // NO camera / ShapeRenderer: RenderSystem excluded, sim is camera-free.
        }
        // Canonical 10-system list in PlayScreen order MINUS RenderSystem. Gdx.gl
        // is null headless, so adding any GL system here NPEs immediately.
        systems {
            // AutopilotSystem replaces production's ControlSystem here: it drives the
            // climbers (which are RelayControllers in production, fed by clients).
            add(AutopilotSystem())
            add(MoveSystem())
            add(JumpSystem())
            add(WallFollowSystem())
            add(PhysicsStepSystem())
            add(TransformSyncSystem())
            add(BoulderSystem())
            add(PlatformSystem())
            add(ScrollSystem())
            add(DeathSystem())
        }
    }

    val factory = EntityFactory(world, pw, cfg, refs)
    // Spread the roster across the floor exactly like PlayScreen.spawnRoster.
    val r = GameConfig.BALL_RADIUS
    val minCx = GameConfig.WALL_THICKNESS + r
    val maxCx = GameConfig.WIDTH - GameConfig.WALL_THICKNESS - r
    val n = controllers.size
    val climbers = controllers.mapIndexed { i, controller ->
        val cx = if (n == 1) GameConfig.WIDTH / 2f
        else minCx + (maxCx - minCx) * (i.toFloat() / (n - 1))
        factory.spawnPlayer(
            x = cx - r,
            y = GameConfig.GROUND_HEIGHT,
            controller = controller,
            group = (-(i + 1)).toShort(),
        )
    }
    refs.player = climbers.first()
    // Climbers are spawned directly here (not via GameInstance.addPlayer), so flag
    // the run started -> DeathSystem can end it once every climber dies.
    runState.started = true

    if (spawnPlatforms) {
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnPlatform(GameConfig.GROUND_HEIGHT + i * GameConfig.PLATFORM_INTERVALS)
        }
    }
    if (spawnBoulders) {
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnBoulder(GameConfig.WIDTH / 2f - GameConfig.BOULDER_RADIUS, -3.0f - i * 0.6f)
        }
    }

    pw.setContactListener(PlayContactListener(world))
    return SimHarness(pw, world, runState, refs, cfg, climbers.first(), climbers)
}
