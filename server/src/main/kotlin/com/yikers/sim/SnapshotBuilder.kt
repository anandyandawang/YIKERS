package com.yikers.sim

import com.github.quillraven.fleks.World
import com.yikers.ecs.component.PlatformC
import com.yikers.ecs.component.Player
import com.yikers.ecs.component.RenderShape
import com.yikers.ecs.component.Transform
import com.yikers.ecs.component.augment.AugmentOffer
import com.yikers.ecs.component.augment.Augments
import com.yikers.ecs.resource.RunState
import com.yikers.net.EntitySnap
import com.yikers.net.PlatformSnap
import com.yikers.net.PlayerSnap
import com.yikers.net.PropSnap
import com.yikers.net.WorldSnapshot

// Live world -> wire. Player presence picks PlayerSnap vs PropSnap.
class SnapshotBuilder(
    private val world: World,
    private val runState: RunState,
) {
    private val renderables = world.family { all(Transform, RenderShape) }
    private val platforms = world.family { all(PlatformC) }

    fun build(tick: Long): WorldSnapshot {
        val ents = ArrayList<EntitySnap>()
        val plats = ArrayList<PlatformSnap>()
        with(world) {
            renderables.forEach { e ->
                val t = e[Transform]
                val rs = e[RenderShape]
                val player = e.getOrNull(Player)
                ents += if (player != null) {
                    PlayerSnap(
                        rs.kind, rs.color.r, rs.color.g, rs.color.b, rs.color.a,
                        t.position.x, t.position.y, t.size.x, t.size.y, t.rotation,
                        slot = player.slot,
                        augments = e[Augments].owned.map { it.id },
                        offer = e.getOrNull(AugmentOffer)?.options?.map { it.id } ?: emptyList(),
                    )
                } else {
                    PropSnap(
                        rs.kind, rs.color.r, rs.color.g, rs.color.b, rs.color.a,
                        t.position.x, t.position.y, t.size.x, t.size.y, t.rotation,
                        id = e.id,
                    )
                }
            }
            platforms.forEach { e ->
                val p = e[PlatformC]
                plats += PlatformSnap(p.y, p.holeX, p.holeWidth)
            }
        }
        return WorldSnapshot(
            tick = tick,
            entities = ents,
            platforms = plats,
            score = runState.score,
            dead = runState.dead,
            scrollY = runState.scrollY,
            highScore = runState.highScore,
        )
    }
}
