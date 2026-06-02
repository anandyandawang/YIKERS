// iOS launcher. RoboVM/MobiVM backend + iOS natives.
// NOTE: this module only builds on macOS with Xcode + the iOS SDK
// installed. It cannot be compiled on Linux/CI without them.
plugins {
    kotlin("jvm")
}

// RoboVM plugin ships no plugins{} marker; apply it the legacy way.
// Its classpath is declared in the root buildscript block.
apply(plugin = "robovm")

val gdxVersion: String by project
val ktxVersion: String by project
val robovmVersion: String by project

dependencies {
    implementation(project(":core"))
    // ktx-app: YikersGame extends KtxGame, so launcher needs that type chain.
    implementation("io.github.libktx:ktx-app:$ktxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-robovm:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-ios")
    // Box2D native lib for iOS. Omit = UnsatisfiedLinkError on device.
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-ios")
    implementation("com.mobidevelop.robovm:robovm-rt:$robovmVersion")
    implementation("com.mobidevelop.robovm:robovm-cocoatouch:$robovmVersion")
}
