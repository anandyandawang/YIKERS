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
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.yikers.YikersGame
import com.yikers.control.HumanController
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.DraftOffer
import com.yikers.ecs.component.RenderShape
import com.yikers.ecs.component.augment.Augment
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.component.augment.label

// The augment draft's screen half: draws the open offer over the frozen run and
// applies the local player's pick. Pulled out of PlayScreen -- the draft is its own
// render + input + apply concern. State stays in the ECS: per-climber offers are
// DraftOffer components, so this just finds the human whose turn it is and acts on
// it. One shared keyboard, so humans resolve in order (first pending); true
// simultaneous multi-human picking is a later step. Number-key / tap input; cards
// tinted with the climber's color so a human knows the offer is theirs.
class DraftOverlay(
    private val world: World,
    private val viewport: ExtendViewport,
    private val camera: OrthographicCamera,
    private val game: YikersGame,
) {
    private val offers = world.family { all(DraftOffer) }
    private val layout = GlyphLayout()

    // Any human still has an open offer -> the overlay is up (and the run frozen).
    val active: Boolean get() = activeHuman() != null

    // Whose offer the shared input resolves now: the first pending human. Computed,
    // not stored -- the DraftOffer components are the source of truth.
    private fun activeHuman(): Entity? = with(world) {
        var found: Entity? = null
        offers.forEach { e -> if (found == null && e[Controlled].controller is HumanController) found = e }
        found
    }

    fun render() {
        val e = activeHuman() ?: return
        draw(e)
        handleInput(e)
    }

    private fun draw(e: Entity) {
        viewport.apply()
        val w = viewport.worldWidth
        val h = viewport.worldHeight
        val offer = with(world) { e[DraftOffer] }
        val owned = with(world) { e[Augments].owned }
        val accent = with(world) { e[RenderShape].color }
        val swap = offer.pendingPick != null
        val items: List<Augment> = if (swap) owned.toList() else offer.options
        val cards = optionRects(items.size, w, h)
        val skip = skipRect(w, h)

        val shape = game.shape
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shape.projectionMatrix = camera.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.setColor(0f, 0f, 0f, 0.6f)
        shape.rect(0f, 0f, w, h)
        shape.setColor(accent.r * 0.4f, accent.g * 0.4f, accent.b * 0.4f, 0.95f)
        cards.forEach { shape.rect(it.x, it.y, it.width, it.height) }
        shape.setColor(0.30f, 0.16f, 0.16f, 0.95f)
        shape.rect(skip.x, skip.y, skip.width, skip.height)
        shape.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        val batch = game.batch
        batch.projectionMatrix = camera.combined
        batch.begin()
        game.font.color = accent
        val title = if (swap) {
            "MAX ${Augments.MAX} - DROP ONE FOR ${offer.pendingPick?.label}"
        } else {
            "CHOOSE AUGMENT"
        }
        centered(title, h * 0.82f, w)
        game.font.color = Color.WHITE
        if (items.isEmpty()) {
            centered("no augments left", h * 0.6f, w)
        } else {
            items.forEachIndexed { i, aug ->
                val r = cards[i]
                centered("${i + 1}    ${aug.label}", r.y + r.height / 2f - 6f, w)
            }
        }
        game.font.color = Color.LIGHT_GRAY
        centered(if (swap) "ESC  cancel" else "S  skip", skip.y + skip.height / 2f - 6f, w)
        batch.end()
    }

    // Number key or tapped card picks an option (a swap target in the swap step);
    // S / ESC / the skip band declines or cancels. Edge-triggered: one press
    // resolves one choice. Resolving removes the DraftOffer, so the next human (or
    // the resumed run) follows on the next frame.
    private fun handleInput(e: Entity) {
        val offer = with(world) { e[DraftOffer] }
        if (offer.pendingPick != null) {
            val n = with(world) { e[Augments].owned.size }
            keyIndex(n)?.let { swapOption(e, it); return }
            tappedIndex(n)?.let { swapOption(e, it); return }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || skipTapped()) cancelSwap(e)
        } else {
            val n = offer.options.size
            keyIndex(n)?.let { pickOption(e, it); return }
            tappedIndex(n)?.let { pickOption(e, it); return }
            if (Gdx.input.isKeyJustPressed(Input.Keys.S) || skipTapped()) skipOffer(e)
        }
    }

    // Take option i: add it if under the cap and close the offer; at the cap, stash
    // it as the pending pick and switch to the swap step.
    private fun pickOption(e: Entity, i: Int) = with(world) {
        val offer = e[DraftOffer]
        val choice = offer.options.getOrNull(i) ?: return@with
        val owned = e[Augments].owned
        if (owned.size < Augments.MAX) {
            owned += choice
            e.configure { it -= DraftOffer }
        } else {
            offer.pendingPick = choice
        }
    }

    // Swap step: drop owned augment i and add the pending pick, then close.
    private fun swapOption(e: Entity, i: Int) = with(world) {
        val offer = e[DraftOffer]
        val incoming = offer.pendingPick ?: return@with
        val owned = e[Augments].owned
        val drop = owned.toList().getOrNull(i) ?: return@with
        owned -= drop
        owned += incoming
        e.configure { it -= DraftOffer }
    }

    private fun skipOffer(e: Entity) = with(world) { e.configure { it -= DraftOffer } }

    private fun cancelSwap(e: Entity) = with(world) { e[DraftOffer].pendingPick = null }

    private fun centered(text: String, y: Float, w: Float) {
        layout.setText(game.font, text)
        game.font.draw(game.batch, text, w / 2f - layout.width / 2f, y)
    }

    // Card rectangles in HUD pixel space, shared by draw + tap hit-testing so they
    // never drift. Stacked top-down, centered.
    private fun optionRects(count: Int, w: Float, h: Float): List<Rectangle> {
        val cardW = w * 0.78f
        val cardH = 56f
        val gap = 14f
        val x = (w - cardW) / 2f
        val top = h * 0.66f
        return (0 until count).map { i -> Rectangle(x, top - i * (cardH + gap) - cardH, cardW, cardH) }
    }

    private fun skipRect(w: Float, h: Float): Rectangle {
        val cardW = w * 0.5f
        return Rectangle((w - cardW) / 2f, h * 0.16f, cardW, 48f)
    }

    // First number key (top row or numpad) pressed for 1..count, else null.
    private fun keyIndex(count: Int): Int? {
        for (i in 0 until minOf(count, NUM_KEYS.size)) {
            if (Gdx.input.isKeyJustPressed(NUM_KEYS[i]) || Gdx.input.isKeyJustPressed(NUMPAD_KEYS[i])) return i
        }
        return null
    }

    // Index of the card tapped this frame, else null.
    private fun tappedIndex(count: Int): Int? {
        if (!Gdx.input.justTouched()) return null
        val p = viewport.unproject(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
        optionRects(count, viewport.worldWidth, viewport.worldHeight)
            .forEachIndexed { i, r -> if (r.contains(p.x, p.y)) return i }
        return null
    }

    private fun skipTapped(): Boolean {
        if (!Gdx.input.justTouched()) return false
        val p = viewport.unproject(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
        return skipRect(viewport.worldWidth, viewport.worldHeight).contains(p.x, p.y)
    }

    companion object {
        private val NUM_KEYS = intArrayOf(
            Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5,
        )
        private val NUMPAD_KEYS = intArrayOf(
            Input.Keys.NUMPAD_1, Input.Keys.NUMPAD_2, Input.Keys.NUMPAD_3, Input.Keys.NUMPAD_4, Input.Keys.NUMPAD_5,
        )
    }
}
