package com.yikers.net

import com.yikers.net.wire.AugmentOffer
import com.yikers.net.wire.AugmentPick

// A client's handle to ONE room: THE SEAM. Local + Network implement it identically.
interface GameSession {
    val slot: Int   // this client's seat, assigned at join

    fun submitInput(cmd: InputCommand)
    fun submitAugmentPick(pick: AugmentPick)
    fun snapshot(): WorldSnapshot

    // Augment offer is event-driven, not in the snapshot. Non-null = show the picker;
    // awaitingAugmentResume = picked, room still frozen on others ("waiting").
    fun augmentOffer(): AugmentOffer?
    fun awaitingAugmentResume(): Boolean

    fun close()
}
