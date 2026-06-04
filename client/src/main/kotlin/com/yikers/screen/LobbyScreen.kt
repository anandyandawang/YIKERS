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
import com.yikers.bot.BotAgent
import com.yikers.config.GameConfig
import com.yikers.control.BootConfig
import com.yikers.net.DedicatedServer
import com.yikers.net.Participant
import com.yikers.net.Session
import com.yikers.net.SessionConfig
import com.yikers.net.discovery.DEFAULT_TCP_PORT
import com.yikers.net.discovery.DiscoveredServer
import com.yikers.net.discovery.LanScanner
import ktx.app.KtxScreen
import java.net.InetAddress
import kotlin.concurrent.thread

// Multiplayer lobby: broadcasts a LAN discovery query, lists the servers that answer
// as clickable rows, and offers Host (start an in-process server + join it), Join
// 127.0.0.1 (direct-connect fallback when UDP discovery is filtered, e.g. the
// one-machine demo), Refresh, and Back. Same asset-free ShapeRenderer + font style as
// the menu. Selecting any target stashes it in Session and switches to PlayScreen.
class LobbyScreen(private val game: YikersGame) : KtxScreen {
    private val viewport = ExtendViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX)
    private val layout = GlyphLayout()
    private val touch = Vector2()

    // Discovered servers + their on-screen row rects, both rebuilt each scan/frame.
    @Volatile
    private var servers: List<DiscoveredServer> = emptyList()

    @Volatile
    private var scanning = false

    private val rows = ArrayList<Pair<Rectangle, DiscoveredServer>>()

    private val hostBtn = Rectangle()
    private val joinLocalBtn = Rectangle()
    private val refreshBtn = Rectangle()
    private val backBtn = Rectangle()

    override fun show() {
        servers = emptyList()
        startScan()
    }

    private fun startScan() {
        if (scanning) return
        scanning = true
        thread(name = "yikers-lan-scan", isDaemon = true) {
            try {
                servers = LanScanner.scan()
            } finally {
                scanning = false
            }
        }
    }

    override fun render(delta: Float) {
        viewport.apply()
        val w = viewport.worldWidth
        val h = viewport.worldHeight
        layoutButtons(w, h)
        layoutRows(w, h)
        handleInput()

        ScreenUtils.clear(0.10f, 0.12f, 0.16f, 1f)

        val shape = game.shape
        shape.projectionMatrix = viewport.camera.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = ROW_FILL
        rows.forEach { (r, _) -> shape.rect(r.x, r.y, r.width, r.height) }
        shape.color = BUTTON_FILL
        listOf(hostBtn, joinLocalBtn, refreshBtn, backBtn).forEach {
            shape.rect(it.x, it.y, it.width, it.height)
        }
        shape.end()

        val batch = game.batch
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        game.font.color = Color.CORAL
        centered("LAN GAMES", h - 28f, w)
        game.font.color = Color.LIGHT_GRAY
        centered(statusLine(), h - 60f, w)
        game.font.color = Color.WHITE
        rows.forEach { (r, s) -> labelLeft(s.label(), r) }
        labelIn("HOST", hostBtn)
        labelIn("JOIN 127.0.0.1", joinLocalBtn)
        labelIn("REFRESH", refreshBtn)
        labelIn("BACK", backBtn)
        batch.end()
    }

    private fun statusLine(): String = when {
        scanning -> "scanning the LAN..."
        servers.isEmpty() -> "no servers found - host one, or join 127.0.0.1"
        else -> "tap a game to join"
    }

    private fun layoutButtons(w: Float, h: Float) {
        val gap = 10f
        val bw = (w - gap * 5f) / 4f
        val bh = 56f
        val y = 24f
        var x = gap
        hostBtn.set(x, y, bw, bh); x += bw + gap
        joinLocalBtn.set(x, y, bw, bh); x += bw + gap
        refreshBtn.set(x, y, bw, bh); x += bw + gap
        backBtn.set(x, y, bw, bh)
    }

    private fun layoutRows(w: Float, h: Float) {
        rows.clear()
        val list = servers
        val rowH = 52f
        val gap = 8f
        val margin = 24f
        val top = h - 90f
        list.forEachIndexed { i, s ->
            val y = top - (i + 1) * (rowH + gap)
            if (y < 90f) return@forEachIndexed // don't overlap the action buttons
            rows.add(Rectangle(margin, y, w - margin * 2f, rowH) to s)
        }
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen<MenuScreen>()
            return
        }
        if (!Gdx.input.justTouched()) return
        touch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        viewport.unproject(touch)
        when {
            hostBtn.contains(touch) -> startHost()
            joinLocalBtn.contains(touch) -> joinTarget("127.0.0.1", DEFAULT_TCP_PORT)
            refreshBtn.contains(touch) -> startScan()
            backBtn.contains(touch) -> game.setScreen<MenuScreen>()
            else -> rows.firstOrNull { it.first.contains(touch) }
                ?.let { (_, s) -> joinTarget(s.host, s.port) }
        }
    }

    // Start an in-process server and immediately join it as a human client. Other
    // clients find it via discovery, or direct-connect to 127.0.0.1 on the same box.
    // BootConfig.bots in-process bot clients are attached to the hosted server: they
    // get a slot via the server's generic localSession() and are pumped on its tick
    // thread (~1-tick lag, so they actually climb). The server stays bot-blind.
    private fun startHost() {
        val cfg = SessionConfig()
        val name = "Host @ ${hostName()}"
        // Prefer the well-known port so a second local client can direct-connect even
        // if UDP discovery is blocked; fall back to an ephemeral port if it's taken.
        val server = runCatching { DedicatedServer(name, DEFAULT_TCP_PORT, cfg) }
            .getOrElse { DedicatedServer(name, 0, cfg) }
        repeat(BootConfig.bots) {
            val bot = Participant(server.localSession(), BotAgent(cfg.runConfig))
            server.addLocalPump(bot::pump)
        }
        server.start()
        Session.setHosted(server)
        joinTarget("127.0.0.1", server.port)
    }

    private fun joinTarget(host: String, port: Int) {
        Session.network(host, port)
        game.setScreen<PlayScreen>()
    }

    private fun hostName(): String =
        runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("me")

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

    private fun labelLeft(text: String, rect: Rectangle) {
        layout.setText(game.font, text)
        game.font.draw(game.batch, text, rect.x + 12f, rect.y + (rect.height + layout.height) / 2f)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    companion object {
        private val ROW_FILL = Color(0.16f, 0.20f, 0.26f, 1f)
        private val BUTTON_FILL = Color(0.18f, 0.22f, 0.30f, 1f)
    }
}
