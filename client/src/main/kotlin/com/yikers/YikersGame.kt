package com.yikers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.yikers.config.GameConfig
import com.yikers.net.Session
import com.yikers.screen.LobbyScreen
import com.yikers.screen.MenuScreen
import com.yikers.screen.PlayScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen
import java.io.PrintWriter
import java.io.StringWriter

// App shell: shared render resources, swaps menu + play screens. Sim in a Fleks ECS
// world inside PlayScreen.
class YikersGame : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    val shape by lazy { ShapeRenderer() }
    val font by lazy { BitmapFont() }

    // Diagnostic crash screen: no iOS device logs, so catch render-loop throwables
    // and paint the trace on-screen. Remove once the iOS crash is fixed.
    private var crashText: String? = null
    private val crashViewport by lazy { FitViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX) }

    override fun create() {
        addScreen(MenuScreen(this))
        addScreen(LobbyScreen(this))
        addScreen(PlayScreen(this))
        // Quick-connect: -Dyikers.connect=host:port skips menu into a network run.
        val connect = System.getProperty("yikers.connect")?.parseHostPort()
        if (connect != null) {
            Session.network(connect.first, connect.second)
            setScreen<PlayScreen>()
        } else {
            setScreen<MenuScreen>()
        }
    }

    override fun render() {
        if (crashText != null) {
            drawCrash()
            return
        }
        try {
            super.render()
        } catch (t: Throwable) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            crashText = sw.toString()
            Gdx.app.error("YIKERS", "uncaught in render loop", t)
            // Font draws nothing on iOS, so persist the trace too. external ==
            // Documents, surfaced in the Files app; local is the fallback.
            runCatching { Gdx.files.external("yikers-crash.txt").writeString(crashText, false) }
            runCatching { Gdx.files.local("yikers-crash.txt").writeString(crashText, false) }
        }
    }

    private fun drawCrash() {
        // Re-size every frame: no resize() ran before the crash, so apply() draws 0x0.
        crashViewport.update(Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight, true)
        ScreenUtils.clear(0.15f, 0.02f, 0.02f, 1f)
        batch.projectionMatrix = crashViewport.camera.combined
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(0.6f)
        font.draw(
            batch, crashText, 6f, GameConfig.HEIGHT_PX - 6f,
            GameConfig.WIDTH_PX - 12f, Align.left, true,
        )
        font.data.setScale(1f)
        batch.end()
    }

    override fun dispose() {
        super.dispose()
        batch.dispose()
        shape.dispose()
        font.dispose()
    }
}

private fun String.parseHostPort(): Pair<String, Int>? {
    val i = lastIndexOf(':')
    if (i <= 0 || i == length - 1) return null
    val port = substring(i + 1).toIntOrNull() ?: return null
    return substring(0, i) to port
}
