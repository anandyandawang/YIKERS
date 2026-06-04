package com.yikers.sim

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Family
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.control.BotController
import com.yikers.control.Controller
import com.yikers.control.HumanRelayController
import com.yikers.control.Palette
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.buildArena
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.component.RenderShape
import com.yikers.ecs.component.Transform
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
import com.yikers.net.EntitySnap
import com.yikers.net.InputCommand
import com.yikers.net.PlatformSnap
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.physics.PlayContactListener
import ktx.box2d.createWorld
import ktx.math.vec2
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// One authoritative game world: Box2D + Fleks + RNG, fully headless (no GL). This
// is what PlayScreen.newRun() used to build inline, minus the render system + its
// camera/ShapeRenderer. It owns its own clock via tick(); a host runs many of these
// (one per room), each independent.
class GameInstance(private val cfg: SessionConfig) {
    private val runState = RunState().apply {
        highScore = cfg.previousHighScore
        viewHeight = cfg.viewHeight
    }
    private val physicsWorld: PhysicsWorld =
        createWorld(gravity = vec2(0f, GameConfig.GRAVITY * cfg.runConfig.gravityScale))
    private val refs = Refs()
    private val relays = ArrayList<HumanRelayController>()
    private val world: World
    private val renderables: Family
    private val platforms: Family
    private var tickCount = 0L

    val players: Int get() = cfg.humans + cfg.bots

    init {
        cfg.seed?.let { MathUtils.random.setSeed(it) }
        val arena = buildArena(physicsWorld)
        world = configureWorld {
            injectables {
                add(physicsWorld)
                add(cfg.runConfig)
                add(runState)
                add(arena)
                add(refs)
                // NO camera / ShapeRenderer: the sim is headless; the client renders
                // from snapshots.
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
            }
        }
        renderables = world.family { all(Transform, RenderShape) }
        platforms = world.family { all(PlatformC) }

        val factory = EntityFactory(world, physicsWorld, cfg.runConfig, refs)
        buildRoster(factory)
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnPlatform(GameConfig.GROUND_HEIGHT + i * GameConfig.PLATFORM_INTERVALS)
        }
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnBoulder(GameConfig.WIDTH / 2f - GameConfig.BOULDER_RADIUS, -3.0f - i * 0.6f)
        }
        physicsWorld.setContactListener(PlayContactListener(world))
    }

    // Same layout as the old PlayScreen.spawnRoster: humans first (each a relay fed
    // by the client over the seam), then bots; spread across the floor in distinct
    // colors; first spawned = primary (PlatformSystem scores off it). Empty roster
    // (0 humans, 0 bots) falls back to a lone bot = hands-free attract mode.
    private fun buildRoster(factory: EntityFactory) {
        val controllers: List<Controller> =
            List(cfg.humans) { HumanRelayController(it).also { r -> relays += r } } +
                List(cfg.bots) { BotController() }
        val roster = controllers.ifEmpty { listOf(BotController()) }
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

    // Route a human's input to its relay. Out-of-range playerId is ignored.
    fun applyInput(cmd: InputCommand) {
        relays.getOrNull(cmd.playerId)?.submit(cmd)
    }

    // Client viewport height -> shared world bounds (WallFollowSystem reads it).
    fun setViewHeight(height: Float) {
        runState.viewHeight = height
    }

    fun tick(deltaTime: Float) {
        world.update(deltaTime)
    }

    // Extract renderable state for the client. Reads exactly the fields the old
    // RenderSystem drew; arena (ground/walls) is redrawn client-side from GameConfig.
    fun snapshot(): WorldSnapshot {
        val ents = ArrayList<EntitySnap>()
        val plats = ArrayList<PlatformSnap>()
        with(world) {
            renderables.forEach { e ->
                val t = e[Transform]
                val rs = e[RenderShape]
                ents += EntitySnap(
                    rs.kind, rs.color.r, rs.color.g, rs.color.b, rs.color.a,
                    t.position.x, t.position.y, t.size.x, t.size.y, t.rotation,
                )
            }
            platforms.forEach { e ->
                val p = e[PlatformC]
                plats += PlatformSnap(p.y, p.holeX, p.holeWidth)
            }
        }
        return WorldSnapshot(
            tick = tickCount++,
            entities = ents,
            platforms = plats,
            score = runState.score,
            dead = runState.dead,
            scrollY = runState.scrollY,
            highScore = runState.highScore,
            viewHeight = runState.viewHeight,
        )
    }

    fun close() {
        world.dispose()
        physicsWorld.dispose()
    }
}
