package com.yikers.control

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.yikers.net.InputAgent
import com.yikers.net.InputCommand
import com.yikers.net.WorldSnapshot

// Key bindings for one human. ARROWS = the original single-player binds (so default
// play is unchanged). WASD is defined for a future second human and is not wired up
// yet. Only one profile should claim touch.
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

// CLIENT-side human agent: reads live keyboard/touch/tilt and packages it as an
// InputCommand. The world snapshot is ignored — a person looks at the screen, not at
// the wire. Same InputAgent contract a bot satisfies, so the run loop drives humans
// and bots identically. Held arrow = x-velocity, edge-press = jump (ground gating
// still happens server-side in ControlSystem). On tilt devices horizontal comes from
// the accelerometer: vx = -speed * accelX (raw, no deadzone), matching YIKES. `speed`
// (horizontalSpeed) is passed in since the client no longer owns the sim RunConfig.
class HumanAgent(
    private val speed: Float,
    private val keys: KeyProfile = KeyProfile.ARROWS,
) : InputAgent {
    override fun decide(world: WorldSnapshot, selfId: Int, dt: Float): InputCommand {
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
        return InputCommand(selfId, vx, jump)
    }
}
