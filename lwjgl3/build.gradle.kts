// Desktop launcher. LWJGL3 backend + desktop natives.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("com.yikers.lwjgl3.Lwjgl3Launcher")
}

dependencies {
    implementation(project(":core"))
    // ktx-app: YikersGame extends KtxGame, so launcher needs that type chain visible.
    implementation(libs.ktx.app)
    implementation(libs.gdx.backend.lwjgl3)
    implementation(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
    // Box2D native lib for desktop. Omit = UnsatisfiedLinkError.
    implementation(variantOf(libs.gdx.box2d.platform) { classifier("natives-desktop") })
}
