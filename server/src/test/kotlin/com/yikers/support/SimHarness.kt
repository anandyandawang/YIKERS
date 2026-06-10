package com.yikers.support

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.Controller
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.buildArena
import com.yikers.ecs.component.Physics
import com.yikers.ecs.event.Events
import com.yikers.ecs.resource.AugmentChoices
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.level.ClassicGenerator
import com.yikers.sim.buildSimWorld
import com.yikers.sim.spawnInitialLayout
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// A full headless run: the production sim via buildSimWorld (single source of the
// system order), only the control system swapped for the scripted climber.
// close() disposes both worlds.
class SimHarness(
    val physicsWorld: PhysicsWorld,
    val world: World,
    val runState: RunState,
    val refs: Refs,
    val cfg: RunConfig,
    val events: Events,
    val choices: AugmentChoices,
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
    val runState = RunState()
    val refs = Refs()
    val events = Events()
    val choices = AugmentChoices()
    val generator = ClassicGenerator(cfg)

    val pw = physicsWorld(cfg.gravityScale)
    val arena = buildArena(pw)

    // ScriptedClimbSystem replaces production's ControlSystem here.
    val world = buildSimWorld(pw, cfg, runState, arena, refs, events, choices, generator) {
        ScriptedClimbSystem()
    }

    val factory = EntityFactory(world, pw, refs)
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
            slot = i,
        )
    }
    refs.player = climbers.first()
    // Spawned directly (not via addPlayer), so flag started for DeathSystem.
    runState.started = true

    spawnInitialLayout(factory, generator, platforms = spawnPlatforms, boulders = spawnBoulders)

    return SimHarness(pw, world, runState, refs, cfg, events, choices, climbers.first(), climbers)
}
