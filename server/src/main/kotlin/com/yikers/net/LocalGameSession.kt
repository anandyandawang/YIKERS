package com.yikers.net

import com.yikers.sim.GameInstance

// In-process GameSession: forwards to the GameInstance for one slot.
class LocalGameSession(
    private val instance: GameInstance,
    override val playerId: Int,
) : GameSession {
    override fun submitInput(cmd: InputCommand) = instance.applyInput(cmd)
    override fun step(deltaTime: Float) = instance.tick(deltaTime)
    override fun snapshot(): WorldSnapshot = instance.snapshot()
    override fun close() = instance.removePlayer(playerId)
}
