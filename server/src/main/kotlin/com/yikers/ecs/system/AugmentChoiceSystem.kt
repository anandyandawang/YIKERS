package com.yikers.ecs.system

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.ecs.component.Player
import com.yikers.ecs.component.augment.AugmentCatalog
import com.yikers.ecs.component.augment.AugmentOffer
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.resource.AugmentChoices

// Applies a latched wire answer to the climber's pending offer. Authoritative
// validation: the pick must be in the offer and unowned; at MAX_AUGMENTS a valid
// drop is required (the swap). An invalid answer is discarded, the offer stays.
class AugmentChoiceSystem(
    private val choices: AugmentChoices = inject(),
) : IteratingSystem(family { all(Player, Augments) }) {
    override fun onTickEntity(entity: Entity) {
        val choice = choices.take(entity[Player].slot) ?: return
        val offer = entity.getOrNull(AugmentOffer) ?: return // stale answer: no offer open
        if (choice.skip) {
            entity.configure { it -= AugmentOffer }
            return
        }
        val pick = choice.pick?.let(AugmentCatalog::byId) ?: return
        val owned = entity[Augments].owned
        if (pick !in offer.options || pick in owned) return
        if (owned.size >= GameConfig.MAX_AUGMENTS) {
            val drop = choice.drop?.let(AugmentCatalog::byId) ?: return
            if (!owned.remove(drop)) return
        }
        owned += pick
        entity.configure { it -= AugmentOffer }
    }
}
