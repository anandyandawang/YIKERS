package com.yikers.ecs.resource

import com.yikers.net.AugmentId

// Latest augment-offer answer per slot, latched off the wire on the tick thread
// (GameInstance.applyInput) and consumed by AugmentChoiceSystem. One pending
// answer per slot; an answer with no offer open is dropped on read.
class AugmentChoices {
    data class Choice(val pick: AugmentId?, val drop: AugmentId?, val skip: Boolean)

    private val pending = HashMap<Int, Choice>()

    fun latch(slot: Int, choice: Choice) {
        pending[slot] = choice
    }

    fun take(slot: Int): Choice? = pending.remove(slot)

    fun clear(slot: Int) {
        pending.remove(slot)
    }
}
