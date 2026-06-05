// Client-shared = the client seam shared by :client and :bot (both ARE clients, not
// the authority). Holds the play-as-a-client types: GameSession/GameHost/InputAgent/
// Participant + the socket impls NetworkGameSession/NetworkHost. NOT the wire contract
// (that stays :shared, which the server also needs); pure JVM, no gdx/Box2D/Fleks.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // implementation, not api: the seam names :shared types (WorldSnapshot, InputCommand)
    // in its signatures, but :client and :bot declare :shared directly, so no need to
    // re-export it. Anyone wanting only the seam does not get :shared dragged along.
    implementation(project(":shared"))
}
