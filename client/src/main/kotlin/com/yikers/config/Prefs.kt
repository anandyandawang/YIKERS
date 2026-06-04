package com.yikers.config

import com.badlogic.gdx.Gdx

// Save keys. [SEAM] meta-progress (unlocks/checkpoints) writes here later.
object Prefs {
    private const val FILE = "yikers"
    private const val KEY_HIGHSCORE = "highscore"

    @Suppress("unused") private const val KEY_UNLOCKED = "unlockedCharacters"
    @Suppress("unused") private const val KEY_CHECKPOINTS = "checkpoints"

    // Cache one instance: iOS getPreferences() builds a NEW IOSPreferences each
    // call, so splitting put + flush across instances loses the write on iOS.
    private val prefs by lazy { Gdx.app.getPreferences(FILE) }

    var highScore: Int
        get() = prefs.getInteger(KEY_HIGHSCORE, 0)
        set(value) {
            prefs.putInteger(KEY_HIGHSCORE, value)
            prefs.flush()
        }
}
