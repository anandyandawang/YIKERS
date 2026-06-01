package com.yikers.ecs.system

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.ecs.resource.RunState

// Score readout + game-over overlay. SpriteBatch pass, drawn after shapes.
class HudSystem(
    private val batch: SpriteBatch = inject(),
    private val font: BitmapFont = inject(),
    private val cam: OrthographicCamera = inject(),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    private val layout = GlyphLayout()

    override fun onTick() {
        batch.projectionMatrix = cam.combined
        batch.begin()
        val top = cam.position.y + GameConfig.HEIGHT / 2f
        font.color = Color.WHITE
        font.draw(batch, "SCORE ${runState.score}", 12f, top - 12f)

        if (runState.dead) {
            font.color = Color.CORAL
            centered("GAME OVER", cam.position.y + 80f)
            font.color = Color.WHITE
            centered("SCORE ${runState.score}", cam.position.y + 20f)
            centered("HIGH ${runState.highScore}", cam.position.y - 20f)
            centered("press space", cam.position.y - 90f)
        }
        batch.end()
    }

    private fun centered(text: String, y: Float) {
        layout.setText(font, text)
        font.draw(batch, text, cam.position.x - layout.width / 2f, y)
    }
}
