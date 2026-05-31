package com.yikers.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.yikers.YikersGame

/** Desktop entry point. Boots libGDX with LWJGL3 backend. */
object Lwjgl3Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        Lwjgl3Application(
            YikersGame(),
            Lwjgl3ApplicationConfiguration().apply {
                setTitle("YIKERS")
                setWindowedMode(800, 480)
                useVsync(true)
                setForegroundFPS(60)
            },
        )
    }
}
