// Core = platform-agnostic game logic. No backend here.
plugins {
    kotlin("jvm")
}

val gdxVersion: String by project
val fleksVersion: String by project
val ktxVersion: String by project

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    // Fleks = modern Kotlin ECS. Game sim live here.
    implementation("io.github.quillraven.fleks:Fleks:$fleksVersion")
    // ktx = idiomatic Kotlin libGDX helpers.
    implementation("io.github.libktx:ktx-app:$ktxVersion")
    implementation("io.github.libktx:ktx-box2d:$ktxVersion")
    implementation("io.github.libktx:ktx-graphics:$ktxVersion")
    implementation("io.github.libktx:ktx-math:$ktxVersion")
}
