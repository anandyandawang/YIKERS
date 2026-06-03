package com.yikers.ecs.resource

// Run-wide augment-draft state -- just the score cadence now. Each climber's actual
// offer lives on its own DraftOffer component (the per-player pool), and the screen
// (DraftOverlay) finds whose offer to show. The run is paused (RunState.paused)
// while any human still owes a pick.
class Draft {
    // Score at which the next draft round fires. DraftSystem bumps it OFFER_INTERVAL.
    var nextOfferScore = OFFER_INTERVAL

    companion object {
        const val OFFER_INTERVAL = 50  // score between draft rounds
        const val OFFER_SIZE = 3       // cards offered to each climber
    }
}
