package com.yikers.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.yikers.config.GameConfig
import com.yikers.net.ShapeKind
import com.yikers.net.WorldSnapshot

// Asset-free draw pass via ShapeRenderer, fed purely by a WorldSnapshot from the
// server. This is the old RenderSystem.onTick body with ECS family iteration
// swapped for snapshot lists: same camera math, same arena draw from GameConfig,
// same platform halves, same per-entity circle/rect. Separate from the HUD batch.
class SnapshotRenderer(
    private val shape: ShapeRenderer,
    private val cam: OrthographicCamera,
) {
    // Reused so each entity's flattened r/g/b/a doesn't allocate a Color per frame.
    private val tint = Color()

    // viewH is THIS client's local visible world height (device aspect via the
    // viewport). It never crosses the seam — each client centers its own camera.
    fun render(snap: WorldSnapshot, viewH: Float) {
        // kill-line (scrollY) is the view's bottom edge; center the cam half a
        // view-height above it so [scrollY, scrollY + viewH] shows.
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
