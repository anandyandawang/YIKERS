package com.yikers.net

import com.yikers.sim.GameInstance

// In-process GameSession: forwards straight to the GameInstance, bound to one slot
// (allocated at join). close() drops this client's player; the room itself is owned
// by the host.
class LocalGameSession(
    private val instance: GameInstance,
    override val playerId: Int,
) : GameSession {
    override fun submitInput(cmd: InputCommand) = instance.applyInput(cmd)
    override fun step(deltaTime: Float) = instance.tick(deltaTime)
    override fun snapshot(): WorldSnapshot = instance.snapshot()
    override fun close() = instance.removePlayer(playerId)
}
