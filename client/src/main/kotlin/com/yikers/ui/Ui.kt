package com.yikers.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle

// Flat-UI palette shared by every screen.
object UiColors {
    val BG = Color(0.10f, 0.12f, 0.16f, 1f)
    val BUTTON = Color(0.18f, 0.22f, 0.30f, 1f)
    val ROW = Color(0.16f, 0.20f, 0.26f, 1f)
}

// Text placement helpers over one reused GlyphLayout. Caller sets font.color.
class UiText(private val font: BitmapFont, private val batch: SpriteBatch) {
    private val layout = GlyphLayout()

    fun centered(text: String, y: Float, w: Float) {
        layout.setText(font, text)
        font.draw(batch, text, (w - layout.width) / 2f, y)
    }

    fun inRect(text: String, rect: Rectangle) {
        layout.setText(font, text)
        font.draw(
            batch, text,
            rect.x + (rect.width - layout.width) / 2f,
            rect.y + (rect.height + layout.height) / 2f,
        )
    }

    fun leftIn(text: String, rect: Rectangle) {
        layout.setText(font, text)
        font.draw(batch, text, rect.x + 12f, rect.y + (rect.height + layout.height) / 2f)
    }
}
