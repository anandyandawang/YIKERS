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
import com.yikers.screen.MenuScreen
import com.yikers.screen.PlayScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen
import java.io.PrintWriter
import java.io.StringWriter

/**
 * YIKERS = roguelike-flavored vertical climber. Inspired by YIKES.
 * App shell: holds shared render resources, swaps between menu + play screens.
 * Game sim live in a Fleks ECS world inside PlayScreen.
 */
class YikersGame : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    val shape by lazy { ShapeRenderer() }
    val font by lazy { BitmapFont() }

    // Diagnostic crash screen. No device logs available on iOS, so instead of
    // letting an uncaught throwable kill the app (which looks like an instant
    // crash on tap), we catch it in the render loop and paint the stack trace
    // on-screen. Read it off the device to find the real fault. Remove once the
    // iOS crash is fixed.
    private var crashText: String? = null
    private val crashViewport by lazy { FitViewport(GameConfig.WIDTH_PX, GameConfig.HEIGHT_PX) }

    override fun create() {
        addScreen(MenuScreen(this))
        addScreen(PlayScreen(this))
        setScreen<MenuScreen>()
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
        }
    }

    // Paint the captured stack trace, top-aligned + wrapped, on a red field.
    private fun drawCrash() {
        // Re-size every frame: the viewport was never sized via resize() before
        // the crash, so apply() would set a 0x0 glViewport and draw nothing.
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
        super.dispose() // disposes registered screens
        batch.dispose()
        shape.dispose()
        font.dispose()
    }
}
