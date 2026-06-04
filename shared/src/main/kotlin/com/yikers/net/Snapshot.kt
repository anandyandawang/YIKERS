package com.yikers.net

// One renderable entity (ball or boulder). Color is flattened to r/g/b/a floats so
// a future socket transport needs no custom Color serializer; the client rebuilds
// a Color at draw time. Positions/sizes are meters, center-origin.
data class EntitySnap(
    val kind: ShapeKind,
    val r: Float, val g: Float, val b: Float, val a: Float,
    val x: Float, val y: Float,
    val sizeX: Float, val sizeY: Float,
    val rotation: Float,
)

// One platform: a solid slab at y with a gap spanning [holeX, holeX + holeWidth].
data class PlatformSnap(val y: Float, val holeX: Float, val holeWidth: Float)

// Everything the client needs to render one frame. No ECS / Box2D types leak — the
// arena (ground + walls) is redrawn client-side from GameConfig, so it is omitted.
data class WorldSnapshot(
    val tick: Long,
    val entities: List<EntitySnap>,
    val platforms: List<PlatformSnap>,
    val score: Int,
    val dead: Boolean,
    val scrollY: Float,        // kill-line = camera bottom edge
    val highScore: Int,
    val viewHeight: Float,
)
