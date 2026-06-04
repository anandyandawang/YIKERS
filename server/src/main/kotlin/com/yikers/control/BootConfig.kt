package com.yikers.control

import com.badlogic.gdx.math.MathUtils

// Launch knobs read from system properties (preferred) or env vars at boot. There
// is NO humans count: 1 client == 1 player, and you are always exactly one human.
// `bots` is how many in-process bot clients to add to a run — pumped by the screen
// in singleplayer, or by the hosted server when hosting a LAN game. Examples:
//   YIKERS_BOTS=3        -> race 3 in-process bots
//   -Dyikers.seed=42     -> reproducible platform/boulder layout
object BootConfig {
    // Seed parsed at launch, read by the client into SessionConfig so the embedded
    // GameInstance reseeds deterministically. null = random layout.
    var seed: Long? = null
        private set

    // In-process bot clients to spawn for a run. 0 = pure single-human play.
    var bots: Int = 0
        private set

    fun apply() {
        intOf("yikers.bots", "YIKERS_BOTS")?.let { bots = it.coerceAtLeast(0) }
        seed = longOf("yikers.seed", "YIKERS_SEED")
        // Seed the global RNG too: harmless before the instance reseeds, and the
        // seeded-layout integration test relies on apply() alone seeding it.
        seed?.let { MathUtils.random.setSeed(it) }
    }

    private fun raw(prop: String, env: String): String? =
        System.getProperty(prop) ?: System.getenv(env)

    private fun intOf(prop: String, env: String) = raw(prop, env)?.trim()?.toIntOrNull()
    private fun longOf(prop: String, env: String) = raw(prop, env)?.trim()?.toLongOrNull()
}
