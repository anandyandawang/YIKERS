package com.yikers.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.yikers.YikersGame
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.control.AugmentPicker
import com.yikers.control.HumanAgent
import com.yikers.net.AugmentId
import com.yikers.net.NetworkGameSession
import com.yikers.net.Participant
import com.yikers.net.PlayerSnap
import com.yikers.net.Session
import com.yikers.net.WorldSnapshot
import com.yikers.render.SnapshotRenderer
import com.yikers.ui.UiColors
import com.yikers.ui.UiText
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
    private val ui = UiText(game.font, game.batch)

    private val renderer = SnapshotRenderer(game.shape, camera)

    private var human: Participant? = null
    private var picker: AugmentPicker? = null

    private var persisted = false

    // Augment-offer overlay. Pick stage lists the server's options; swap stage
    // (loadout full) lists owned augments to drop. The run continues behind it.
    private val touch = Vector2()
    private val optionRects = List(GameConfig.AUGMENT_OFFER_CHOICES) { Rectangle() }
    private val ownedRects = List(GameConfig.MAX_AUGMENTS) { Rectangle() }
    private val skipBtn = Rectangle()
    private var swapPick: AugmentId? = null   // picked while full -> now choosing the drop
    private var titleY = 0f

    override fun show() {
        teardown()
        persisted = false
        swapPick = null

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
        val agent = AugmentPicker(HumanAgent(session.config.runConfig.horizontalSpeed))
        picker = agent
        human = Participant(session, agent)
    }

    override fun render(delta: Float) {
        val human = human ?: return
        ScreenUtils.clear(UiColors.BG)
        viewport.apply()

        human.pump(delta)                 // decide + submit input
        val snap = human.session.snapshot()

        // Center on the kill-line using OUR local view height (never sent).
        renderer.render(snap, viewport.worldHeight)
        val me = snap.entities.filterIsInstance<PlayerSnap>()
            .firstOrNull { it.slot == human.session.slot }
        drawHud(snap, me)

        if (!snap.dead && me != null && me.offer.isNotEmpty()) {
            handleOfferInput(me)
            drawOffer(me)
        } else {
            swapPick = null
        }

        if (snap.dead) handleGameOver(delta, snap)
    }

    private fun drawHud(snap: WorldSnapshot, me: PlayerSnap?) {
        hudViewport.apply() // world fills the glViewport; HUD needs its own
        val batch = game.batch
        val font = game.font
        val w = hudViewport.worldWidth
        val h = hudViewport.worldHeight
        batch.projectionMatrix = hudCamera.combined
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "SCORE ${snap.score}", 12f, h - 12f)

        // Loadout, oldest first, under the score.
        if (me != null) {
            font.color = Color.LIGHT_GRAY
            me.augments.forEachIndexed { i, id ->
                font.draw(batch, id.displayName, 12f, h - 40f - i * 20f)
            }
        }

        if (snap.dead) {
            val midY = h / 2f
            font.color = Color.CORAL
            ui.centered("GAME OVER", midY + 80f, w)
            font.color = Color.WHITE
            ui.centered("SCORE ${snap.score}", midY + 20f, w)
            ui.centered("HIGH ${snap.highScore}", midY - 20f, w)
            ui.centered("press space", midY - 90f, w)
        }
        batch.end()
    }

    private fun drawOffer(me: PlayerSnap) {
        val w = hudViewport.worldWidth
        val h = hudViewport.worldHeight
        val swapping = swapPick != null
        val rects = if (swapping) ownedRects else optionRects
        val ids = if (swapping) me.augments else me.offer
        layoutOffer(w, h, rects, ids.size)

        val shape = game.shape
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shape.projectionMatrix = hudCamera.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.setColor(0f, 0f, 0f, 0.55f)
        shape.rect(0f, 0f, w, h)
        shape.color = UiColors.BUTTON
        ids.indices.forEach { i -> rects[i].let { shape.rect(it.x, it.y, it.width, it.height) } }
        shape.color = UiColors.ROW
        shape.rect(skipBtn.x, skipBtn.y, skipBtn.width, skipBtn.height)
        shape.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        val batch = game.batch
        val font = game.font
        batch.projectionMatrix = hudCamera.combined
        batch.begin()
        font.color = Color.CORAL
        ui.centered(if (swapping) "FULL — SWAP OUT WHICH?" else "CHOOSE AN AUGMENT", titleY, w)
        ids.forEachIndexed { i, id ->
            val rect = rects[i]
            font.color = Color.WHITE
            font.draw(batch, "${i + 1}. ${id.displayName}", rect.x + 14f, rect.y + rect.height - 12f)
            font.color = Color.LIGHT_GRAY
            font.draw(batch, id.blurb, rect.x + 14f, rect.y + 26f)
        }
        font.color = Color.WHITE
        ui.inRect(if (swapping) "BACK" else "SKIP", skipBtn)
        batch.end()
    }

    private fun layoutOffer(w: Float, h: Float, rects: List<Rectangle>, rows: Int) {
        val rw = minOf(w * 0.84f, 400f)
        val rh = 64f
        val gap = 12f
        val x = (w - rw) / 2f
        val blockH = rows * rh + (rows - 1) * gap
        titleY = h / 2f + blockH / 2f + 48f
        var y = h / 2f + blockH / 2f - rh
        repeat(rows) { i ->
            rects[i].set(x, y, rw, rh)
            y -= rh + gap
        }
        skipBtn.set(x, y + rh - gap - 48f - 8f, rw, 48f)
    }

    // Keys 1..N pick a row, S skips / ESC backs out of the swap stage; taps hit
    // the same rects. Picking while at MAX_AUGMENTS enters the swap stage.
    private fun handleOfferInput(me: PlayerSnap) {
        val picker = picker ?: return
        val pending = swapPick
        val ids = if (pending != null) me.augments else me.offer
        ids.forEachIndexed { i, id ->
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                rowChosen(me, pending, id)
                return
            }
        }
        if (pending != null && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            swapPick = null
            return
        }
        if (pending == null && Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            picker.skipOffer()
            return
        }
        if (!Gdx.input.justTouched()) return
        touch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        hudViewport.unproject(touch)
        val rects = if (pending != null) ownedRects else optionRects
        ids.forEachIndexed { i, id ->
            if (rects[i].contains(touch)) {
                rowChosen(me, pending, id)
                return
            }
        }
        if (skipBtn.contains(touch)) {
            if (pending != null) swapPick = null else picker.skipOffer()
        }
    }

    private fun rowChosen(me: PlayerSnap, pending: AugmentId?, id: AugmentId) {
        when {
            pending != null -> {                                      // id = augment to drop
                picker?.choose(pending, dropId = id)
                swapPick = null
            }
            me.augments.size >= GameConfig.MAX_AUGMENTS -> swapPick = id
            else -> picker?.choose(id)
        }
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
        picker = null
    }
}
