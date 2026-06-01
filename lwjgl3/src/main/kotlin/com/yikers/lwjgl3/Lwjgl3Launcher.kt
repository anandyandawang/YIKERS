package com.yikers.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.yikers.YikersGame
import com.yikers.config.GameConfig
import com.yikers.control.BootConfig

/** Desktop entry point. Boots libGDX with LWJGL3 backend. */
object Lwjgl3Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        BootConfig.apply() // roster + seed from sysprops/env before the app starts
        Lwjgl3Application(
            YikersGame(),
            Lwjgl3ApplicationConfiguration().apply {
                setTitle("YIKERS")
                // Portrait. Climber go up.
                setWindowedMode(GameConfig.WIDTH_PX.toInt(), GameConfig.HEIGHT_PX.toInt())
                useVsync(true)
                setForegroundFPS(60)
            },
        )
    }
}
