package com.yikers.control

import com.badlogic.gdx.graphics.Color

// Distinct climber colors. Roster size is known at spawn, so evenly-spaced HSV
// hues give maximum separation (golden-angle stepping is only needed when N is
// unknown/incremental). Solo run keeps the original coral.
object Palette {
    fun distinct(i: Int, n: Int): Color {
        if (n <= 1) return Color(Color.CORAL)
        val hue = (i.toFloat() / n) * 360f
        return Color().fromHsv(hue, 0.65f, 0.95f).also { it.a = 1f }
    }
}
