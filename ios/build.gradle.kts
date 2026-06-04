// iOS launcher. RoboVM/MobiVM backend + iOS natives.
// NOTE: this module only builds on macOS with Xcode + the iOS SDK
// installed. It cannot be compiled on Linux/CI without them.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

// RoboVM plugin ships no plugins{} marker; apply it the legacy way.
// Its classpath is declared in the root buildscript block.
apply(plugin = "robovm")

dependencies {
    implementation(project(":client"))
    // ktx-app: YikersGame extends KtxGame, so launcher needs that type chain.
    implementation(libs.ktx.app)
    implementation(libs.gdx.backend.robovm)
    implementation(variantOf(libs.gdx.platform) { classifier("natives-ios") })
    // Box2D native lib for iOS. Omit = UnsatisfiedLinkError on device.
    implementation(variantOf(libs.gdx.box2d.platform) { classifier("natives-ios") })
    implementation(libs.robovm.rt)
    implementation(libs.robovm.cocoatouch)
}
