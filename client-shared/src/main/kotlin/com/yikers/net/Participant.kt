package com.yikers.net

// One client = a session (transport) + an agent (brain). pump() = read, decide, send.
class Participant(
    val session: GameSession,
    private val agent: InputAgent,
) {
    fun pump(dt: Float) {
        session.submitInput(agent.decide(session.snapshot(), session.slot, dt))
    }

    fun close() = session.close()
}
