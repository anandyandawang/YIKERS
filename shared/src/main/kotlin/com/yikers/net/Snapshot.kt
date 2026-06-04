package com.yikers.net

import kotlinx.serialization.Serializable

// One renderable entity. Meters, center-origin. playerId = owning slot, else -1.
@Serializable
data class EntitySnap(
    val kind: ShapeKind,
    val r: Float, val g: Float, val b: Float, val a: Float,
    val x: Float, val y: Float,
    val sizeX: Float, val sizeY: Float,
    val rotation: Float,
    val id: Int = -1,
    val playerId: Int = -1,
)

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
)
