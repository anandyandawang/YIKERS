package com.yikers.bot

import com.yikers.config.RunConfig
import com.yikers.net.InputAgent
import com.yikers.net.InputCommand
import com.yikers.net.WorldSnapshot

class BotAgent(runConfig: RunConfig) : InputAgent {
    private val percept = SnapshotPercept(runConfig)
    private val brain = BotBrain()

    override fun decide(world: WorldSnapshot, selfId: Int, dt: Float): InputCommand {
        percept.update(world, selfId)
        val move = brain.decide(percept.self, percept.view)
        return InputCommand(selfId, move.vx, move.jump)
    }
}
