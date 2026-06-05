// Client seam shared by :client and :bot: GameSession/GameHost/InputAgent/Participant
// + socket impls. Not the wire contract (:shared); off the server. Pure JVM.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":shared"))
}
