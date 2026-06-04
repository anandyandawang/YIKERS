package com.yikers.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.yikers.YikersGame
import com.yikers.control.BootConfig

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BootConfig.apply() // seed from sysprops/env before app starts
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = true // tilt = horizontal control
            useCompass = false
            useImmersiveMode = true
        }
        initialize(YikersGame(), config)
    }
}
