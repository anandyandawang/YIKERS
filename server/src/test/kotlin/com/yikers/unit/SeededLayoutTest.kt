package com.yikers.unit

import com.yikers.config.BootConfig
import com.yikers.net.SessionConfig
import com.yikers.sim.GameInstance
import com.yikers.support.HeadlessGdx
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// The seed knob end to end: BootConfig parses it, GameInstance reproduces the
// same procedural platform layout from it.
@HeadlessGdx
class SeededLayoutTest {

    @AfterEach
    fun reset() {
        System.clearProperty("yikers.seed")
    }

    @Test
    fun bootConfigParsesSeedKnob() {
        System.setProperty("yikers.seed", "12345")
        BootConfig.apply()
        assertEquals(12345L, BootConfig.seed) { "-Dyikers.seed must parse into BootConfig.seed" }
    }

    @Test
    fun sameSeedReproducesPlatformLayout() {
        assertEquals(platformHoles(SEED), platformHoles(SEED)) {
            "same seed must yield the same hole layout"
        }
    }

    // The production path: GameInstance seeds the RNG from SessionConfig itself.
    private fun platformHoles(seed: Long): List<Float> {
        val instance = GameInstance(SessionConfig(seed = seed))
        return try {
            instance.snapshot().platforms.map { it.holeX }
        } finally {
            instance.close()
        }
    }

    companion object {
        private const val SEED = 12345L
    }
}
