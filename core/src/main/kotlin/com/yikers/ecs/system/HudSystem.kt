package com.yikers.ecs.system

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.ecs.resource.RunState

// Score readout + game-over overlay. SpriteBatch pass, drawn after shapes.
// Uses its own pixel-space camera (world cam is meters now), so the font draws
// at native px size and the HUD stays screen-fixed (doesn't scroll with world).
class HudSystem(
    private val batch: SpriteBatch = inject(),
    private val font: BitmapFont = inject(),
    private val cam: OrthographicCamera = inject("hud"),
    private val runState: RunState = inject(),
) : IntervalSystem() {
    private val layout = GlyphLayout()

    override fun onTick() {
        batch.projectionMatrix = cam.combined
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "SCORE ${runState.score}", 12f, 800f - 12f)

        if (runState.dead) {
            font.color = Color.CORAL
            centered("GAME OVER", 400f + 80f)
            font.color = Color.WHITE
            centered("SCORE ${runState.score}", 400f + 20f)
            centered("HIGH ${runState.highScore}", 400f - 20f)
            centered("press space", 400f - 90f)
        }
        batch.end()
    }

    private fun centered(text: String, y: Float) {
        layout.setText(font, text)
        font.draw(batch, text, 240f - layout.width / 2f, y)
    }
}
