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
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import com.yikers.YikersGame
import com.yikers.config.GameConfig
import com.yikers.config.Prefs
import com.yikers.config.RunConfig
import com.yikers.control.BotController
import com.yikers.control.Controller
import com.yikers.control.HumanController
import com.yikers.control.Palette
import com.yikers.control.Roster
import com.yikers.ecs.EntityFactory
import com.yikers.ecs.buildArena
import com.yikers.ecs.component.DraftOffer
import com.yikers.ecs.component.RenderShape
import com.yikers.ecs.component.augment.Augment
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.component.augment.label
import com.yikers.ecs.resource.Draft
import com.yikers.ecs.resource.Refs
import com.yikers.ecs.resource.RunState
import com.yikers.ecs.system.BoulderSystem
import com.yikers.ecs.system.ScrollSystem
import com.yikers.ecs.system.ControlSystem
import com.yikers.ecs.system.DeathSystem
import com.yikers.ecs.system.DraftSystem
import com.yikers.ecs.system.JumpSystem
import com.yikers.ecs.system.MoveSystem
import com.yikers.ecs.system.PhysicsStepSystem
import com.yikers.ecs.system.PlatformSystem
import com.yikers.ecs.system.RenderSystem
import com.yikers.ecs.system.TransformSyncSystem
import com.yikers.ecs.system.WallFollowSystem
import com.yikers.physics.PlayContactListener
import ktx.app.KtxScreen
import ktx.box2d.createWorld
import ktx.math.vec2
import com.badlogic.gdx.physics.box2d.World as PhysicsWorld

// Owns one run: builds Box2D + Fleks worlds on show, tears them down on hide.
class PlayScreen(private val game: YikersGame) : KtxScreen {
    private val camera = OrthographicCamera()
    // ExtendViewport: pin world WIDTH to full screen width (no side bars) and let
    // HEIGHT extend on taller phones, so the bottom sits on the kill-line and
    // higher-aspect screens see more world above.
    private val viewport = ExtendViewport(GameConfig.WIDTH, GameConfig.HEIGHT, camera)

