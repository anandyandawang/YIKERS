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
import com.yikers.config.GameConfig
import com.yikers.net.DedicatedServer
import com.yikers.net.Session
import com.yikers.net.SessionConfig
import com.yikers.net.discovery.DEFAULT_TCP_PORT
import com.yikers.net.discovery.DiscoveredServer
import com.yikers.net.discovery.LanScanner
import com.yikers.ui.UiColors
import com.yikers.ui.UiText
import ktx.app.KtxScreen
import java.net.InetAddress
import kotlin.concurrent.thread

// Multiplayer lobby: LAN discovery rows + Host / Join 127.0.0.1 / Refresh / Back.
class LobbyScreen(private val game: YikersGame) : KtxScreen {
    private val viewport = ExtendViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX)
    private val ui = UiText(game.font, game.batch)
    private val touch = Vector2()

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

        ScreenUtils.clear(UiColors.BG)

        val shape = game.shape
        shape.projectionMatrix = viewport.camera.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = UiColors.ROW
        rows.forEach { (r, _) -> shape.rect(r.x, r.y, r.width, r.height) }
        shape.color = UiColors.BUTTON
        listOf(hostBtn, joinLocalBtn, refreshBtn, backBtn).forEach {
            shape.rect(it.x, it.y, it.width, it.height)
        }
        shape.end()

        val batch = game.batch
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        game.font.color = Color.CORAL
        ui.centered("LAN GAMES", h - 28f, w)
        game.font.color = Color.LIGHT_GRAY
        ui.centered(statusLine(), h - 60f, w)
        game.font.color = Color.WHITE
        rows.forEach { (r, s) -> ui.leftIn(s.label(), r) }
        ui.inRect("HOST", hostBtn)
        ui.inRect("JOIN 127.0.0.1", joinLocalBtn)
        ui.inRect("REFRESH", refreshBtn)
        ui.inRect("BACK", backBtn)
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

    // Start an in-process server + join it. Well-known port, else ephemeral.
    private fun startHost() {
        val cfg = SessionConfig()
        val name = "Host @ ${hostName()}"
        val server = runCatching { DedicatedServer(name, DEFAULT_TCP_PORT, cfg) }
            .getOrElse { DedicatedServer(name, 0, cfg) }
        Session.hostAndJoin(server)
        game.setScreen<PlayScreen>()
    }

    private fun joinTarget(host: String, port: Int) {
        Session.network(host, port)
        game.setScreen<PlayScreen>()
    }

    private fun hostName(): String =
        runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("me")

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }
}
