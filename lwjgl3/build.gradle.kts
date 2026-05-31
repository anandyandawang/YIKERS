// Desktop launcher. LWJGL3 backend + desktop natives.
plugins {
    kotlin("jvm")
    application
}

val gdxVersion: String by project

application {
    mainClass.set("com.yikers.lwjgl3.Lwjgl3Launcher")
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}
