package com.yikers.sim

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.event.Events
import com.yikers.ecs.resource.Arena
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.BoulderSystem
import com.yikers.ecs.system.ControlSystem
import com.yikers.ecs.system.DeathSystem
import com.yikers.ecs.system.EventFlushSystem
import com.yikers.ecs.system.JumpSystem
import com.yikers.ecs.system.MoveSystem
import com.yikers.ecs.system.PhysicsStepSystem
import com.yikers.ecs.system.PlatformBridgeSystem
import com.yikers.ecs.system.PlatformRecycleSystem
import com.yikers.ecs.system.PlatformScoreSystem
import com.yikers.ecs.system.ScrollSystem
import com.yikers.ecs.system.TransformSyncSystem
import com.yikers.ecs.system.WallFollowSystem
import com.yikers.level.LevelGenerator
import com.yikers.physics.PlayContactListener
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// THE sim assembly: injectables, system order, contact listener. GameInstance
// (production) and the component-test harness both build here, so the pipeline
// cannot drift between them. Order is load-bearing — see ARCHITECTURE.md.
fun buildSimWorld(
    physicsWorld: PhysicsWorld,
    cfg: RunConfig,
    runState: RunState,
    arena: Arena,
    refs: Refs,
    events: Events,
    generator: LevelGenerator,
    // Tests swap in a scripted control system; production relays client input.
    // Factory (not instance): Fleks inject() only resolves during configureWorld.
    controlSystem: () -> IntervalSystem = { ControlSystem() },
): World {
    val world = configureWorld {
        injectables {
            add(physicsWorld)
            add(cfg)
            add(runState)
            add(arena)
            add(refs)
            add(events)
            add<LevelGenerator>(generator)
        }
        systems {
            add(controlSystem())
            add(MoveSystem())
            add(JumpSystem())
            add(WallFollowSystem())
            add(PhysicsStepSystem())
            add(TransformSyncSystem())
            add(BoulderSystem())
            add(PlatformScoreSystem())
            add(PlatformBridgeSystem())
            add(PlatformRecycleSystem())
            add(ScrollSystem())
            add(DeathSystem())
            add(EventFlushSystem())
        }
    }
    physicsWorld.setContactListener(PlayContactListener(world, events))
    return world
}

// The layout every run opens with. Flags let tests strip platforms or hazards.
fun spawnInitialLayout(
    factory: EntityFactory,
    generator: LevelGenerator,
    platforms: Boolean = true,
    boulders: Boolean = true,
) {
    if (platforms) {
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            val y = GameConfig.GROUND_HEIGHT + i * GameConfig.PLATFORM_INTERVALS
            factory.spawnPlatform(y, generator.nextPlatform(y))
        }
    }
    if (boulders) {
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnBoulder(GameConfig.WIDTH / 2f - GameConfig.BOULDER_RADIUS, -3.0f - i * 0.6f)
        }
    }
}
