package com.yikers.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
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
import com.yikers.net.AugmentOfferSnap
import com.yikers.net.RoomId
import com.yikers.net.Session
import com.yikers.net.SessionConfig
import com.yikers.net.WorldSnapshot
import com.yikers.net.wire.AugmentPick
import com.yikers.render.SnapshotRenderer
import ktx.app.KtxScreen

// Owns one run client-side: open a room, join, then each frame pump -> step ->
// render the snapshot.
class PlayScreen(private val game: YikersGame) : KtxScreen {
    private val camera = OrthographicCamera()
    // ExtendViewport: pin WIDTH full-width, extend HEIGHT on taller phones.
    private val viewport = ExtendViewport(GameConfig.WIDTH, GameConfig.HEIGHT, camera)

    // HUD in its own pixel space (world cam is meters).
    private val hudCamera = OrthographicCamera()
    private val hudViewport = ExtendViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX, hudCamera)
    private val layout = GlyphLayout()

    private val renderer = SnapshotRenderer(game.shape, camera)

    private var host: GameHost? = null
    private var room: RoomId? = null
    private var human: Participant? = null
    private var speed = 0f

    private var persisted = false

    private val touch = Vector2()
    // Non-null while the player, already at the cap, is choosing which augment to drop.
    private var swapChoiceId: String? = null

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
        drawHud(snap)

        val offer = snap.augmentOffers.firstOrNull { it.slot == human.session.slot }
        if (offer == null) swapChoiceId = null
        when {
            offer != null -> drawAugmentOffer(offer)
            snap.augmentOffers.isNotEmpty() -> drawWaiting()  // others still picking
            snap.dead -> handleGameOver(delta, snap)
        }
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

    // Modal augment picker. Choices mode: tap a choice (or 1..3) to take it, SKIP to
    // pass. If already at the cap, taking a choice flips to swap mode: tap an owned
    // augment (or 1..n) to drop, CANCEL to back out.
    private fun drawAugmentOffer(offer: AugmentOfferSnap) {
        hudViewport.apply()
        val w = hudViewport.worldWidth
        val h = hudViewport.worldHeight
        val swapping = swapChoiceId != null

        val items = if (swapping) offer.owned else offer.choices
        val labels = items.map { "${it.name} - ${it.desc}" }

        val bw = minOf(w * 0.85f, 560f)
        val bh = 54f
        val gap = 14f
        val x = (w - bw) / 2f
        val rows = labels.size + 1                       // + skip/cancel row
        val totalH = rows * bh + (rows - 1) * gap
        var y = h / 2f + totalH / 2f - bh

        val rects = ArrayList<Pair<Rectangle, String?>>(rows)
        val shape = game.shape
        shape.projectionMatrix = hudCamera.combined
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = SCRIM
        shape.rect(0f, 0f, w, h)
        shape.color = BTN_FILL
        for (a in items) {
            val r = Rectangle(x, y, bw, bh)
            shape.rect(r.x, r.y, r.width, r.height)
            rects.add(r to a.id)
            y -= bh + gap
        }
        val tail = Rectangle(x, y, bw, bh)
        shape.color = SKIP_FILL
        shape.rect(tail.x, tail.y, tail.width, tail.height)
        rects.add(tail to null)
        shape.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        val batch = game.batch
        batch.projectionMatrix = hudCamera.combined
        batch.begin()
        game.font.color = Color.CORAL
        val title = if (swapping) "AUGMENTS FULL — SWAP ONE OUT" else "CHOOSE AN AUGMENT"
        centeredHud(title, h / 2f + totalH / 2f + 28f, w)
        game.font.color = Color.WHITE
        rects.forEachIndexed { i, (r, id) ->
            val text = if (id == null) (if (swapping) "CANCEL" else "SKIP")
            else "${i + 1}. ${labels[i]}"
            labelInRect(text, r)
        }
        batch.end()

        handleAugmentInput(offer, rects, swapping)
    }

    private fun drawWaiting() {
        hudViewport.apply()
        val batch = game.batch
        batch.projectionMatrix = hudCamera.combined
        batch.begin()
        game.font.color = Color.LIGHT_GRAY
        centeredHud("waiting for others...", hudViewport.worldHeight / 2f, hudViewport.worldWidth)
        batch.end()
    }

    private fun handleAugmentInput(
        offer: AugmentOfferSnap,
        rects: List<Pair<Rectangle, String?>>,
        swapping: Boolean,
    ) {
        // Number keys 1..n mirror the on-screen rows; S = skip/cancel (the tail row).
        val keyed: Pair<Rectangle, String?>? = when {
            Gdx.input.isKeyJustPressed(Input.Keys.S) -> rects.last()
            else -> (0 until rects.size - 1).firstOrNull {
                Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + it)
            }?.let { rects[it] }
        }
        val hit = keyed ?: run {
            if (!Gdx.input.justTouched()) return
            touch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
            hudViewport.unproject(touch)
            rects.firstOrNull { it.first.contains(touch) } ?: return
        }

        val session = human?.session ?: return
        val id = hit.second
        if (id == null) {                          // skip / cancel
            if (swapping) swapChoiceId = null else session.submitAugmentPick(AugmentPick())
            return
        }
        if (swapping) {                            // id = owned augment to drop
            session.submitAugmentPick(AugmentPick(augmentId = swapChoiceId, swapOutId = id))
            swapChoiceId = null
        } else if (offer.owned.size >= offer.maxOwned) {
            swapChoiceId = id                      // full: pick what to drop next
        } else {
            session.submitAugmentPick(AugmentPick(augmentId = id))
        }
    }

    private fun labelInRect(text: String, rect: Rectangle) {
        layout.setText(game.font, text)
        game.font.draw(
            game.batch, text,
            rect.x + (rect.width - layout.width) / 2f,
            rect.y + (rect.height + layout.height) / 2f,
        )
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

    private companion object {
        private val SCRIM = Color(0f, 0f, 0f, 0.72f)
        private val BTN_FILL = Color(0.18f, 0.22f, 0.30f, 1f)
        private val SKIP_FILL = Color(0.30f, 0.18f, 0.18f, 1f)
    }
}
