package com.yikers.control

import com.yikers.net.InputCommand

// Server-side stand-in for a human climber. The real input is read on the client
// (HumanInput -> InputCommand) and relayed across the GameSession seam; this holds
// the latest command and feeds it to ControlSystem like any other Controller, so
// ControlSystem itself is unchanged. Jump is latched so an edge press is never
// dropped between client frame rate and sim ticks.
class HumanRelayController(val slot: Int) : Controller {
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