    // HUD draws in its own pixel space — the world cam is meters, so the font
    // would render ~100x too big through it. Extends with the screen like the world.
    private val hudCamera = OrthographicCamera()
    private val hudViewport = ExtendViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX, hudCamera)
    private val layout = GlyphLayout()

    private lateinit var physicsWorld: PhysicsWorld
    private lateinit var world: World
    private lateinit var runState: RunState
    private lateinit var draft: Draft
    private var built = false
    private var deadElapsed = 0f

    override fun show() {
        if (built) teardown()
        newRun()
    }

    private fun newRun() {
        deadElapsed = 0f
        val cfg = RunConfig()
        runState = RunState().apply { highScore = Prefs.highScore }
        draft = Draft()
        val refs = Refs()

        camera.position.set(GameConfig.WIDTH / 2f, GameConfig.HEIGHT / 2f, 0f)
        camera.update()
        hudCamera.position.set(GameConfig.WIDTH_PX / 2f, GameConfig.HEIGHT_PX / 2f, 0f)
        hudCamera.update()

        physicsWorld = createWorld(gravity = vec2(0f, GameConfig.GRAVITY * cfg.gravityScale))
        val arena = buildArena(physicsWorld)

        world = configureWorld {
            injectables {
                add(physicsWorld)
                add(camera)
                add(cfg)
                add(runState)
                add(draft)
                add(arena)
                add(refs)
                add(game.shape)
            }
            systems {
                add(ControlSystem())
                add(MoveSystem())
                add(JumpSystem())
                add(WallFollowSystem())
                add(PhysicsStepSystem())
                add(TransformSyncSystem())
                add(BoulderSystem())
                add(PlatformSystem())
                add(ScrollSystem())
                add(DraftSystem())
                add(DeathSystem())
                add(RenderSystem())
            }
        }

        val factory = EntityFactory(world, physicsWorld, cfg, refs)
        spawnRoster(factory, refs)
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnPlatform(GameConfig.GROUND_HEIGHT + i * GameConfig.PLATFORM_INTERVALS)
        }
        for (i in 1..GameConfig.NUM_PLATFORMS) {
            factory.spawnBoulder(GameConfig.WIDTH / 2f - GameConfig.BOULDER_RADIUS, -3.0f - i * 0.6f)
        }

        physicsWorld.setContactListener(PlayContactListener(world))
        built = true
    }

    // Spawn Roster.humans humans + Roster.bots bots, spread across the floor in
    // distinct colors. First spawned = primary (PlatformSystem scores off it).
    private fun spawnRoster(factory: EntityFactory, refs: Refs) {
        val controllers: List<Controller> =
            List(Roster.humans) { HumanController() } + List(Roster.bots) { BotController() }
        val roster = controllers.ifEmpty {
            listOf(if (Roster.handsFree) BotController() else HumanController())
        }
        val n = roster.size

        val r = GameConfig.BALL_RADIUS
        val minCx = GameConfig.WALL_THICKNESS + r
        val maxCx = GameConfig.WIDTH - GameConfig.WALL_THICKNESS - r
        roster.forEachIndexed { i, controller ->
            val cx = if (n == 1) GameConfig.WIDTH / 2f
            else minCx + (maxCx - minCx) * (i.toFloat() / (n - 1))
            val e = factory.spawnPlayer(
                x = cx - r,
                y = GameConfig.GROUND_HEIGHT,
                controller = controller,
                color = Palette.distinct(i, n),
                group = (-(i + 1)).toShort(),
            )
            if (i == 0) refs.player = e
        }
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0.10f, 0.12f, 0.16f, 1f)
        viewport.apply()
        runState.viewHeight = viewport.worldHeight // device aspect -> visible world height
        world.update(delta)
        drawHud()

        if (draft.isAwaitingHuman) {
            drawDraft()
            handleDraftInput()
        } else if (runState.dead) {
            handleGameOver(delta)
        }
    }

    // Run-level HUD overlay: score readout + game-over panel, drawn in pixel
    // space after the world. Was HudSystem — UI isn't entity data, so it lives on
    // the screen. Inherits the world viewport's glViewport rect (same 0.6 aspect),
    // so no hudViewport.apply() needed; matches the old HudSystem draw exactly.
    private fun drawHud() {
        hudViewport.apply() // world is ExtendViewport (full-screen glViewport); HUD needs its own
        val batch = game.batch
        val font = game.font
        val w = hudViewport.worldWidth
        val h = hudViewport.worldHeight
        batch.projectionMatrix = hudCamera.combined
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "SCORE ${runState.score}", 12f, h - 12f)

        if (runState.dead) {
            val midY = h / 2f
            font.color = Color.CORAL
            centeredHud("GAME OVER", midY + 80f, w)
            font.color = Color.WHITE
            centeredHud("SCORE ${runState.score}", midY + 20f, w)
            centeredHud("HIGH ${runState.highScore}", midY - 20f, w)
            centeredHud("press space", midY - 90f, w)
        }
        batch.end()
    }

    private fun centeredHud(text: String, y: Float, w: Float) {
        layout.setText(game.font, text)
        game.font.draw(game.batch, text, w / 2f - layout.width / 2f, y)
    }

    // Augment draft overlay, drawn over the frozen run in HUD pixel space for the
    // human whose offer is up now (DraftSystem.currentHuman). Up to three cards (the
    // owned list, in the swap step) plus a skip/cancel band, tinted with the
    // climber's own color so each human knows the offer is theirs. Dim pass via
    // ShapeRenderer, labels via the SpriteBatch -- never both open at once.
    private fun drawDraft() {
        val e = draft.currentHuman ?: return
        hudViewport.apply()
        val w = hudViewport.worldWidth
        val h = hudViewport.worldHeight
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
        shape.projectionMatrix = hudCamera.combined
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
        batch.projectionMatrix = hudCamera.combined
        batch.begin()
        game.font.color = accent
        val title = if (swap) {
            "MAX ${Draft.MAX_AUGMENTS} - DROP ONE FOR ${offer.pendingPick?.label}"
        } else {
            "CHOOSE AUGMENT"
        }
        centeredHud(title, h * 0.82f, w)
        game.font.color = Color.WHITE
        if (items.isEmpty()) {
            centeredHud("no augments left", h * 0.6f, w)
        } else {
            items.forEachIndexed { i, aug ->
                val r = cards[i]
                centeredHud("${i + 1}    ${aug.label}", r.y + r.height / 2f - 6f, w)
            }
        }
        game.font.color = Color.LIGHT_GRAY
        centeredHud(if (swap) "ESC  cancel" else "S  skip", skip.y + skip.height / 2f - 6f, w)
        batch.end()
    }

    // Apply the current human's draft input to their climber. Number key or tapped
    // card picks an option (a swap target in the swap step); S / ESC / the skip band
    // declines or cancels. Edge-triggered: one press resolves one choice. Resolving
    // removes the DraftOffer, so DraftSystem advances to the next human or unpauses.
    private fun handleDraftInput() {
        val e = draft.currentHuman ?: return
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
        if (owned.size < Draft.MAX_AUGMENTS) {
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
        val p = hudViewport.unproject(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
        optionRects(count, hudViewport.worldWidth, hudViewport.worldHeight)
            .forEachIndexed { i, r -> if (r.contains(p.x, p.y)) return i }
        return null
    }

    private fun skipTapped(): Boolean {
        if (!Gdx.input.justTouched()) return false
        val p = hudViewport.unproject(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
        return skipRect(hudViewport.worldWidth, hudViewport.worldHeight).contains(p.x, p.y)
    }

    // Hands-free run returns to the menu (which auto-starts again) after a beat;
    // otherwise wait for a key/tap, as before.
    private fun handleGameOver(delta: Float) {
        if (Roster.handsFree) {
            deadElapsed += delta
            if (deadElapsed >= AUTO_RESTART_DELAY) game.setScreen<MenuScreen>()
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.justTouched()) {
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
        if (!built) return
        world.dispose()
        physicsWorld.dispose()
        built = false
    }

    companion object {
        private const val AUTO_RESTART_DELAY = 1.5f
        private val NUM_KEYS = intArrayOf(
            Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5,
        )
        private val NUMPAD_KEYS = intArrayOf(
            Input.Keys.NUMPAD_1, Input.Keys.NUMPAD_2, Input.Keys.NUMPAD_3, Input.Keys.NUMPAD_4, Input.Keys.NUMPAD_5,
        )
    }
}
