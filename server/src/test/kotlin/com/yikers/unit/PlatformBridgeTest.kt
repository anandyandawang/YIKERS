package com.yikers.unit

import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.RelayController
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.event.Events
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.PhysicsStepSystem
import com.yikers.ecs.system.PlatformBridgeSystem
import com.yikers.ecs.system.TransformSyncSystem
import com.yikers.physics.PlayContactListener
import com.yikers.support.HeadlessGdx
import com.yikers.support.TestWorld
import com.yikers.support.physicsWorld
import com.yikers.support.spawnTestClimber
import com.yikers.support.spawnTestPlatform
import com.yikers.support.step
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// A slab seals its hole only once every living climber foot-landed AND is above it.
@HeadlessGdx
class PlatformBridgeTest {

    // Build a no-gravity world running only PlatformBridgeSystem; state is poked by hand.
    private fun bridgeWorld(): TestWorld {
        val pw = physicsWorld(gravityScale = 0f)
        val cfg = RunConfig()
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()
        val world = configureWorld {
            injectables { add(pw); add(runState) }
            systems { add(PlatformBridgeSystem()) }
        }
        return TestWorld(pw, world, runState, refs, cfg)
    }

    @Test
    fun holeStaysOpenUntilLastClimberLands() {
        bridgeWorld().use { tw ->
            val world = tw.world
            val pw = tw.physicsWorld
            val platTop = PLAT_Y + GameConfig.PLATFORM_HEIGHT

            val plat = world.spawnTestPlatform(pw, PLAT_Y, holeX = 2.0f, holeWidth = 1.0f)
            val leader = world.spawnTestClimber(pw, GameConfig.WIDTH / 2f, platTop + 1f)
            val laggard = world.spawnTestClimber(pw, GameConfig.WIDTH / 2f, platTop + 1f)
            tw.refs.player = leader

            with(world) {
                plat[PlatformC].touchedBy += leader
                world.step(1)
                assertFalse(plat[PlatformC].bridged) {
                    "hole must stay open until every living climber has landed"
                }

                plat[PlatformC].touchedBy += laggard
                world.step(1)
                assertTrue(plat[PlatformC].bridged) {
                    "hole must bridge once the last climber has landed"
                }
            }
        }
    }

    // Fell-back trap: a landed climber that dropped below must re-block the seal.
    @Test
    fun holeStaysOpenIfALandedClimberFellBackBelow() {
        bridgeWorld().use { tw ->
            val world = tw.world
            val pw = tw.physicsWorld
            val platTop = PLAT_Y + GameConfig.PLATFORM_HEIGHT

            val plat = world.spawnTestPlatform(pw, PLAT_Y, holeX = 2.0f, holeWidth = 1.0f)
            val fallen = world.spawnTestClimber(pw, GameConfig.WIDTH / 2f, PLAT_Y - 0.4f)
            val last = world.spawnTestClimber(pw, GameConfig.WIDTH / 2f, platTop + 1f)
            tw.refs.player = last

            with(world) {
                plat[PlatformC].touchedBy += fallen
                plat[PlatformC].touchedBy += last
                world.step(1)
                assertFalse(plat[PlatformC].bridged) {
                    "must not seal over a climber that fell back below it"
                }

                fallen[Physics].body.setTransform(GameConfig.WIDTH / 2f, platTop + 1f, 0f)
                world.step(1)
                assertTrue(plat[PlatformC].bridged) {
                    "bridges once the fallen climber is back above the slab"
                }
            }
        }
    }

    // Trigger is foot contact, not position: flying above without landing won't seal.
    @Test
    fun holeNeedsFootContactNotJustPosition() {
        bridgeWorld().use { tw ->
            val world = tw.world
            val pw = tw.physicsWorld
            val platTop = PLAT_Y + GameConfig.PLATFORM_HEIGHT

            val plat = world.spawnTestPlatform(pw, PLAT_Y, holeX = 2.0f, holeWidth = 1.0f)
            val flyer = world.spawnTestClimber(pw, GameConfig.WIDTH / 2f, platTop + 5f) // never landed
            tw.refs.player = flyer

            with(world) {
                world.step(1)
                assertFalse(plat[PlatformC].bridged) {
                    "being above the slab is not enough; needs foot contact"
                }

                plat[PlatformC].touchedBy += flyer
                world.step(1)
                assertTrue(plat[PlatformC].bridged) {
                    "bridges once the climber has landed on it"
                }
            }
        }
    }

    // End-to-end: a dropped ball lands, the contact listener records it, the hole seals.
    @Test
    fun footContactBridgesAfterLanding() {
        val pw = physicsWorld(gravityScale = 1f)
        val cfg = RunConfig()
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()
        val world = configureWorld {
            injectables { add(pw); add(runState) }
            systems {
                add(PhysicsStepSystem())
                add(TransformSyncSystem())
                add(PlatformBridgeSystem())
            }
        }
        TestWorld(pw, world, runState, refs, cfg).use { tw ->
            val plat = world.spawnTestPlatform(pw, PLAT_Y, holeX = 3.0f, holeWidth = 1.0f)

            val factory = EntityFactory(world, pw, refs)
            val player = factory.spawnPlayer(x = 0.5f, y = 1.5f, controller = RelayController(0), slot = 0)
            refs.player = player

            pw.setContactListener(PlayContactListener(world, Events()))

            world.step(120) // ~2s: fall, land, register, bridge

            with(world) {
                assertTrue(player in plat[PlatformC].touchedBy) {
                    "landing must record the climber on the platform it stood on"
                }
                assertTrue(plat[PlatformC].bridged) {
                    "platform must bridge once its only living climber has landed"
                }
            }
        }
    }

    companion object {
        private const val PLAT_Y = 1.0f
    }
}
