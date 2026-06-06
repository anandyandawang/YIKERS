package com.yikers.bot

import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.net.EntitySnap
import com.yikers.net.PlatformSnap
import com.yikers.net.PlayerSnap
import com.yikers.net.PropSnap
import com.yikers.net.ShapeKind
import com.yikers.net.WorldSnapshot
import kotlin.math.abs

// Rebuilds BotSelf + BotView from snapshots. velocity/grounded derived off the sim
// tick (not wall-clock), so re-reading a frame can't fake vy = 0.
class SnapshotPercept(private val runConfig: RunConfig) {
    val self = BotSelf()
    val view = BotView()

    private var lastTick = -1L
    private var haveSelf = false
    private var lastSelfY = 0f
    private val prevBoulderX = HashMap<Int, Float>()
    private val prevBoulderY = HashMap<Int, Float>()
    private val prevOtherX = HashMap<Int, Float>()
    private val prevOtherY = HashMap<Int, Float>()
    private val gravity = abs(GameConfig.GRAVITY * runConfig.gravityScale)

    fun update(snap: WorldSnapshot, mySlot: Int) {
        self.speed = runConfig.horizontalSpeed
        self.jumpVelocity = runConfig.jumpVelocity
        view.gravityPxS2 = gravity

        val advanced = snap.tick != lastTick
        val frameDt = if (lastTick < 0) 0f else (snap.tick - lastTick) / GameConfig.SIM_HZ.toFloat()

        locateSelf(snap, mySlot, advanced, frameDt)
        fillHoles(snap.platforms, self.y)
        view.distToKillLine = self.y - snap.scrollY
        fillBoulders(snap.entities, advanced, frameDt)
        fillOthers(snap.entities, mySlot, advanced, frameDt)
        self.grounded = inferGrounded(snap.platforms)

        if (advanced) lastTick = snap.tick
    }

    private fun locateSelf(snap: WorldSnapshot, mySlot: Int, advanced: Boolean, frameDt: Float) {
        val mine = snap.entities.filterIsInstance<PlayerSnap>().firstOrNull { it.slot == mySlot } ?: return  // not spawned yet
        self.x = mine.x
        self.y = mine.y
        if (advanced) {
            self.vy = if (haveSelf && frameDt > 0f) (mine.y - lastSelfY) / frameDt else 0f
            lastSelfY = mine.y
            haveSelf = true
        }
    }

    // Two lowest holes above + the support slab below. holeWidth ~ 0 reads as solid.
    private fun fillHoles(platforms: List<PlatformSnap>, py: Float) {
        var firstY = Float.MAX_VALUE
        var firstCx = GameConfig.WIDTH / 2f
        var firstW = 0f
        var secondY = Float.MAX_VALUE
        var secondCx = GameConfig.WIDTH / 2f
        var secondW = 0f
        var supY = -Float.MAX_VALUE
        var supCx = GameConfig.WIDTH / 2f
        var supW = 0f
        for (p in platforms) {
            val holeW = if (p.holeWidth <= HOLE_EPS) 0f else p.holeWidth
            val cx = if (holeW <= 0f) GameConfig.WIDTH / 2f else p.holeX + p.holeWidth / 2f
            if (p.y > py) {
                if (p.y < firstY) {
                    secondY = firstY; secondCx = firstCx; secondW = firstW
                    firstY = p.y; firstCx = cx; firstW = holeW
                } else if (p.y < secondY) {
                    secondY = p.y; secondCx = cx; secondW = holeW
                }
            } else if (p.y > supY) {
                supY = p.y; supCx = cx; supW = holeW
            }
        }
        view.targetHoleCenterX = firstCx
        view.targetHoleWidth = firstW
        view.targetPlatformY =
            if (firstY == Float.MAX_VALUE) py + GameConfig.PLATFORM_INTERVALS else firstY
        view.nextHoleCenterX = secondCx
        view.nextHoleWidth = if (secondY == Float.MAX_VALUE) 0f else secondW
        view.supportHoleCenterX = supCx
        view.supportHoleWidth = if (supY == -Float.MAX_VALUE) 0f else supW
    }

