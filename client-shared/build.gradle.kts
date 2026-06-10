// Client seam shared by :client and :bot: GameSession/InputAgent/Participant
// + the socket impl. Not the wire contract (:shared); off the server. Pure JVM.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":shared"))
}
