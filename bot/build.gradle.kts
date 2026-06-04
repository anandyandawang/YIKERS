// Bot client = a headless client. It connects to a server over the socket exactly
// like a human's client and drives a player with the shared BotBrain. Pure JVM: NO
// gdx / Box2D / Fleks, and crucially NO dependency on :server — a bot is a client,
// not part of the authority. It needs only the shared seam (GameSession, the wire
// transport NetworkHost/NetworkGameSession, and the BotAgent).
plugins {
    alias(libs.plugins.kotlin.jvm)
    // `application` = runnable: ./gradlew :bot:run (connects bots to a server).
    application
}

application {
    mainClass.set("com.yikers.bot.app.BotMainKt")
}

dependencies {
    implementation(project(":shared"))
}
