package com.yikers.control

import com.yikers.net.InputCommand

// Server-side stand-in for one client's climber (human or bot, indistinguishable).
// Holds the latest relayed InputCommand. Jump is latched so an edge press isn't
// dropped between client frame rate and sim ticks.
class RelayController(val slot: Int) : Controller {
    private var vx = 0f
    private var jumpLatched = false

    fun submit(cmd: InputCommand) {
        vx = cmd.vx
        if (cmd.jump) jumpLatched = true
    }

    override fun decide(ctx: ControlContext): Move {
        val jump = jumpLatched
        jumpLatched = false
        return Move(vx, jump)
    }
}
