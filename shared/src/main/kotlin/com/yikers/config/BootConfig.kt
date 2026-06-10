package com.yikers.config

// Launch knobs parsed once at boot (launchers call apply() before the app starts).
// Engine-free: the seed only takes effect when a GameInstance is built from it.
object BootConfig {
    var seed: Long? = null      // null = random layout
        private set

    fun apply() {
        seed = Knobs.long("yikers.seed", "YIKERS_SEED")
    }
}
