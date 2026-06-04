// Shared = pure-Kotlin contract between client and server: configs + the net seam
// types (InputCommand, WorldSnapshot, SessionConfig, GameSession/GameHost). No gdx,
// no Box2D, no Fleks — so neither side drags the other's engine in.
plugins {
    alias(libs.plugins.kotlin.jvm)
    // kotlinx-serialization: generates the CBOR codec for the @Serializable wire
    // types (net seam). Compiler plugin only — no runtime engine deps leak in.
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // CBOR runtime for the wire types + protocol envelope. The ONLY dep :shared
    // carries; still no gdx / Box2D / Fleks, so the pure-contract rule holds.
    implementation(libs.kotlinx.serialization.cbor)
}
