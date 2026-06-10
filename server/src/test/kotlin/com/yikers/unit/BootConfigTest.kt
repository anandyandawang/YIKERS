package com.yikers.unit

import com.github.quillraven.fleks.configureWorld
import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.control.BootConfig
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.level.ClassicGenerator
import com.yikers.support.HeadlessGdx
import com.yikers.support.TestWorld
import com.yikers.support.physicsWorld
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// Same seed reproduces the same procedural platform layout.
@HeadlessGdx
class BootConfigTest {

    @AfterEach
    fun reset() {
        System.clearProperty("yikers.seed")
    }

    @Test
    fun seedMakesPlatformLayoutReproducible() {
        System.setProperty("yikers.seed", "12345")
        val first = platformHoles()

        System.setProperty("yikers.seed", "12345")
        val second = platformHoles()

        assertEquals(first, second) { "same seed must yield the same hole layout" }
    }

    private fun platformHoles(): List<Float> {
        BootConfig.apply()
        val pw = physicsWorld(gravityScale = 0f)
        val cfg = RunConfig()
        val refs = Refs()
        val world = configureWorld { }
        return TestWorld(pw, world, RunState(), refs, cfg).use { _ ->
            val factory = EntityFactory(world, pw, refs)
            val gen = ClassicGenerator(cfg)
            with(world) {
                (1..GameConfig.NUM_PLATFORMS).map { i ->
                    val y = GameConfig.GROUND_HEIGHT + i * GameConfig.PLATFORM_INTERVALS
                    val e = factory.spawnPlatform(y, gen.nextPlatform(y))
                    e[PlatformC].holeX
                }
            }
        }
    }
}
