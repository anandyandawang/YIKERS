// Headless bot client. Pure JVM (no gdx/Box2D/Fleks), and NO :server dependency — a
// bot is a client, not part of the authority. Needs the wire contract + client seam.
plugins {
    alias(libs.plugins.kotlin.jvm)
    // `application` = runnable: ./gradlew :bot:run (connects one bot to a server).
    application
}

application {
    mainClass.set("com.yikers.bot.app.BotMainKt")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":client-shared"))
}
