package com.yikers.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.yikers.YikersGame
import com.yikers.config.BootConfig
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.net.DedicatedServer
import com.yikers.net.Session
import com.yikers.net.SessionConfig
import com.yikers.ui.UiColors
import com.yikers.ui.UiText
import ktx.app.KtxScreen

class MenuScreen(private val game: YikersGame) : KtxScreen {
    private val viewport = ExtendViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX)
    private val ui = UiText(game.font, game.batch)
    private val touch = Vector2()

    private val singleBtn = Rectangle()
    private val multiBtn = Rectangle()

    override fun show() {
        // Menu return stops a solo server; an MP host survives.
        Session.hostedServer?.let { if (!it.discoverable) Session.shutdownHost() }
    }

    override fun render(delta: Float) {
        viewport.apply()
        val w = viewport.worldWidth
        val h = viewport.worldHeight
        layoutButtons(w, h)

        handleInput()

        ScreenUtils.clear(UiColors.BG)

        val shape = game.shape
        shape.projectionMatrix = viewport.camera.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = UiColors.BUTTON
        shape.rect(singleBtn.x, singleBtn.y, singleBtn.width, singleBtn.height)
        shape.rect(multiBtn.x, multiBtn.y, multiBtn.width, multiBtn.height)
        shape.end()

        val batch = game.batch
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        game.font.color = Color.CORAL
        ui.centered(GameConfig.TITLE, h * 0.74f, w)
        game.font.color = Color.WHITE
        ui.centered("HIGH ${Prefs.highScore}", h * 0.64f, w)
        ui.inRect("SINGLE PLAYER", singleBtn)
        ui.inRect("MULTIPLAYER", multiBtn)
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
        // Solo = boot a private server + join it over loopback.
        val cfg = SessionConfig(seed = BootConfig.seed, previousHighScore = Prefs.highScore)
        val server = runCatching { DedicatedServer("solo", 0, cfg, discoverable = false) }
            .getOrElse {
                Gdx.app.error("YIKERS", "solo server boot failed", it)
                return
            }
        Session.hostAndJoin(server)
        game.setScreen<PlayScreen>()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }
}
