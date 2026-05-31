// Core = platform-agnostic game logic. No backend here.
plugins {
    kotlin("jvm")
}

val gdxVersion: String by project

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
}
