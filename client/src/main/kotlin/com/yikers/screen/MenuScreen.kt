package com.yikers.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.yikers.YikersGame
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.control.Roster
import com.yikers.net.Session
import ktx.app.KtxScreen

// Title + high score + two mode buttons. Single Player runs the embedded local sim
// (unchanged classic play); Multiplayer opens the LAN lobby. Drawn the asset-free way
// (ShapeRenderer rects + BitmapFont), hit-tested via unprojected touch. Hands-free
// (0 humans) still auto-starts a local run after a beat, so attract/recording is kept.
class MenuScreen(private val game: YikersGame) : KtxScreen {
    private val viewport = ExtendViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX)
    private val layout = GlyphLayout()
    private val touch = Vector2()

    private val singleBtn = Rectangle()
    private val multiBtn = Rectangle()

    private var elapsed = 0f

    override fun show() {
        elapsed = 0f
        // Returning here from a network run resets the routing so the next click of
        // Single Player (or a hands-free auto-start) takes the local path.
        Session.local()
    }

    override fun render(delta: Float) {
        elapsed += delta
        viewport.apply()
        val w = viewport.worldWidth
        val h = viewport.worldHeight
        layoutButtons(w, h)

        // Hands-free attract mode: auto-start a local run, no input needed.
        if (Roster.handsFree && elapsed >= AUTO_START_DELAY) {
            startSinglePlayer()
            return
        }
        handleInput()

        ScreenUtils.clear(0.10f, 0.12f, 0.16f, 1f)

        val shape = game.shape
        shape.projectionMatrix = viewport.camera.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = BUTTON_FILL
        shape.rect(singleBtn.x, singleBtn.y, singleBtn.width, singleBtn.height)
        shape.rect(multiBtn.x, multiBtn.y, multiBtn.width, multiBtn.height)
        shape.end()

        val batch = game.batch
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        game.font.color = Color.CORAL
        centered(GameConfig.TITLE, h * 0.74f, w)
        game.font.color = Color.WHITE
        centered("HIGH ${Prefs.highScore}", h * 0.64f, w)
        labelIn("SINGLE PLAYER", singleBtn)
        labelIn("MULTIPLAYER", multiBtn)
        batch.end()
    }

    private fun layoutButtons(w: Float, h: Float) {
        val bw = minOf(w * 0.7f, 320f)
        val bh = 64f
        val x = (w - bw) / 2f
        singleBtn.set(x, h * 0.46f, bw, bh)
        multiBtn.set(x, h * 0.46f - bh - 24f, bw, bh)
    }

    private fun handleInput() {
        // Keyboard keeps the classic one-key start = Single Player.
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            startSinglePlayer()
            return
        }
        if (!Gdx.input.justTouched()) return
        touch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        viewport.unproject(touch)
        when {
            singleBtn.contains(touch) -> startSinglePlayer()
            multiBtn.contains(touch) -> game.setScreen<LobbyScreen>()
        }
    }

    private fun startSinglePlayer() {
        Session.local()
        game.setScreen<PlayScreen>()
    }

    private fun centered(text: String, y: Float, w: Float) {
        layout.setText(game.font, text)
        game.font.draw(game.batch, text, (w - layout.width) / 2f, y)
    }

    private fun labelIn(text: String, rect: Rectangle) {
        layout.setText(game.font, text)
        game.font.draw(
            game.batch, text,
            rect.x + (rect.width - layout.width) / 2f,
            rect.y + (rect.height + layout.height) / 2f,
        )
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    companion object {
        private const val AUTO_START_DELAY = 0.6f
        private val BUTTON_FILL = Color(0.18f, 0.22f, 0.30f, 1f)
    }
}
