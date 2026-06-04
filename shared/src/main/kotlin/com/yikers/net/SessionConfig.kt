package com.yikers.net

import com.yikers.config.GameConfig
import com.yikers.config.RunConfig

// Everything needed to start a run, handed to a host when opening a room. Sent
// client -> server once at session open (in-process today, over a socket later).
data class SessionConfig(
    val humans: Int,
    val bots: Int,
    val seed: Long? = null,                       // RNG seed; null = random layout
    val viewHeight: Float = GameConfig.HEIGHT,    // client's visible world height
    val previousHighScore: Int = 0,               // for in-run HIGH display
    val runConfig: RunConfig = RunConfig(),       // feel knobs (roguelike layer later)
)
