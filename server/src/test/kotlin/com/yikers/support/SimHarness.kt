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

// A full headless run: the same worlds PlayScreen builds, minus the GL RenderSystem.
// close() disposes both worlds.
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

    fun playerY(): Float = with(world) { player[Physics].body.position.y }

    override fun close() {
        world.dispose()
        physicsWorld.dispose()
    }
}

// Build a full headless run (seed => reproducible layout; default lone climber).
fun buildSim(
    controllers: List<Controller> = listOf(ScriptedClimber()),
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
        }
        // 10-system PlayScreen order minus RenderSystem (Gdx.gl is null headless).
        systems {
            // ScriptedClimbSystem replaces production's ControlSystem here.
            add(ScriptedClimbSystem())
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
    // Spawned directly (not via addPlayer), so flag started for DeathSystem.
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
