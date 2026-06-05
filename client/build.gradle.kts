// Client = render + input + camera + HUD + menu + game shell. Talks to the sim only
// through the GameSession seam (shared); embeds the server for singleplayer. No
// Box2D/Fleks imports here — those arrive transitively via :server at runtime only.
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

dependencies {
    // api so the launchers (which depend on :client) also see shared GameConfig and
    // server BootConfig — the only sim types a launcher touches at boot.
    api(project(":shared"))
    api(project(":server"))
    implementation(project(":client-shared"))
    implementation(libs.gdx)
    // ktx = idiomatic Kotlin libGDX helpers (app shell + graphics + math).
    implementation(libs.ktx.app)
    implementation(libs.ktx.graphics)
    implementation(libs.ktx.math)
}
