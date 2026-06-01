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

// Asset-free draw pass via ShapeRenderer. Separate from the HUD's SpriteBatch.
class RenderSystem(
    private val shape: ShapeRenderer = inject(),
    private val cam: OrthographicCamera = inject(),
) : IntervalSystem() {
    private val renderables = world.family { all(Transform, RenderShape) }
    private val platforms = world.family { all(PlatformC) }

    override fun onTick() {
        shape.projectionMatrix = cam.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)

        val viewBottom = cam.position.y - GameConfig.HEIGHT / 2f

        shape.color = Color.DARK_GRAY
        shape.rect(0f, 0f, GameConfig.WIDTH, GameConfig.GROUND_HEIGHT)

        shape.color = Color.GRAY
        shape.rect(0f, viewBottom, GameConfig.WALL_THICKNESS, GameConfig.HEIGHT)
        shape.rect(
            GameConfig.WIDTH - GameConfig.WALL_THICKNESS, viewBottom,
            GameConfig.WALL_THICKNESS, GameConfig.HEIGHT,
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
