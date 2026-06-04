package com.yikers.control

import com.badlogic.gdx.graphics.Color

object Palette {
    fun distinct(i: Int, n: Int): Color {
        if (n <= 1) return Color(Color.CORAL)
        val hue = (i.toFloat() / n) * 360f
        return Color().fromHsv(hue, 0.65f, 0.95f).also { it.a = 1f }
    }
}
