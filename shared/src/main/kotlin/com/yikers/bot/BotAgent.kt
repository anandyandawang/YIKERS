package com.yikers.bot

import com.yikers.config.RunConfig
import com.yikers.net.InputAgent
import com.yikers.net.InputCommand
import com.yikers.net.WorldSnapshot

// A bot's brain expressed as an InputAgent: rebuild a percept from the snapshot, run
// the BotBrain, emit an InputCommand. Same contract a human agent satisfies — the
// only difference is a human reads a keyboard and this reads the world. Drop it into
// a Participant alongside any GameSession (in-process or a real socket) and it's a
// client like any other; the server can't tell it from a person.
class BotAgent(runConfig: RunConfig) : InputAgent {
    private val percept = SnapshotPercept(runConfig)
    private val brain = BotBrain()

    override fun decide(world: WorldSnapshot, selfId: Int, dt: Float): InputCommand {
        percept.update(world, selfId)
        val move = brain.decide(percept.self, percept.view)
        return InputCommand(selfId, move.vx, move.jump)
    }
}
