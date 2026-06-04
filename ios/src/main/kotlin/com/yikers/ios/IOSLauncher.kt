package com.yikers.ios

import com.badlogic.gdx.backends.iosrobovm.IOSApplication
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration
import com.yikers.YikersGame
import com.yikers.control.BootConfig
import org.robovm.apple.foundation.NSAutoreleasePool
import org.robovm.apple.uikit.UIApplication

class IOSLauncher : IOSApplication.Delegate() {
    override fun createApplication(): IOSApplication {
        BootConfig.apply() // seed from sysprops/env before app starts
        val config = IOSApplicationConfiguration()
        return IOSApplication(YikersGame(), config)
    }

    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val pool = NSAutoreleasePool()
            // null principal class -> default UIApplication; cast for type inference.
            UIApplication.main(argv, null as Class<UIApplication>?, IOSLauncher::class.java)
            pool.close()
        }
    }
}
