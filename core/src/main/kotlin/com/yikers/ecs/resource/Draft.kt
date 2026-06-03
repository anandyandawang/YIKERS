package com.yikers.ecs.resource

import com.github.quillraven.fleks.Entity

// Run-wide augment-draft state. DraftSystem advances nextOfferScore and points
// currentHuman at the human whose offer the screen should show now; each climber's
// actual offer lives on its DraftOffer component, so this stays tiny. The run is
// paused (RunState.paused) while any human offer is still open.
class Draft {
    // Score at which the next draft round fires. DraftSystem bumps it OFFER_INTERVAL.
    var nextOfferScore = OFFER_INTERVAL

    // The human whose offer the screen renders right now (first one still choosing),
    // or null when no human has an open offer. Set by DraftSystem each tick.
    var currentHuman: Entity? = null

    companion object {
        const val OFFER_INTERVAL = 50  // score between draft rounds
        const val OFFER_SIZE = 3       // cards offered to each climber
        const val MAX_AUGMENTS = 5     // owned cap before a pick must swap
    }
}
