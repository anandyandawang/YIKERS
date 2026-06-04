package com.yikers.net

import kotlinx.serialization.Serializable

// One renderable entity (ball or boulder). Color is flattened to r/g/b/a floats so
// the transport needs no Color serializer. Meters, center-origin. `id` = stable
// per-entity handle (track an object across frames); `playerId` = owning slot for a
// player ball, else -1 (lets a client find its own ball).
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

// One platform: a solid slab at y with a gap spanning [holeX, holeX + holeWidth].
@Serializable
data class PlatformSnap(val y: Float, val holeX: Float, val holeWidth: Float)

// Everything the client needs to render one frame. No ECS / Box2D types leak — the
// arena (ground + walls) is redrawn client-side from GameConfig, so it is omitted.
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
