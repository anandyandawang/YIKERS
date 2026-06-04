package com.yikers.net

import com.yikers.config.RunConfig
import kotlinx.serialization.Serializable

// Everything needed to start a run, handed to a host when opening a room. Sent
// server -> client once in the Welcome handshake (in-process for singleplayer).
// No humans/bots count: the roster is dynamic (one player per joined client) and
// the server treats every client identically — a bot is just another client.
@Serializable
data class SessionConfig(
    val seed: Long? = null,                       // RNG seed; null = random layout
    val previousHighScore: Int = 0,               // for in-run HIGH display
    val runConfig: RunConfig = RunConfig(),       // feel knobs (roguelike layer later)
)
