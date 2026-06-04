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
                setTitle(System.getProperty("yikers.title") ?: "YIKERS")
                // Portrait. Climber go up.
                setWindowedMode(GameConfig.WIDTH_PX.toInt(), GameConfig.HEIGHT_PX.toInt())
                // Optional fixed window placement (yikers.winx/winy) — lets a demo or a
                // two-client setup pin each window side by side instead of stacking.
                val wx = System.getProperty("yikers.winx")?.toIntOrNull()
                val wy = System.getProperty("yikers.winy")?.toIntOrNull()
                if (wx != null && wy != null) setWindowPosition(wx, wy)
                useVsync(true)
                setForegroundFPS(60)
            },
        )
    }
}
