package com.yikers.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.yikers.YikersGame
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.control.HumanAgent
import com.yikers.net.NetworkGameSession
import com.yikers.net.Participant
import com.yikers.net.Session
import com.yikers.net.WorldSnapshot
import com.yikers.render.SnapshotRenderer
import ktx.app.KtxScreen

// Owns one run client-side: join the server, then each frame pump input ->
// render the latest snapshot.
class PlayScreen(private val game: YikersGame) : KtxScreen {
    private val camera = OrthographicCamera()
    // ExtendViewport: pin WIDTH full-width, extend HEIGHT on taller phones.
    private val viewport = ExtendViewport(GameConfig.WIDTH, GameConfig.HEIGHT, camera)

    // HUD in its own pixel space (world cam is meters).
    private val hudCamera = OrthographicCamera()
    private val hudViewport = ExtendViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX, hudCamera)
    private val layout = GlyphLayout()

    private val renderer = SnapshotRenderer(game.shape, camera)

    private var human: Participant? = null

    private var persisted = false

    override fun show() {
        teardown()
        persisted = false

        camera.position.set(GameConfig.WIDTH / 2f, GameConfig.HEIGHT / 2f, 0f)
        camera.update()
        hudCamera.position.set(GameConfig.WIDTH_PX / 2f, GameConfig.HEIGHT_PX / 2f, 0f)
        hudCamera.update()

        // Server owns the run config; the Welcome handshake hands it over.
        val session = try {
            NetworkGameSession.connect(Session.host, Session.port)
        } catch (e: Exception) {
            Gdx.app.error("YIKERS", "join failed", e)
            game.setScreen<LobbyScreen>()
            return
        }
        human = Participant(session, HumanAgent(session.config.runConfig.horizontalSpeed))
    }

    override fun render(delta: Float) {
        val human = human ?: return
        ScreenUtils.clear(0.10f, 0.12f, 0.16f, 1f)
        viewport.apply()

        human.pump(delta)                 // decide + submit input
        val snap = human.session.snapshot()

        // Center on the kill-line using OUR local view height (never sent).
        renderer.render(snap, viewport.worldHeight)
        drawHud(snap)

        if (snap.dead) handleGameOver(delta, snap)
    }

    private fun drawHud(snap: WorldSnapshot) {
        hudViewport.apply() // world fills the glViewport; HUD needs its own
        val batch = game.batch
        val font = game.font
        val w = hudViewport.worldWidth
        val h = hudViewport.worldHeight
        batch.projectionMatrix = hudCamera.combined
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "SCORE ${snap.score}", 12f, h - 12f)

        if (snap.dead) {
            val midY = h / 2f
            font.color = Color.CORAL
            centeredHud("GAME OVER", midY + 80f, w)
            font.color = Color.WHITE
            centeredHud("SCORE ${snap.score}", midY + 20f, w)
            centeredHud("HIGH ${snap.highScore}", midY - 20f, w)
            centeredHud("press space", midY - 90f, w)
        }
        batch.end()
    }

    private fun centeredHud(text: String, y: Float, w: Float) {
        layout.setText(game.font, text)
        game.font.draw(game.batch, text, w / 2f - layout.width / 2f, y)
    }

    // Persist the high score once (client owns Prefs), then wait for a key/tap.
    private fun handleGameOver(delta: Float, snap: WorldSnapshot) {
        if (!persisted) {
            if (snap.highScore > Prefs.highScore) Prefs.highScore = snap.highScore
            persisted = true
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.justTouched()) {
            game.setScreen<MenuScreen>()
        }
    }

    override fun resize(width: Int, height: Int) {
        // PlayScreen drives camera.y for scroll — don't recenter the world cam.
        viewport.update(width, height, false)
        hudViewport.update(width, height, true)
    }

    override fun hide() = teardown()

    override fun dispose() = teardown()

    private fun teardown() {
        human?.close()
        human = null
    }
}