    // Boulders = props (PropSnap circles). Velocity by id-matched frame diff; a
    // recycle teleport (jump > MAX_PLAUSIBLE_STEP) clamps to zero.
    private fun fillBoulders(entities: List<EntitySnap>, advanced: Boolean, frameDt: Float) {
        var n = 0
        for (e in entities) {
            if (n >= view.boulderX.size) break
            if (e !is PropSnap || e.kind != ShapeKind.CIRCLE) continue
            view.boulderX[n] = e.x
            view.boulderY[n] = e.y
            if (advanced) {
                val px = prevBoulderX[e.id]
                val py = prevBoulderY[e.id]
                if (frameDt > 0f && px != null && py != null) {
                    val recycled = abs(e.x - px) > MAX_PLAUSIBLE_STEP || abs(e.y - py) > MAX_PLAUSIBLE_STEP
                    view.boulderVx[n] = if (recycled) 0f else (e.x - px) / frameDt
                    view.boulderVy[n] = if (recycled) 0f else (e.y - py) / frameDt
                } else {
                    view.boulderVx[n] = 0f
                    view.boulderVy[n] = 0f
                }
            }
            n++
        }
        view.boulderCount = n

        if (advanced) {
            prevBoulderX.clear()
            prevBoulderY.clear()
            for (e in entities) {
                if (e !is PropSnap || e.kind != ShapeKind.CIRCLE) continue
                prevBoulderX[e.id] = e.x
                prevBoulderY[e.id] = e.y
            }
        }
    }

    // Other players collide too, but they climb -- so track them by measured
    // velocity (not the boulder wall-bounce), keyed by slot (stable per frame).
    private fun fillOthers(entities: List<EntitySnap>, mySlot: Int, advanced: Boolean, frameDt: Float) {
        var n = 0
        for (e in entities) {
            if (n >= view.otherX.size) break
            if (e !is PlayerSnap || e.slot == mySlot) continue
            view.otherX[n] = e.x
            view.otherY[n] = e.y
            if (advanced) {
                val px = prevOtherX[e.slot]
                val py = prevOtherY[e.slot]
                if (frameDt > 0f && px != null && py != null) {
                    val recycled = abs(e.x - px) > MAX_PLAUSIBLE_STEP || abs(e.y - py) > MAX_PLAUSIBLE_STEP
                    view.otherVx[n] = if (recycled) 0f else (e.x - px) / frameDt
                    view.otherVy[n] = if (recycled) 0f else (e.y - py) / frameDt
                } else {
                    view.otherVx[n] = 0f
                    view.otherVy[n] = 0f
                }
            }
            n++
        }
        view.otherCount = n

        if (advanced) {
            prevOtherX.clear()
            prevOtherY.clear()
            for (e in entities) {
                if (e !is PlayerSnap || e.slot == mySlot) continue
                prevOtherX[e.slot] = e.x
                prevOtherY[e.slot] = e.y
            }
        }
    }

    // Resting on a solid top with ~0 vy: speed gate rejects the apex, surface gate
    // rejects free-fall over a hole.
    private fun inferGrounded(platforms: List<PlatformSnap>): Boolean {
        if (abs(self.vy) > GROUNDED_VY_BAND) return false
        val r = GameConfig.BALL_RADIUS
        val ballBottom = self.y - r
        if (ballBottom <= GameConfig.GROUND_HEIGHT + GROUNDED_GAP) return true
        for (p in platforms) {
            val top = p.y + GameConfig.PLATFORM_HEIGHT
            if (abs(ballBottom - top) > GROUNDED_GAP) continue
            val overHole = p.holeWidth > HOLE_EPS && self.x > p.holeX && self.x < p.holeX + p.holeWidth
            if (!overHole) return true
        }
        return false
    }

    private companion object {
        const val MAX_PLAUSIBLE_STEP = 0.5f // m/frame; past this = a recycle teleport
        const val HOLE_EPS = 0.02f
        const val GROUNDED_VY_BAND = 0.8f
        const val GROUNDED_GAP = 0.08f
    }
}
