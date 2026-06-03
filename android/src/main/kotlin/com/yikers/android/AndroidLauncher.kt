package com.yikers.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.yikers.YikersGame
import com.yikers.control.BootConfig

/**
 * Android entry point. Boots libGDX with the Android backend.
 * Mirrors the other launchers' boot order: BootConfig.apply() then YikersGame().
 * Horizontal control comes from the accelerometer (tilt) and jump from a screen
 * tap — both handled in shared core (HumanController), so this launcher only
 * wires up the platform backend.
 */
class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BootConfig.apply() // roster + seed from sysprops/env before the app starts
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = true // tilt = horizontal control (HumanController reads accelerometerX)
            useCompass = false
            useImmersiveMode = true // hide system bars for fullscreen play
        }
        initialize(YikersGame(), config)
    }
}
