package com.yikers.net

// One client = a session (transport) + an agent (brain). pump() = read, decide, send.
class Participant(
    val session: GameSession,
    private val agent: InputAgent,
) {
    fun pump(dt: Float) {
        val cmd = agent.decide(session.snapshot(), session.playerId, dt)
        session.submitInput(cmd.copy(playerId = session.playerId))
    }

    fun close() = session.close()
}
