package com.yikers.ios

import com.badlogic.gdx.backends.iosrobovm.IOSApplication
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration
import com.yikers.YikersGame
import com.yikers.control.BootConfig
import org.robovm.apple.foundation.NSAutoreleasePool
import org.robovm.apple.uikit.UIApplication

/**
 * iOS entry point. Boots libGDX with the RoboVM backend.
 * Mirrors Lwjgl3Launcher boot order: BootConfig.apply() then YikersGame().
 * Horizontal control comes from the accelerometer (tilt) and jump from a
 * screen tap — both handled in shared core (HumanController), so this
 * launcher only wires up the platform backend.
 */
class IOSLauncher : IOSApplication.Delegate() {
    override fun createApplication(): IOSApplication {
        BootConfig.apply() // roster + seed from sysprops/env before the app starts
        val config = IOSApplicationConfiguration()
        return IOSApplication(YikersGame(), config)
    }

    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val pool = NSAutoreleasePool()
            // null principal class -> default UIApplication; cast so Kotlin
            // can infer the generic type parameter.
            UIApplication.main(argv, null as Class<UIApplication>?, IOSLauncher::class.java)
            pool.close()
        }
    }
}
