package com.yikers.control

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.yikers.net.InputAgent
import com.yikers.net.InputCommand
import com.yikers.net.WorldSnapshot

// ARROWS = default binds; WASD is for a future second human (not wired up).
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

// Reads keyboard/touch/tilt into an InputCommand (ignores world). Tilt:
// vx = -speed * accelX (raw, matching YIKES).
class HumanAgent(
    private val speed: Float,
    private val keys: KeyProfile = KeyProfile.ARROWS,
) : InputAgent {
    override fun decide(world: WorldSnapshot, slot: Int, dt: Float): InputCommand {
        val tilt = Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer)
        val vx = if (tilt) {
            -speed * Gdx.input.accelerometerX
        } else {
            val left = Gdx.input.isKeyPressed(keys.left)
            val right = Gdx.input.isKeyPressed(keys.right)
            when {
                left && !right -> -speed
                right && !left -> speed
                else -> 0f
            }
        }
        val jump = Gdx.input.isKeyJustPressed(keys.jump1) ||
            Gdx.input.isKeyJustPressed(keys.jump2) ||
            (keys.touchJumps && Gdx.input.justTouched())
        return InputCommand(slot, vx, jump)
    }
}
