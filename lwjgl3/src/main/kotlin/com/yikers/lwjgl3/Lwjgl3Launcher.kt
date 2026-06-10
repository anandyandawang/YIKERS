package com.yikers.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.yikers.YikersGame
import com.yikers.config.BootConfig
import com.yikers.config.GameConfig

object Lwjgl3Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        BootConfig.apply() // seed from sysprops/env before app starts
        Lwjgl3Application(
            YikersGame(),
            Lwjgl3ApplicationConfiguration().apply {
                setTitle(System.getProperty("yikers.title") ?: "YIKERS")
                setWindowedMode(GameConfig.WIDTH_PX.toInt(), GameConfig.HEIGHT_PX.toInt())
                // Optional fixed window placement (yikers.winx/winy) for side-by-side demos.
                val wx = System.getProperty("yikers.winx")?.toIntOrNull()
                val wy = System.getProperty("yikers.winy")?.toIntOrNull()
                if (wx != null && wy != null) setWindowPosition(wx, wy)
                useVsync(true)
                setForegroundFPS(60)
            },
        )
    }
}
