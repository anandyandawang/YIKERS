package com.yikers.net

import com.yikers.config.RunConfig
import kotlinx.serialization.Serializable

// Everything needed to start a run, handed to a host when opening a room. Sent
// server -> client once in the Welcome handshake (in-process for singleplayer).
@Serializable
data class SessionConfig(
    val humans: Int,
    val bots: Int,
    val seed: Long? = null,                       // RNG seed; null = random layout
    val previousHighScore: Int = 0,               // for in-run HIGH display
    val runConfig: RunConfig = RunConfig(),       // feel knobs (roguelike layer later)
)
