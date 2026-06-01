package com.yikers.config

import com.badlogic.gdx.Gdx

// One spot for save keys. highscore used v1; rest reserved for meta-progress.
// [SEAM] character unlocks / checkpoints write here later.
object Prefs {
    private const val FILE = "yikers"
    private const val KEY_HIGHSCORE = "highscore"

    @Suppress("unused") private const val KEY_UNLOCKED = "unlockedCharacters"
    @Suppress("unused") private const val KEY_CHECKPOINTS = "checkpoints"

    private val prefs get() = Gdx.app.getPreferences(FILE)

    var highScore: Int
        get() = prefs.getInteger(KEY_HIGHSCORE, 0)
        set(value) {
            prefs.putInteger(KEY_HIGHSCORE, value)
            prefs.flush()
        }
}
