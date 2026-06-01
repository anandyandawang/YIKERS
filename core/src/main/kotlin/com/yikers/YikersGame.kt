package com.yikers

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.yikers.screen.MenuScreen
import com.yikers.screen.PlayScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen

/**
 * YIKERS = roguelike-flavored vertical climber. Inspired by YIKES.
 * App shell: holds shared render resources, swaps between menu + play screens.
 * Game sim live in a Fleks ECS world inside PlayScreen.
 */
class YikersGame : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    val shape by lazy { ShapeRenderer() }
    val font by lazy { BitmapFont() }

    override fun create() {
        addScreen(MenuScreen(this))
        addScreen(PlayScreen(this))
        setScreen<MenuScreen>()
    }

    override fun dispose() {
        super.dispose() // disposes registered screens
        batch.dispose()
        shape.dispose()
        font.dispose()
    }
}
