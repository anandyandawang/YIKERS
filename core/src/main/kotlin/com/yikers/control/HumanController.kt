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
// On tilt devices (iOS/Android) horizontal comes from the accelerometer
// instead, replicating YIKES exactly: vx = -speed * accelX (raw, no deadzone,
// no clamp). Same speed knob as the keys, so one number tunes both inputs;
// speed = 4 = YIKES 20 * 0.2 timescale, matching YIKES' -20 * accelX. Desktop
// has no accelerometer so it keeps the keys.
class HumanController(private val keys: KeyProfile = KeyProfile.ARROWS) : Controller {
    override fun decide(ctx: ControlContext): Move {
        val tilt = Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer)
        val vx = if (tilt) {
            -ctx.speed * Gdx.input.accelerometerX
        } else {
            val left = Gdx.input.isKeyPressed(keys.left)
            val right = Gdx.input.isKeyPressed(keys.right)
            when {
                left && !right -> -ctx.speed
                right && !left -> ctx.speed
                else -> 0f
            }
        }
        val jump = Gdx.input.isKeyJustPressed(keys.jump1) ||
            Gdx.input.isKeyJustPressed(keys.jump2) ||
            (keys.touchJumps && Gdx.input.justTouched())
        return Move(vx, jump)
    }
}
