package com.yikers.control

import com.badlogic.gdx.math.MathUtils

// Launch knobs from system properties / env vars. yikers.seed only.
object BootConfig {
    var seed: Long? = null      // null = random layout
        private set

    fun apply() {
        seed = longOf("yikers.seed", "YIKERS_SEED")
        seed?.let { MathUtils.random.setSeed(it) }   // seeded-layout test relies on this
    }

    private fun raw(prop: String, env: String): String? =
        System.getProperty(prop) ?: System.getenv(env)

    private fun longOf(prop: String, env: String) = raw(prop, env)?.trim()?.toLongOrNull()
}
