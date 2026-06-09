package com.yikers.screen.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.yikers.YikersGame
import com.yikers.net.AugmentOfferSnap
import com.yikers.net.WorldSnapshot
import com.yikers.net.wire.AugmentPick

// Self-contained modal augment picker. Owns its own swap state + input. Choices mode:
// tap a row (or 1..n) to take it, SKIP to pass. At the cap, taking a choice flips to
// swap mode: tap an owned augment to drop, CANCEL to back out. Drawn in pixel/HUD space.
class AugmentOfferOverlay(
    private val game: YikersGame,
    private val viewport: ExtendViewport,
) {
    private val layout = GlyphLayout()
    private val touch = Vector2()
    private var swapChoiceId: String? = null

    // Draw + handle THIS client's offer. Returns true while any overlay is up (offer
    // or "waiting for others"), so the screen suppresses its other input.
    fun render(snap: WorldSnapshot, slot: Int, submit: (AugmentPick) -> Unit): Boolean {
        val offer = snap.augmentOffers.firstOrNull { it.slot == slot }
        if (offer == null) {
            swapChoiceId = null
            if (snap.augmentOffers.isEmpty()) return false
            drawWaiting()           // others still picking; room stays frozen
            return true
        }
        drawOffer(offer, submit)
        return true
    }

    private fun drawOffer(offer: AugmentOfferSnap, submit: (AugmentPick) -> Unit) {
        viewport.apply()
        val w = viewport.worldWidth
        val h = viewport.worldHeight
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
        shape.projectionMatrix = viewport.camera.combined
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
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        game.font.color = Color.CORAL
        val title = if (swapping) "AUGMENTS FULL - SWAP ONE OUT" else "CHOOSE AN AUGMENT"
        centered(title, h / 2f + totalH / 2f + 28f, w)
        game.font.color = Color.WHITE
        rects.forEachIndexed { i, (r, id) ->
            val text = if (id == null) (if (swapping) "CANCEL" else "SKIP")
            else "${i + 1}. ${labels[i]}"
            labelInRect(text, r)
        }
        batch.end()

        handleInput(offer, rects, swapping, submit)
    }

    private fun drawWaiting() {
        viewport.apply()
        val batch = game.batch
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        game.font.color = Color.LIGHT_GRAY
        centered("waiting for others...", viewport.worldHeight / 2f, viewport.worldWidth)
        batch.end()
    }

    private fun handleInput(
        offer: AugmentOfferSnap,
        rects: List<Pair<Rectangle, String?>>,
        swapping: Boolean,
        submit: (AugmentPick) -> Unit,
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
            viewport.unproject(touch)
            rects.firstOrNull { it.first.contains(touch) } ?: return
        }

        val id = hit.second
        if (id == null) {                          // skip / cancel
            if (swapping) swapChoiceId = null else submit(AugmentPick())
            return
        }
        if (swapping) {                            // id = owned augment to drop
            submit(AugmentPick(augmentId = swapChoiceId, swapOutId = id))
            swapChoiceId = null
        } else if (offer.owned.size >= offer.maxOwned) {
            swapChoiceId = id                      // full: pick what to drop next
        } else {
            submit(AugmentPick(augmentId = id))
        }
    }

    private fun centered(text: String, y: Float, w: Float) {
        layout.setText(game.font, text)
        game.font.draw(game.batch, text, w / 2f - layout.width / 2f, y)
    }

    private fun labelInRect(text: String, rect: Rectangle) {
        layout.setText(game.font, text)
        game.font.draw(
            game.batch, text,
            rect.x + (rect.width - layout.width) / 2f,
            rect.y + (rect.height + layout.height) / 2f,
        )
    }

    private companion object {
        private val SCRIM = Color(0f, 0f, 0f, 0.72f)
        private val BTN_FILL = Color(0.18f, 0.22f, 0.30f, 1f)
        private val SKIP_FILL = Color(0.30f, 0.18f, 0.18f, 1f)
    }
}
