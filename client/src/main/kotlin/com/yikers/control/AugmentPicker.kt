package com.yikers.control

import com.badlogic.gdx.Gdx
import com.yikers.net.AugmentId
import com.yikers.net.InputAgent
import com.yikers.net.InputCommand
import com.yikers.net.PlayerSnap
import com.yikers.net.WorldSnapshot

// Decorates the human agent with one-shot augment-offer answers from the overlay.
// While an offer is open, taps belong to the overlay, not the jump button (keys
// still jump — the run keeps going behind the overlay).
class AugmentPicker(private val inner: InputAgent) : InputAgent {
    private var pick: AugmentId? = null
    private var drop: AugmentId? = null
    private var skip = false

    fun choose(pickId: AugmentId, dropId: AugmentId? = null) {
        pick = pickId
        drop = dropId
    }

    fun skipOffer() {
        skip = true
    }

    override fun decide(world: WorldSnapshot, slot: Int, dt: Float): InputCommand {
        var cmd = inner.decide(world, slot, dt)
        val offerOpen = world.entities.filterIsInstance<PlayerSnap>()
            .firstOrNull { it.slot == slot }?.offer?.isNotEmpty() == true
        if (offerOpen && Gdx.input.justTouched()) cmd = cmd.copy(jump = false)
        if (pick != null || skip) {
            cmd = cmd.copy(pick = pick, drop = drop, skipOffer = skip)
            pick = null
            drop = null
            skip = false
        }
        return cmd
    }
}
