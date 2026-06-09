package com.yikers.net

import com.yikers.net.wire.AugmentPick

// A client's handle to ONE room: THE SEAM. Local + Network implement it identically.
interface GameSession {
    val slot: Int   // this client's seat, assigned at join

    fun submitInput(cmd: InputCommand)
    fun submitAugmentPick(pick: AugmentPick)
    fun snapshot(): WorldSnapshot
    fun close()
}
