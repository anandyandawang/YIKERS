package com.yikers.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.yikers.YikersGame
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.control.Roster
import ktx.app.KtxScreen

// Title + high score. Any key/tap starts a run.
class MenuScreen(private val game: YikersGame) : KtxScreen {
    private val viewport = FitViewport(GameConfig.WIDTH, GameConfig.HEIGHT)
    private val layout = GlyphLayout()

    private var elapsed = 0f

    override fun show() {
        elapsed = 0f
    }

    override fun render(delta: Float) {
        elapsed += delta
        val pressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.justTouched()
        if (pressed || (Roster.handsFree && elapsed >= AUTO_START_DELAY)) {
            game.setScreen<PlayScreen>()
            return
        }

        ScreenUtils.clear(0.10f, 0.12f, 0.16f, 1f)
        viewport.apply()
        game.batch.projectionMatrix = viewport.camera.combined
        game.batch.begin()
        game.font.color = Color.CORAL
        centered(GameConfig.TITLE, GameConfig.HEIGHT * 0.66f)
        game.font.color = Color.WHITE
        centered("press space to climb", GameConfig.HEIGHT * 0.50f)
        centered("HIGH ${Prefs.highScore}", GameConfig.HEIGHT * 0.40f)
        game.batch.end()
    }

    private fun centered(text: String, y: Float) {
        layout.setText(game.font, text)
        game.font.draw(game.batch, text, (GameConfig.WIDTH - layout.width) / 2f, y)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    companion object {
        private const val AUTO_START_DELAY = 0.6f
    }
}
