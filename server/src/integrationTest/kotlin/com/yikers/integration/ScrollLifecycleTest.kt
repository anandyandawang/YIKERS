package com.yikers.integration

import com.yikers.support.HeadlessGdx
import com.yikers.support.buildSim
import com.yikers.support.stepSeconds
import com.yikers.support.stepUntil
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// ScrollSystem owns the kill-line lifecycle: it stays still on the ground floor,
// arms once a climber actually reaches the first platform, then accelerates the
// kill-line (RunState.scrollY) upward. Drives the real sim and watches the arming.
@HeadlessGdx
class ScrollLifecycleTest {

    @Test
    fun killLineHoldsUntilPlatformReachedThenScrolls() {
        buildSim(seed = SEED).use { h ->
            // At spawn the lone bot rests on the ground floor: kill-line idle.
            assertFalse(h.startCamera) { "scroll must not start on the ground floor" }
            assertTrue(h.scrollY == 0f) { "kill-line sits at 0 until a platform is reached" }

            // Bot climbs; once it foot-lands on the first real platform, the cam arms.
            val armed = h.world.stepUntil(MAX_SECONDS) { h.startCamera }
            assertTrue(armed) { "camera should arm once a climber reaches a platform" }

            // Armed -> kill-line must now rise over time.
            val before = h.scrollY
            h.world.stepSeconds(1f)
            assertTrue(h.scrollY > before) {
                "kill-line must rise once armed; before=$before now=${h.scrollY}"
            }
        }
    }

    companion object {
        private const val SEED = 42L
        private const val MAX_SECONDS = 20f
    }
}
