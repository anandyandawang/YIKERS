package com.yikers.support

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

// Boots a headless libGDX app once per JVM. Gdx.gl stays null, so any GL-bound
// system NPEs if added to a test world.
class HeadlessGdxExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        if (Gdx.app == null) {
            HeadlessApplication(object : ApplicationAdapter() {}, HeadlessApplicationConfiguration())
        }
    }
}

// Put @HeadlessGdx on a test class to boot the headless env before its tests run.
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(HeadlessGdxExtension::class)
annotation class HeadlessGdx
