package com.yikers.control

import com.badlogic.gdx.math.MathUtils

// Reads the roster + optional RNG seed from system properties (preferred) or
// env vars at launch. Examples:
//   YIKERS_HUMANS=0 YIKERS_BOTS=2   -> two bots, hands-free
//   -Dyikers.seed=42                -> reproducible platform/boulder layout
object BootConfig {
    fun apply() {
        intOf("yikers.humans", "YIKERS_HUMANS")?.let { Roster.humans = it.coerceAtLeast(0) }
        intOf("yikers.bots", "YIKERS_BOTS")?.let { Roster.bots = it.coerceAtLeast(0) }
        longOf("yikers.seed", "YIKERS_SEED")?.let { MathUtils.random.setSeed(it) }
    }

    private fun raw(prop: String, env: String): String? =
        System.getProperty(prop) ?: System.getenv(env)

    private fun intOf(prop: String, env: String) = raw(prop, env)?.trim()?.toIntOrNull()
    private fun longOf(prop: String, env: String) = raw(prop, env)?.trim()?.toLongOrNull()
}
