package com.yikers.component

import com.yikers.config.GameConfig
import com.yikers.ecs.component.augment.AugmentCatalog
import com.yikers.ecs.component.augment.AugmentOffer
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.resource.AugmentChoices.Choice
import com.yikers.support.HeadlessGdx
import com.yikers.support.buildSim
import com.yikers.support.step
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Acquisition flow: every AUGMENT_OFFER_INTERVAL score the climber is offered
// AUGMENT_OFFER_CHOICES unowned augments; answers (pick/skip/swap) resolve via
// the AugmentChoices latch exactly as wire input does.
@HeadlessGdx
class AugmentAcquisitionTest {

    @Test
    fun crossingTheThresholdRollsAnOfferAndAPickIsApplied() {
        buildSim(seed = 7L).use { sim ->
            with(sim.world) {
                sim.runState.score = GameConfig.AUGMENT_OFFER_INTERVAL
                sim.world.step(1)

                val offer = sim.player.getOrNull(AugmentOffer)
                assertNotNull(offer) { "crossing the threshold must roll an offer" }
                val options = offer!!.options
                assertEquals(GameConfig.AUGMENT_OFFER_CHOICES, options.size)
                assertEquals(options.size, options.distinct().size) { "options must be distinct" }
                assertTrue(options.none { it in sim.player[Augments].owned }) { "only unowned augments roll" }

                sim.choices.latch(0, Choice(pick = options[0].id, drop = null, skip = false))
                sim.world.step(1)
                assertTrue(options[0] in sim.player[Augments].owned) { "pick must be granted" }
                assertNull(sim.player.getOrNull(AugmentOffer)) { "answered offer must close" }
            }
        }
    }

    @Test
    fun skipClosesTheOfferWithoutGranting() {
        buildSim(seed = 7L).use { sim ->
            with(sim.world) {
                sim.runState.score = GameConfig.AUGMENT_OFFER_INTERVAL
                sim.world.step(1)
                assertNotNull(sim.player.getOrNull(AugmentOffer))

                sim.choices.latch(0, Choice(pick = null, drop = null, skip = true))
                sim.world.step(1)
                assertNull(sim.player.getOrNull(AugmentOffer)) { "skip must close the offer" }
                assertTrue(sim.player[Augments].owned.isEmpty()) { "skip must grant nothing" }
            }
        }
    }

    @Test
    fun aBigScoreJumpYieldsOneOfferAndTheThresholdCatchesUp() {
        buildSim(seed = 7L).use { sim ->
            with(sim.world) {
                sim.runState.score = GameConfig.AUGMENT_OFFER_INTERVAL * 3 + 20
                sim.world.step(1)
                assertNotNull(sim.player.getOrNull(AugmentOffer))
                assertEquals(GameConfig.AUGMENT_OFFER_INTERVAL * 4, sim.runState.nextAugmentScore) {
                    "threshold must catch up past the current score"
                }
            }
        }
    }

    @Test
    fun fullLoadoutRequiresAValidSwap() {
        buildSim(seed = 7L).use { sim ->
            with(sim.world) {
                val owned = sim.player[Augments].owned
                AugmentCatalog.ALL.take(GameConfig.MAX_AUGMENTS).forEach { owned += it }
                val keeper = owned.first()

                sim.runState.score = GameConfig.AUGMENT_OFFER_INTERVAL
                sim.world.step(1)
                val offer = sim.player.getOrNull(AugmentOffer)
                assertNotNull(offer) { "a full loadout still gets offers while the pool has augments" }
                val pick = offer!!.options.first()

                // No drop while full -> rejected, offer stays open.
                sim.choices.latch(0, Choice(pick = pick.id, drop = null, skip = false))
                sim.world.step(1)
                assertFalse(pick in owned) { "a pick at the cap without a drop must be rejected" }
                assertNotNull(sim.player.getOrNull(AugmentOffer)) { "rejected answer keeps the offer open" }

                // Valid swap -> dropped augment leaves, pick enters, cap holds.
                sim.choices.latch(0, Choice(pick = pick.id, drop = keeper.id, skip = false))
                sim.world.step(1)
                assertTrue(pick in owned)
                assertFalse(keeper in owned) { "the swapped-out augment must leave" }
                assertEquals(GameConfig.MAX_AUGMENTS, owned.size)
                assertNull(sim.player.getOrNull(AugmentOffer))
            }
        }
    }

    @Test
    fun aPickOutsideTheOfferIsRejected() {
        buildSim(seed = 7L).use { sim ->
            with(sim.world) {
                sim.runState.score = GameConfig.AUGMENT_OFFER_INTERVAL
                sim.world.step(1)
                val offered = sim.player.getOrNull(AugmentOffer)!!.options
                val outside = AugmentCatalog.ALL.first { it !in offered }

                sim.choices.latch(0, Choice(pick = outside.id, drop = null, skip = false))
                sim.world.step(1)
                assertTrue(sim.player[Augments].owned.isEmpty()) { "an unoffered pick must be rejected" }
                assertNotNull(sim.player.getOrNull(AugmentOffer)) { "the offer stays open" }
            }
        }
    }
}
