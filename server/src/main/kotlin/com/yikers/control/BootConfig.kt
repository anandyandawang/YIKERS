package com.yikers.control

import com.badlogic.gdx.math.MathUtils

// Launch knobs from system properties / env vars. No humans count (1 client == 1
// player); `bots` = in-process bot clients to add to a run. yikers.bots, yikers.seed.
object BootConfig {
    var seed: Long? = null      // null = random layout
        private set

    var bots: Int = 0           // in-process bots; 0 = solo human
        private set

    fun apply() {
        intOf("yikers.bots", "YIKERS_BOTS")?.let { bots = it.coerceAtLeast(0) }
        seed = longOf("yikers.seed", "YIKERS_SEED")
        seed?.let { MathUtils.random.setSeed(it) }   // seeded-layout test relies on this
    }

    private fun raw(prop: String, env: String): String? =
        System.getProperty(prop) ?: System.getenv(env)

    private fun intOf(prop: String, env: String) = raw(prop, env)?.trim()?.toIntOrNull()
    private fun longOf(prop: String, env: String) = raw(prop, env)?.trim()?.toLongOrNull()
}
