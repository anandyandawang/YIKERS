package com.yikers.net

import com.yikers.sim.GameInstance

// In-process GameSession: forwards straight to the embedded GameInstance with no
// serialization. Bound to ONE player slot (allocated at join), so a human client
// and a bot client are the same kind of object here — each owns a slot and relays
// its InputCommand. The instance's lifetime is owned by the host (close the room
// via GameHost.close); close() here just drops this client's player from the room.
class LocalGameSession(
    private val instance: GameInstance,
    override val playerId: Int,
) : GameSession {
    override fun submitInput(cmd: InputCommand) = instance.applyInput(cmd)
    override fun step(deltaTime: Float) = instance.tick(deltaTime)
    override fun snapshot(): WorldSnapshot = instance.snapshot()
    override fun close() = instance.removePlayer(playerId)
}
