package com.yikers.screen.hud

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.yikers.YikersGame
import com.yikers.net.WorldSnapshot

// Score readout, plus the game-over banner. Draw only; the screen owns the flow
// (input, score persistence, screen change).
class Hud(
    private val game: YikersGame,
    private val viewport: ExtendViewport,
) {
    private val layout = GlyphLayout()

    fun render(snap: WorldSnapshot) {
        viewport.apply()
        val batch = game.batch
        val font = game.font
        val w = viewport.worldWidth
        val h = viewport.worldHeight
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "SCORE ${snap.score}", 12f, h - 12f)

        if (snap.dead) {
            val midY = h / 2f
            font.color = Color.CORAL
            centered("GAME OVER", midY + 80f, w)
            font.color = Color.WHITE
            centered("SCORE ${snap.score}", midY + 20f, w)
            centered("HIGH ${snap.highScore}", midY - 20f, w)
            centered("press space", midY - 90f, w)
        }
        batch.end()
    }

    private fun centered(text: String, y: Float, w: Float) {
        layout.setText(game.font, text)
        game.font.draw(game.batch, text, w / 2f - layout.width / 2f, y)
    }
}
