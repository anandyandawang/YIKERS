package com.yikers.net

import com.yikers.net.wire.AugmentPick

// One client = a session (transport) + an agent (brain). pump() = read, decide, send.
// autoPickAugments: brains with no UI (bots) resolve any offer by taking a random
// choice (swapping out a random owned one if already full), so the room never stalls
// waiting on them; the human client resolves offers through its own screen instead.
class Participant(
    val session: GameSession,
    private val agent: InputAgent,
    private val autoPickAugments: Boolean = false,
) {
    fun pump(dt: Float) {
        val snap = session.snapshot()
        if (autoPickAugments) {
            val offer = snap.augmentOffers.firstOrNull { it.slot == session.slot }
            if (offer != null && offer.choices.isNotEmpty()) {
                val choice = offer.choices.random()
                val swapOut = if (offer.owned.size >= offer.maxOwned) offer.owned.random().id else null
                session.submitAugmentPick(AugmentPick(augmentId = choice.id, swapOutId = swapOut))
            }
        }
        val cmd = agent.decide(snap, session.slot, dt)
        session.submitInput(cmd.copy(slot = session.slot))
    }

    fun close() = session.close()
}
