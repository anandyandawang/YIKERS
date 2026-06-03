package com.yikers.ecs.system

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.control.BotController
import com.yikers.control.HumanController
import com.yikers.ecs.component.Controlled
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.DraftOffer
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.component.augment.rollAugmentOffer
import com.yikers.ecs.resource.Draft
import com.yikers.ecs.resource.RunState

// Augment acquisition. When the run's score crosses the next OFFER_INTERVAL, every
// living climber is offered up to OFFER_SIZE unowned augments (a DraftOffer). Bots
// resolve at once -- they pick at random. Humans keep their offer until the screen
// applies their choice, and the run stays paused (RunState.paused) until no human
// offer remains: "one player crosses 50" opens the round, "all players have picked"
// closes it. Advancing the threshold before rolling means an exhausted catalog
// (nothing new to offer anyone) just skips the round instead of retrying each tick.
class DraftSystem(
    private val runState: RunState = inject(),
    private val draft: Draft = inject(),
) : IntervalSystem() {
    private val draftable = family { all(Controlled, Augments).none(Dead) }
    private val pending = family { all(DraftOffer) }

    override fun onTick() {
        if (runState.dead) {
            runState.paused = false
            return
        }
        with(world) {
            if (pending.numEntities == 0 && runState.score >= draft.nextOfferScore) {
                draft.nextOfferScore += Draft.OFFER_INTERVAL
                openRound()
            }
            resolveBots()
            draft.currentHuman = firstPendingHuman()
            runState.paused = draft.isAwaitingHuman
        }
    }

    // Offer each living climber its own roll of unowned augments. A climber that
    // already owns everything gets no offer (empty roll -> no component added).
    private fun openRound() = with(world) {
        draftable.forEach { e ->
            val options = rollAugmentOffer(e[Augments].owned, Draft.OFFER_SIZE)
            if (options.isNotEmpty()) e.configure { it += DraftOffer(options) }
        }
    }

    // Bots grab a random offered augment at once; at the cap they drop a random
    // owned one first. No skip -- a bot always takes something. Collect before
    // mutating so removing DraftOffer doesn't disturb the scan.
    private fun resolveBots() = with(world) {
        val bots = ArrayList<Entity>()
        pending.forEach { e -> if (e[Controlled].controller is BotController) bots += e }
        bots.forEach { e ->
            val offer = e[DraftOffer]
            val owned = e[Augments].owned
            val choice = offer.options[MathUtils.random(offer.options.size - 1)]
            if (owned.size >= Draft.MAX_AUGMENTS) {
                owned -= owned.toList()[MathUtils.random(owned.size - 1)]
            }
            owned += choice
            e.configure { it -= DraftOffer }
        }
    }

    // The first human still holding an offer -- the one the screen prompts now.
    private fun firstPendingHuman(): Entity? = with(world) {
        var found: Entity? = null
        pending.forEach { e ->
            if (found == null && e[Controlled].controller is HumanController) found = e
        }
        found
    }
}
