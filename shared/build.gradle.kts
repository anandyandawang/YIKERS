// Shared = pure-Kotlin contract between client and server: configs + the net seam
// types (InputCommand, WorldSnapshot, SessionConfig, GameSession/GameHost). No gdx,
// no Box2D, no Fleks — so neither side drags the other's engine in.
plugins {
    alias(libs.plugins.kotlin.jvm)
}
