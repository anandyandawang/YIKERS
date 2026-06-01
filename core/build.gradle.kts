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

    // --- headless integration test ---
    // Headless backend: boots Gdx env (app/files) + loads gdx native, no GL.
    testImplementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    // Desktop natives at test runtime: headless backend ships none, Box2D needs them.
    testRuntimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    testRuntimeOnly("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}
