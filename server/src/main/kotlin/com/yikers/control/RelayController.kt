package com.yikers.control

import com.yikers.net.InputCommand

// Server-side stand-in for ONE client's climber — human or bot, the server can't
// tell. The real decision is made on the client (a human's keypresses or a bot's
// BotBrain reading the WorldSnapshot) and relayed across the GameSession seam as an
// InputCommand; this holds the latest command and feeds it to ControlSystem like
// any other Controller. Jump is latched so an edge press is never dropped between
// client frame rate and sim ticks.
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
