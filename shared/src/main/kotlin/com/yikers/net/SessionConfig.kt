package com.yikers.net

import com.yikers.config.RunConfig
import kotlinx.serialization.Serializable

// Run params, sent server -> client once in the Welcome handshake.
@Serializable
data class SessionConfig(
    val seed: Long? = null,                       // RNG seed; null = random layout
    val previousHighScore: Int = 0,               // for in-run HIGH display
    val runConfig: RunConfig = RunConfig(),       // feel knobs (roguelike layer later)
)
