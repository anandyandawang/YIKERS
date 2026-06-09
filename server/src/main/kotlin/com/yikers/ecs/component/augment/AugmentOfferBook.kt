package com.yikers.ecs.component.augment

// Per-player augment offers at score milestones, plus applying the pick. World- and
// score-agnostic: the caller feeds the score and a way to reach each player's owned
// set, so this is pure, testable logic. While any offer is open (isPaused) the caller
// should freeze the sim for everyone.
class AugmentOfferBook(private val step: Int = 50) {
    private val offers = HashMap<Int, List<Augment>>()

    var nextScore = step
        private set

    val isPaused: Boolean get() = offers.isNotEmpty()

    fun offerFor(slot: Int): List<Augment>? = offers[slot]
    fun activeSlots(): Set<Int> = offers.keys.toSet()

    // Deal each player their own random unowned draw when score crosses the next
    // milestone. No-op if a round is already open or the run is over. A player with
    // nothing left to offer is simply skipped; if that is everyone, advance the gate.
    fun maybeOpen(score: Int, dead: Boolean, slots: Iterable<Int>, owned: (Int) -> Set<Augment>) {
        if (offers.isNotEmpty() || dead || score < nextScore) return
        for (slot in slots) {
            val choices = (AugmentCatalog.all - owned(slot))
                .shuffled()
                .take(AugmentCatalog.OFFER_SIZE)
            if (choices.isNotEmpty()) offers[slot] = choices
        }
        if (offers.isEmpty()) nextScore += step
    }

    // augmentId null = skip. At the cap a valid swapOutId is required, else the pick
    // is ignored and the offer stays open (UI must force a swap choice).
    fun resolve(slot: Int, augmentId: String?, swapOutId: String?, owned: MutableSet<Augment>) {
        val offer = offers[slot] ?: return
        val chosen = augmentId?.let { id -> offer.firstOrNull { it.id == id } }
        if (chosen != null) {
            if (owned.size >= AugmentCatalog.MAX_OWNED) {
                val out = swapOutId?.let { id -> owned.firstOrNull { it.id == id } } ?: return
                owned.remove(out)
            }
            owned.add(chosen)
        }
        clear(slot)
    }

    // Player left mid-round: forget their offer so it can't freeze the room.
    fun drop(slot: Int) {
        if (slot in offers) clear(slot)
    }

    private fun clear(slot: Int) {
        offers.remove(slot)
        if (offers.isEmpty()) nextScore += step
    }
}
