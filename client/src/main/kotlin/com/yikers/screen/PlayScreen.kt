package com.yikers.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.yikers.YikersGame
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.control.HumanAgent
import com.yikers.net.GameHost
import com.yikers.net.NetworkGameSession
import com.yikers.net.NetworkHost
import com.yikers.net.Participant
import com.yikers.net.RoomId
import com.yikers.net.Session
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.render.SnapshotRenderer
import com.yikers.screen.hud.AugmentOfferOverlay
import com.yikers.screen.hud.Hud
import ktx.app.KtxScreen

// Owns one run client-side: open a room, join, then each frame pump -> render the
// snapshot. HUD + augment offer live in their own components (see screen/hud).
class PlayScreen(private val game: YikersGame) : KtxScreen {
    private val camera = OrthographicCamera()
    // ExtendViewport: pin WIDTH full-width, extend HEIGHT on taller phones.
    private val viewport = ExtendViewport(GameConfig.WIDTH, GameConfig.HEIGHT, camera)

    // HUD in its own pixel space (world cam is meters).
    private val hudCamera = OrthographicCamera()
    private val hudViewport = ExtendViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX, hudCamera)

    private val renderer = SnapshotRenderer(game.shape, camera)
    private val hud = Hud(game, hudViewport)
    private val augmentOverlay = AugmentOfferOverlay(game, hudViewport)

    private var host: GameHost? = null
    private var room: RoomId? = null
    private var human: Participant? = null
    private var speed = 0f

    private var persisted = false

    override fun show() {
        teardown()
        persisted = false

        camera.position.set(GameConfig.WIDTH / 2f, GameConfig.HEIGHT / 2f, 0f)
        camera.update()
        hudCamera.position.set(GameConfig.WIDTH_PX / 2f, GameConfig.HEIGHT_PX / 2f, 0f)
        hudCamera.update()

        // Network-only now. Server owns config; open() sentinel, config via Welcome.
        val h: GameHost = NetworkHost(Session.host, Session.port)
        try {
            val r = h.open(SessionConfig())
            room = r
            host = h
            val s = h.join(r)
            speed = (s as? NetworkGameSession)?.config?.runConfig?.horizontalSpeed
                ?: SessionConfig().runConfig.horizontalSpeed
            human = Participant(s, HumanAgent(speed))
        } catch (e: Exception) {
            Gdx.app.error("YIKERS", "join failed", e)
            game.setScreen<LobbyScreen>()
            return
        }
    }

    override fun render(delta: Float) {
        val human = human ?: return
        ScreenUtils.clear(0.10f, 0.12f, 0.16f, 1f)
        viewport.apply()

        human.pump(delta)                 // decide + submit input
        val snap = human.session.snapshot()

        // Center on the kill-line using OUR local view height (never sent).
        renderer.render(snap, viewport.worldHeight)
        hud.render(snap)

        val overlayUp = augmentOverlay.render(
            human.session.augmentOffer(),
            human.session.awaitingAugmentResume(),
        ) { human.session.submitAugmentPick(it) }
        if (!overlayUp && snap.dead) handleGameOver(snap)
    }

    // Persist the high score once (client owns Prefs), then wait for a key/tap.
    private fun handleGameOver(snap: WorldSnapshot) {
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
        host?.let { h -> room?.let { h.close(it) } }
        host = null
        room = null
        human = null
    }
}
