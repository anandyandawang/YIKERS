package com.yikers.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.yikers.YikersGame
import com.yikers.bot.BotAgent
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.control.BootConfig
import com.yikers.control.HumanAgent
import com.yikers.net.GameHost
import com.yikers.net.GameSession
import com.yikers.net.LocalHost
import com.yikers.net.NetworkGameSession
import com.yikers.net.NetworkHost
import com.yikers.net.Participant
import com.yikers.net.RoomId
import com.yikers.net.Session
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.render.SnapshotRenderer
import ktx.app.KtxScreen

// Owns one run from the CLIENT side: opens a room on a host, joins one client per
// participant, then each frame captures input -> submits it -> steps the run ->
// renders the returned snapshot. A bot is just another client (BotClient) joined to
// the same room: locally we launch humans + bots side by side; over the network we
// are a single human client and bots (if any) live on the server. No Fleks/Box2D
// here — the sim lives behind the GameSession seam.
class PlayScreen(private val game: YikersGame) : KtxScreen {
    private val camera = OrthographicCamera()
    // ExtendViewport: pin world WIDTH to full screen width (no side bars) and let
    // HEIGHT extend on taller phones, so the bottom sits on the kill-line and
    // higher-aspect screens see more world above.
    private val viewport = ExtendViewport(GameConfig.WIDTH, GameConfig.HEIGHT, camera)

    // HUD draws in its own pixel space — the world cam is meters, so the font would
    // render ~100x too big through it. Extends with the screen like the world.
    private val hudCamera = OrthographicCamera()
    private val hudViewport = ExtendViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX, hudCamera)
    private val layout = GlyphLayout()

    private val renderer = SnapshotRenderer(game.shape, camera)

    private var host: GameHost? = null
    private var room: RoomId? = null
    // Every joined client as a Participant (session + agent) — humans and bots are
    // the same type here, differing only by agent. The first one's session owns the
    // clock (drives step()); the rest just submit input.
    private var participants: List<Participant> = emptyList()
    private var clock: GameSession? = null
    private var speed = 0f

    private var persisted = false

    override fun show() {
        teardown()
        persisted = false

        camera.position.set(GameConfig.WIDTH / 2f, GameConfig.HEIGHT / 2f, 0f)
        camera.update()
        hudCamera.position.set(GameConfig.WIDTH_PX / 2f, GameConfig.HEIGHT_PX / 2f, 0f)
        hudCamera.update()

        val cfg = SessionConfig(
            seed = BootConfig.seed,
            previousHighScore = Prefs.highScore,
        )

        // Same seam, two hosts: LocalHost embeds the sim (singleplayer + local bots),
        // NetworkHost connects to a LAN server. For network the server already opened
        // the room, so open() returns a sentinel and join() does the handshake.
        val h: GameHost = if (Session.mode == Session.Mode.NETWORK) {
            NetworkHost(Session.host, Session.port)
        } else {
            LocalHost()
        }
        try {
            val r = h.open(cfg)
            room = r
            host = h
            if (Session.mode == Session.Mode.NETWORK) {
                joinNetwork(h, r, cfg)
            } else {
                joinLocal(h, r, cfg)
            }
        } catch (e: Exception) {
            // Server unreachable / refused -> drop back to the lobby instead of crashing.
            Gdx.app.error("YIKERS", "join failed", e)
            game.setScreen<LobbyScreen>()
            return
        }
    }

    // One human client; the server owns the clock. Read feel from the Welcome config
    // and drive only our own slot. (Bots against a LAN server connect separately as
    // their own socket clients — never spawned from here.)
    private fun joinNetwork(h: GameHost, r: RoomId, cfg: SessionConfig) {
        val s = h.join(r)
        speed = (s as? NetworkGameSession)?.config?.runConfig?.horizontalSpeed
            ?: cfg.runConfig.horizontalSpeed
        clock = s
        participants = listOf(Participant(s, HumanAgent(speed)))
    }

    // Local run: exactly one human client, plus BootConfig.bots in-process bot
    // clients on the same embedded room. The bots are pumped right here in the render
    // loop (~1-tick lag), but the embedded server never knows they're bots — each is
    // just a join() + a Participant whose agent happens to be a BotAgent.
    private fun joinLocal(h: GameHost, r: RoomId, cfg: SessionConfig) {
        speed = cfg.runConfig.horizontalSpeed
        val human = Participant(h.join(r), HumanAgent(speed))
        val bots = List(BootConfig.bots) { Participant(h.join(r), BotAgent(cfg.runConfig)) }
        participants = listOf(human) + bots
        clock = participants.first().session
    }

    override fun render(delta: Float) {
        val clock = clock ?: return
        ScreenUtils.clear(0.10f, 0.12f, 0.16f, 1f)
        viewport.apply()

        // Every client (human or bot) decides + submits this frame the same way, then
        // the clock owner advances the run.
        participants.forEach { it.pump(delta) }
        clock.step(delta)
        val snap = clock.snapshot()

        // Center on the kill-line using OUR local view height — never sent to the
        // server, so two clients on different aspects each frame their own view.
        renderer.render(snap, viewport.worldHeight)
        drawHud(snap)

        if (snap.dead) handleGameOver(delta, snap)
    }

    // Run-level HUD overlay: score readout + game-over panel, drawn in pixel space
    // after the world. UI isn't entity data, so it lives on the screen and reads
    // straight from the snapshot.
    private fun drawHud(snap: WorldSnapshot) {
        hudViewport.apply() // world is ExtendViewport (full-screen glViewport); HUD needs its own
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

    // On death: persist the high score once (server tracks the in-run max; the
    // client owns the saved Prefs), then wait for a key/tap to return to the menu.
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
        // PlayScreen drives camera.y for scrolling — don't recenter the world cam.
        viewport.update(width, height, false)
        hudViewport.update(width, height, true)
    }

    override fun hide() = teardown()

    override fun dispose() = teardown()

    private fun teardown() {
        // Close every client first: for a network client this shuts the socket +
        // reader thread; for local clients it drops their player from the room. Then
        // drop the room on the host (disposes the embedded sim).
        participants.forEach { it.close() }
        host?.let { h -> room?.let { h.close(it) } }
        host = null
        room = null
        participants = emptyList()
        clock = null
    }
}
