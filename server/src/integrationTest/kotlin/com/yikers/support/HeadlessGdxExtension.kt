package com.yikers.support

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

// Boots a no-op headless libGDX app ONCE per JVM: sets Gdx.app/files + loads the
// gdx native. Box2D's native loads lazily on the first createWorld. Gdx.gl stays
// null (no GL), so any GL-bound system NPEs at once if added to a test world.
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
