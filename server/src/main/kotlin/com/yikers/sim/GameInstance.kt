package com.yikers.sim

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.yikers.config.GameConfig
import com.yikers.control.Palette
import com.yikers.control.RelayController
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.buildArena
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Physics
import com.yikers.ecs.event.Events
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.level.ClassicGenerator
import com.yikers.level.LevelGenerator
import com.yikers.net.InputCommand
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import java.util.concurrent.ConcurrentLinkedQueue
import ktx.box2d.createWorld
import ktx.math.vec2
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// One authoritative world: Box2D + Fleks + RNG, headless. Dynamic roster: each
// joining client gets a slot via addPlayer(). Layout built up front (stable seed).
class GameInstance(private val cfg: SessionConfig) {
    private val runState = RunState().apply {
        highScore = cfg.previousHighScore
    }
    private val physicsWorld: PhysicsWorld =
        createWorld(gravity = vec2(0f, GameConfig.GRAVITY * cfg.runConfig.gravityScale))
    private val refs = Refs()
    private val events = Events()
    private val generator: LevelGenerator = ClassicGenerator(cfg.runConfig)
    private val world: World
    private val factory: EntityFactory
    private val snapshots: SnapshotBuilder
    private var tickCount = 0L

    // Slots reserved off the tick thread (accept thread needs one for the handshake);
    // spawn/despawn run via pendingOps so Box2D mutates only between steps.
    private val slotLock = Any()
    private val usedSlots = HashSet<Int>()
    private val relays = HashMap<Int, RelayController>()
    private val entitiesBySlot = HashMap<Int, Entity>()
    private val pendingOps = ConcurrentLinkedQueue<() -> Unit>()

    val players: Int get() = synchronized(slotLock) { usedSlots.size }

    init {
        cfg.seed?.let { MathUtils.random.setSeed(it) }
        val arena = buildArena(physicsWorld)
        world = buildSimWorld(physicsWorld, cfg.runConfig, runState, arena, refs, events, generator)
        snapshots = SnapshotBuilder(world, runState)
        factory = EntityFactory(world, physicsWorld, refs)
        spawnInitialLayout(factory, generator)
    }

    // Reserve the lowest free slot; ball spawns next tick. Any-thread safe.
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

    fun removePlayer(slot: Int) {
        pendingOps.add { despawnPlayerSlot(slot) }
    }

    // Route input to the slot's relay; a not-yet-spawned slot is ignored.
    fun applyInput(cmd: InputCommand) {
        relays[cmd.slot]?.submit(cmd)
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
            slot = slot,
        )
        relays[slot] = controller
        entitiesBySlot[slot] = e
        if (refs.player == null) refs.player = e   // first joiner = primary (scroll/score)
        runState.started = true
    }

    private fun despawnPlayerSlot(slot: Int) {
        val e = entitiesBySlot.remove(slot) ?: run {
            synchronized(slotLock) { usedSlots.remove(slot) }
            return
        }
        relays.remove(slot)
        with(world) {
            physicsWorld.destroyBody(e[Physics].body)
            physicsWorld.destroyBody(e[FootSensor].footBody)
        }
        world -= e
        if (refs.player == e) refs.player = entitiesBySlot.values.firstOrNull()
        synchronized(slotLock) { usedSlots.remove(slot) }
    }

    fun snapshot(): WorldSnapshot = snapshots.build(tickCount)

    fun close() {
        world.dispose()
        physicsWorld.dispose()
    }

    private companion object {
        const val COLOR_SPREAD = 8
    }
}
