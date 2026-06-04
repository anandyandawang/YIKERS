package com.yikers.net

import com.yikers.sim.GameInstance

// In-process GameSession: forwards straight to the embedded GameInstance with no
// serialization. The instance's lifetime is owned by the host (close the room via
// GameHost.close), so close() here is a no-op.
class LocalGameSession(private val instance: GameInstance) : GameSession {
    override fun submitInput(cmd: InputCommand) = instance.applyInput(cmd)
    override fun setViewHeight(height: Float) = instance.setViewHeight(height)
    override fun step(deltaTime: Float) = instance.tick(deltaTime)
    override fun snapshot(): WorldSnapshot = instance.snapshot()
    override fun close() = Unit
}
