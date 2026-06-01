// Desktop launcher. LWJGL3 backend + desktop natives.
plugins {
    kotlin("jvm")
    application
}

val gdxVersion: String by project
val ktxVersion: String by project

application {
    mainClass.set("com.yikers.lwjgl3.Lwjgl3Launcher")
}

dependencies {
    implementation(project(":core"))
    // ktx-app: YikersGame extends KtxGame, so launcher needs that type chain visible.
    implementation("io.github.libktx:ktx-app:$ktxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    // Box2D native lib for desktop. Omit = UnsatisfiedLinkError.
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")
}
