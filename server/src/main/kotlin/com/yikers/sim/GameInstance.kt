package com.yikers.sim

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Family
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.control.Palette
import com.yikers.control.RelayController
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.buildArena
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.component.Physics
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
import java.util.concurrent.ConcurrentLinkedQueue
import ktx.box2d.createWorld
import ktx.math.vec2
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// One authoritative game world: Box2D + Fleks + RNG, headless. Roster is dynamic:
// each joining client (human or bot, the instance can't tell) gets a slot via
// addPlayer(); the platform/boulder layout is built up front so the seed is stable.
class GameInstance(private val cfg: SessionConfig) {
    private val runState = RunState().apply {
        highScore = cfg.previousHighScore
    }
    private val physicsWorld: PhysicsWorld =
        createWorld(gravity = vec2(0f, GameConfig.GRAVITY * cfg.runConfig.gravityScale))
    private val refs = Refs()
    private val world: World
    private val factory: EntityFactory
    private val renderables: Family
    private val platforms: Family
    private var tickCount = 0L

    // Slots are reserved off the tick thread (accept thread needs one synchronously
    // for the handshake); spawn/despawn run on the tick thread via pendingOps so
    // Box2D is only mutated between steps.
    private val slotLock = Any()
    private val usedSlots = HashSet<Int>()
    private val relays = HashMap<Int, RelayController>()
    private val entitiesBySlot = HashMap<Int, Entity>()
    private val slotByEntity = HashMap<Entity, Int>()   // reverse, for stamping snapshots
    private val pendingOps = ConcurrentLinkedQueue<() -> Unit>()

    // Live player count (reserved slots). Cheap, thread-safe — used by host listings.
    val players: Int get() = synchronized(slotLock) { usedSlots.size }

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
                // No camera / ShapeRenderer: headless; the client renders snapshots.
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

        factory = EntityFactory(world, physicsWorld, cfg.runConfig, refs)
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnPlatform(GameConfig.GROUND_HEIGHT + i * GameConfig.PLATFORM_INTERVALS)
        }
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnBoulder(GameConfig.WIDTH / 2f - GameConfig.BOULDER_RADIUS, -3.0f - i * 0.6f)
        }
        physicsWorld.setContactListener(PlayContactListener(world))
    }

    // Reserve the lowest free slot; ball spawns next tick. Safe from any thread.
    fun addPlayer(): Int {
        val slot = synchronized(slotLock) {
            var s = 0
            while (s in usedSlots) s++
            usedSlots.add(s)
            s
        }
        pendingOps.add { spawnPlayerSlot(slot) }
        return slot
    }

    // Queue a client's ball to despawn on the next tick and free its slot.
    fun removePlayer(slot: Int) {
        pendingOps.add { despawnPlayerSlot(slot) }
    }

    // Route a client's input to its relay. Out-of-range / not-yet-spawned slot is
    // ignored (the relay appears once the spawn op has run).
    fun applyInput(cmd: InputCommand) {
        relays[cmd.playerId]?.submit(cmd)
    }

    fun tick(deltaTime: Float) {
        drainOps()                 // spawn/despawn between steps -> Box2D-safe
        world.update(deltaTime)
        tickCount++
    }

    private fun drainOps() {
        while (true) {
            val op = pendingOps.poll() ?: break
            op()
        }
    }

    // --- roster mutation, tick thread only (run from drainOps) --------------------

    private fun spawnPlayerSlot(slot: Int) {
        val r = GameConfig.BALL_RADIUS
        val cx = laneX(slot)
        val controller = RelayController(slot)
        val e = factory.spawnPlayer(
            x = cx - r,
            y = GameConfig.GROUND_HEIGHT,
            controller = controller,
            color = Palette.distinct(slot, COLOR_SPREAD),
            group = (-(slot + 1)).toShort(),
        )
        relays[slot] = controller
        entitiesBySlot[slot] = e
        slotByEntity[e] = slot
        if (refs.player == null) refs.player = e   // first joiner = primary (scroll/score)
        runState.started = true
    }

    private fun despawnPlayerSlot(slot: Int) {
        val e = entitiesBySlot.remove(slot) ?: run {
            synchronized(slotLock) { usedSlots.remove(slot) }
            return
        }
        relays.remove(slot)
        slotByEntity.remove(e)
        with(world) {
            physicsWorld.destroyBody(e[Physics].body)
            physicsWorld.destroyBody(e[FootSensor].footBody)
        }
        world -= e
        if (refs.player == e) refs.player = entitiesBySlot.values.firstOrNull()
        synchronized(slotLock) { usedSlots.remove(slot) }
    }

    // Renderable state for the client; the arena is redrawn client-side from GameConfig.
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
                    id = e.id,
                    playerId = slotByEntity[e] ?: -1,
                )
            }
            platforms.forEach { e ->
                val p = e[PlatformC]
                plats += PlatformSnap(p.y, p.holeX, p.holeWidth)
            }
        }
        return WorldSnapshot(
            tick = tickCount,
            entities = ents,
            platforms = plats,
            score = runState.score,
            dead = runState.dead,
            scrollY = runState.scrollY,
            highScore = runState.highScore,
        )
    }

    fun close() {
        world.dispose()
        physicsWorld.dispose()
    }

    private companion object {
        // Nominal palette spread for distinct climber hues; slot index picks one.
        const val COLOR_SPREAD = 8
    }
}
