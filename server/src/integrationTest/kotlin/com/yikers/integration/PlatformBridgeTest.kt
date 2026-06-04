package com.yikers.integration

import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.BotController
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.PhysicsStepSystem
import com.yikers.ecs.system.PlatformSystem
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

// PlatformSystem's multiplayer hole-bridging rule: a slab seals its hole only once
// EVERY living climber has foot-landed on it AND is above it. These poke touchedBy
// / positions by hand (no gravity) except the last test, which drops a real ball
// and proves the contact-listener -> touchedBy -> bridge wiring end to end.
@HeadlessGdx
class PlatformBridgeTest {

    // Build a no-gravity world running only PlatformSystem; state is poked by hand.
    private fun bridgeWorld(): TestWorld {
        val pw = physicsWorld(gravityScale = 0f)
        val cfg = RunConfig()
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()
        val world = configureWorld {
            injectables { add(pw); add(cfg); add(runState); add(refs) }
            systems { add(PlatformSystem()) }
        }
        return TestWorld(pw, world, runState, refs, cfg)
    }

    // Must NOT bridge while a living climber still hasn't landed (else it'd seal
    // under solid floor or eject a ball straddling the hole); closes once the last
    // one has foot contact. Both climbers sit above the slab so only the landed
    // flag gates here; the real contact path is footContactBridgesAfterLanding.
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
                // Only the leader has landed -> stay open.
                plat[PlatformC].touchedBy += leader
                world.step(1)
                assertFalse(plat[PlatformC].bridged) {
                    "hole must stay open until every living climber has landed"
                }

                // Last climber lands -> all living have foot contact -> bridge.
                plat[PlatformC].touchedBy += laggard
                world.step(1)
                assertTrue(plat[PlatformC].bridged) {
                    "hole must bridge once the last climber has landed"
                }
            }
        }
    }

    // Fell-back trap: a climber that landed (so it's in touchedBy) then dropped BACK
    // below the slab must re-block the bridge, even when another is last to arrive,
    // else the slab seals over the climber beneath it.
    @Test
    fun holeStaysOpenIfALandedClimberFellBackBelow() {
        bridgeWorld().use { tw ->
            val world = tw.world
            val pw = tw.physicsWorld
            val platTop = PLAT_Y + GameConfig.PLATFORM_HEIGHT

            val plat = world.spawnTestPlatform(pw, PLAT_Y, holeX = 2.0f, holeWidth = 1.0f)
            // A landed earlier then fell back to the previous platform (below the top).
            val fallen = world.spawnTestClimber(pw, GameConfig.WIDTH / 2f, PLAT_Y - 0.4f)
            // B is the last climber, now resting above the slab.
            val last = world.spawnTestClimber(pw, GameConfig.WIDTH / 2f, platTop + 1f)
            tw.refs.player = last

            with(world) {
                // Both have landed at some point -> both in touchedBy.
                plat[PlatformC].touchedBy += fallen
                plat[PlatformC].touchedBy += last
                world.step(1)
                assertFalse(plat[PlatformC].bridged) {
                    "must not seal over a climber that fell back below it"
                }

                // The fallen climber climbs back above -> now safe to seal.
                fallen[Physics].body.setTransform(GameConfig.WIDTH / 2f, platTop + 1f, 0f)
                world.step(1)
                assertTrue(plat[PlatformC].bridged) {
                    "bridges once the fallen climber is back above the slab"
                }
            }
        }
    }

    // The trigger is foot contact, not position: a climber that flew up through the
    // hole and is now far above -- but never stood on the slab -- must NOT seal it.
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

    // End-to-end: a real ball dropped onto a slab's solid half makes the contact
    // listener record the landing, and PlatformSystem then seals the hole. Proves
    // the listener -> touchedBy -> bridge wiring the other tests poke by hand.
    @Test
    fun footContactBridgesAfterLanding() {
        val pw = physicsWorld(gravityScale = 1f)
        val cfg = RunConfig()
        val runState = RunState().apply { highScore = Int.MAX_VALUE }
        val refs = Refs()
        val world = configureWorld {
            injectables { add(pw); add(cfg); add(runState); add(refs) }
            systems {
                add(PhysicsStepSystem())
                add(TransformSyncSystem())
                add(PlatformSystem())
            }
        }
        TestWorld(pw, world, runState, refs, cfg).use { tw ->
            // Slab at y=1.0, hole on the right; the ball lands on the wide solid-left half.
            val plat = world.spawnTestPlatform(pw, PLAT_Y, holeX = 3.0f, holeWidth = 1.0f)

            val factory = EntityFactory(world, pw, cfg, refs)
            // Over the solid-left half, just above the slab top, then let it fall.
            val player = factory.spawnPlayer(x = 0.5f, y = 1.5f, controller = BotController())
            refs.player = player

            pw.setContactListener(PlayContactListener(world))

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
