package com.yikers.ecs.system

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.GameConfig
import com.yikers.ecs.component.Dead
import com.yikers.ecs.component.Player
import com.yikers.ecs.component.augment.Augment
import com.yikers.ecs.component.augment.AugmentCatalog
import com.yikers.ecs.component.augment.AugmentOffer
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.resource.RunState

// Every AUGMENT_OFFER_INTERVAL score, each living climber without a pending offer
// rolls AUGMENT_OFFER_CHOICES random augments it doesn't own (fewer if the pool
// runs dry; none if empty). A multi-threshold score jump still yields ONE offer —
// the threshold just catches up. Rolls use MathUtils.random so seeds reproduce.
class AugmentOfferSystem(
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Player, Augments).none(Dead, AugmentOffer) }) {
    override fun onTick() {
        if (runState.dead) return
        if (runState.score < runState.nextAugmentScore) return
        while (runState.score >= runState.nextAugmentScore) {
            runState.nextAugmentScore += GameConfig.AUGMENT_OFFER_INTERVAL
        }
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val pool = AugmentCatalog.ALL.filterNot { it in entity[Augments].owned }.toMutableList()
        if (pool.isEmpty()) return
        val options = ArrayList<Augment>(GameConfig.AUGMENT_OFFER_CHOICES)
        repeat(minOf(GameConfig.AUGMENT_OFFER_CHOICES, pool.size)) {
            options += pool.removeAt(MathUtils.random(pool.size - 1))
        }
        entity.configure { it += AugmentOffer(options) }
    }
}
