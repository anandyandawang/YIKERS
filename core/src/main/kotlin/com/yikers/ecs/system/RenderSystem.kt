package com.yikers.ecs.system

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.component.RenderShape
import com.yikers.ecs.component.ShapeKind
import com.yikers.ecs.component.Transform
import com.yikers.ecs.resource.RunState

// Asset-free draw pass via ShapeRenderer. Separate from the HUD's SpriteBatch.
class RenderSystem(
    private val shape: ShapeRenderer = inject(),
    private val cam: OrthographicCamera = inject(),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    private val renderables = world.family { all(Transform, RenderShape) }
    private val platforms = world.family { all(PlatformC) }

    override fun onTick() {
        // Render path: kill-line (scrollY) is the view's bottom edge; center the
        // cam half a view-height above it so [scrollY, scrollY+viewHeight] shows.
        // ScrollSystem (runs earlier) already advanced scrollY.
        val viewBottom = runState.scrollY
        val viewH = runState.viewHeight
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
        platforms.forEach { e ->
            val p = e[PlatformC]
            shape.rect(0f, p.y, p.holeX, GameConfig.PLATFORM_HEIGHT)
            shape.rect(
                p.holeX + p.holeWidth, p.y,
                GameConfig.WIDTH - (p.holeX + p.holeWidth), GameConfig.PLATFORM_HEIGHT,
            )
        }

        renderables.forEach { e ->
            val t = e[Transform]
            val rs = e[RenderShape]
            shape.color = rs.color
            when (rs.kind) {
                ShapeKind.CIRCLE -> shape.circle(t.position.x, t.position.y, t.size.x / 2f, 24)
                ShapeKind.RECT -> shape.rect(
                    t.position.x - t.size.x / 2f, t.position.y - t.size.y / 2f,
                    t.size.x, t.size.y,
                )
            }
        }
        shape.end()
    }
}
