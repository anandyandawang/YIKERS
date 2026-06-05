// Client-shared = the client seam shared by :client and :bot (both ARE clients, not
// the authority). Holds the play-as-a-client types: GameSession/GameHost/InputAgent/
// Participant + the socket impls NetworkGameSession/NetworkHost. NOT the wire contract
// (that stays :shared, which the server also needs); pure JVM, no gdx/Box2D/Fleks.
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

dependencies {
    // api, not implementation: the seam types name :shared contract types (WorldSnapshot,
    // SessionConfig, InputCommand) in their signatures, so consumers must see them too.
    api(project(":shared"))
}
