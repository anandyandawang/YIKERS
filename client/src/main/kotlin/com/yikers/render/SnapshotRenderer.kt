package com.yikers.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.yikers.config.GameConfig
import com.yikers.net.ShapeKind
import com.yikers.net.WorldSnapshot

class SnapshotRenderer(
    private val shape: ShapeRenderer,
    private val cam: OrthographicCamera,
) {
    // Reused so flattened r/g/b/a doesn't allocate a Color per entity per frame.
    private val tint = Color()

    // viewH = this client's local view height; never crosses the seam.
    fun render(snap: WorldSnapshot, viewH: Float) {
        // scrollY = view bottom; center the cam half a view-height above it.
        val viewBottom = snap.scrollY
        cam.position.y = viewBottom + viewH / 2f
        cam.update()
        shape.projectionMatrix = cam.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)

        shape.color = Color.DARK_GRAY
        shape.rect(0f, 0f, GameConfig.WIDTH, GameConfig.GROUND_HEIGHT)

        shape.color = Color.GRAY
        shape.rect(0f, viewBottom, GameConfig.WALL_THICKNESS, viewH)
        shape.rect(
            GameConfig.WIDTH - GameConfig.WALL_THICKNESS, viewBottom,
            GameConfig.WALL_THICKNESS, viewH,
        )

        shape.color = Color.SLATE
        snap.platforms.forEach { p ->
            shape.rect(0f, p.y, p.holeX, GameConfig.PLATFORM_HEIGHT)
            shape.rect(
                p.holeX + p.holeWidth, p.y,
                GameConfig.WIDTH - (p.holeX + p.holeWidth), GameConfig.PLATFORM_HEIGHT,
            )
        }

        snap.entities.forEach { e ->
            tint.set(e.r, e.g, e.b, e.a)
            shape.color = tint
            when (e.kind) {
                ShapeKind.CIRCLE -> shape.circle(e.x, e.y, e.sizeX / 2f, 24)
                ShapeKind.RECT -> shape.rect(
                    e.x - e.sizeX / 2f, e.y - e.sizeY / 2f,
                    e.sizeX, e.sizeY,
                )
            }
        }
        shape.end()
    }
}
