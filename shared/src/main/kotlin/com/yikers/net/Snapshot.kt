package com.yikers.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// One renderable entity. Meters, center-origin. Sealed: a PlayerSnap owns a seat
// (slot); a PropSnap (boulder etc) carries only its entity id. Geometry lives on
// the interface, so a renderer draws any EntitySnap without caring which it is.
@Serializable
sealed interface EntitySnap {
    val kind: ShapeKind
    val r: Float; val g: Float; val b: Float; val a: Float
    val x: Float; val y: Float
    val sizeX: Float; val sizeY: Float
    val rotation: Float
}

@Serializable
@SerialName("p")
data class PlayerSnap(
    override val kind: ShapeKind,
    override val r: Float, override val g: Float, override val b: Float, override val a: Float,
    override val x: Float, override val y: Float,
    override val sizeX: Float, override val sizeY: Float,
    override val rotation: Float,
    val slot: Int,                 // the seat this ball owns (matches Player.slot)
    val augments: List<AugmentId> = emptyList(),  // owned, oldest first
    val offer: List<AugmentId> = emptyList(),     // pending augment offer; empty = none
) : EntitySnap

@Serializable
@SerialName("o")
data class PropSnap(
    override val kind: ShapeKind,
    override val r: Float, override val g: Float, override val b: Float, override val a: Float,
    override val x: Float, override val y: Float,
    override val sizeX: Float, override val sizeY: Float,
    override val rotation: Float,
    val id: Int,                   // entity id; lets the bot track a boulder frame-to-frame
) : EntitySnap

@Serializable
data class PlatformSnap(val y: Float, val holeX: Float, val holeWidth: Float)

// One frame for the client. Arena redrawn from GameConfig, so omitted.
@Serializable
data class WorldSnapshot(
    val tick: Long,
    val entities: List<EntitySnap>,
    val platforms: List<PlatformSnap>,
    val score: Int,
    val dead: Boolean,
    val scrollY: Float,        // kill-line = camera bottom edge
    val highScore: Int,
) {
    companion object {
        // The zero frame: what a session shows before its first server snapshot.
        val EMPTY = WorldSnapshot(
            tick = 0L,
            entities = emptyList(),
            platforms = emptyList(),
            score = 0,
            dead = false,
            scrollY = 0f,
            highScore = 0,
        )
    }
}
