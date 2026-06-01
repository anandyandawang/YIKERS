package com.yikers.control

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

// Key bindings for one human. ARROWS = the original single-player binds (so
// default play is unchanged). WASD is defined for a future second human and is
// not wired up yet. Only one profile should claim touch.
data class KeyProfile(
    val left: Int,
    val right: Int,
    val jump1: Int,
    val jump2: Int,
    val touchJumps: Boolean,
) {
    companion object {
        val ARROWS = KeyProfile(Input.Keys.LEFT, Input.Keys.RIGHT, Input.Keys.SPACE, Input.Keys.UP, touchJumps = true)
        val WASD = KeyProfile(Input.Keys.A, Input.Keys.D, Input.Keys.W, Input.Keys.W, touchJumps = false)
    }
}

// Reads live keyboard/touch. Same feel as before: held arrow = x-velocity,
// edge-press = jump (gating on ground happens in ControlSystem).
class HumanController(private val keys: KeyProfile = KeyProfile.ARROWS) : Controller {
    override fun decide(ctx: ControlContext): Move {
        val left = Gdx.input.isKeyPressed(keys.left)
        val right = Gdx.input.isKeyPressed(keys.right)
        val vx = when {
            left && !right -> -ctx.speed
            right && !left -> ctx.speed
            else -> 0f
        }
        val jump = Gdx.input.isKeyJustPressed(keys.jump1) ||
            Gdx.input.isKeyJustPressed(keys.jump2) ||
            (keys.touchJumps && Gdx.input.justTouched())
        return Move(vx, jump)
    }
}
