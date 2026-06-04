package com.yikers.bot

import com.yikers.config.GameConfig
import com.yikers.config.RunConfig
import com.yikers.net.EntitySnap
import com.yikers.net.PlatformSnap
import com.yikers.net.ShapeKind
import com.yikers.net.WorldSnapshot
import kotlin.math.abs

// Rebuilds a bot's BotSelf + BotView from successive WorldSnapshots — the
// client-side analog of the server's old ControlSystem.fillView. The wire carries
// no entity ids, no velocities, and balls + boulders are both CIRCLEs, so this
// reconstructs everything approximately (per the "derive client-side" design):
//   - players vs boulders: by circle size (ball 2*BALL_RADIUS, boulder 2*BOULDER_RADIUS)
//   - self among the player balls: by position continuity (seeded by the slot's lane)
//   - velocities (own vy, boulder vx/vy): by differencing consecutive frames
//   - grounded: inferred from ~0 vertical speed while resting on a solid top
// Approximate by design; can mislock when two balls overlap. The minimal hardening
// (a slot id on the wire) is deliberately deferred.
class SnapshotPercept(private val runConfig: RunConfig) {
    val self = BotSelf()
    val view = BotView()

    private var haveSelf = false
    private var lastSelfX = 0f
    private var lastSelfY = 0f

    // Previous boulder centers, for nearest-match frame differencing.
    private var prevBoulderX = FloatArray(0)
    private var prevBoulderY = FloatArray(0)
    private var prevBoulderN = 0

    private val ballSize = GameConfig.BALL_RADIUS * 2f
    private val boulderSize = GameConfig.BOULDER_RADIUS * 2f
    private val sizeTol = GameConfig.BALL_RADIUS * 0.5f      // 0.12m: < half the ball/boulder gap
    private val gravity = abs(GameConfig.GRAVITY * runConfig.gravityScale)

    // spawnHintX = laneX(slot): where the server placed this bot's ball, used only
    // to lock onto self in the very first frame before continuity takes over.
    fun update(snap: WorldSnapshot, dt: Float, spawnHintX: Float) {
        self.speed = runConfig.horizontalSpeed
        self.jumpVelocity = runConfig.jumpVelocity
        view.gravityPxS2 = gravity

        locateSelf(snap, dt, spawnHintX)
        fillHoles(snap.platforms, self.y)
        view.distToKillLine = self.y - snap.scrollY
        fillBoulders(snap.entities, dt)
        self.grounded = inferGrounded(snap.platforms)
    }

    // Pick the player-sized circle nearest the anchor (last known self, or the
    // spawn lane on the first lock). Update own vy by differencing.
    private fun locateSelf(snap: WorldSnapshot, dt: Float, spawnHintX: Float) {
        val anchorX = if (haveSelf) lastSelfX else spawnHintX
        val useY = haveSelf
        var best: EntitySnap? = null
        var bestD = Float.MAX_VALUE
        for (e in snap.entities) {
            if (e.kind != ShapeKind.CIRCLE) continue
            if (abs(e.sizeX - ballSize) > sizeTol) continue      // not a player ball
            val dx = e.x - anchorX
            val dy = if (useY) e.y - lastSelfY else 0f
            val d = dx * dx + dy * dy
            if (d < bestD) { bestD = d; best = e }
        }
        if (best == null) return                                 // no ball yet (not spawned)
        self.x = best.x
        self.y = best.y
        self.vy = if (haveSelf && dt > 0f) (best.y - lastSelfY) / dt else 0f
        lastSelfX = best.x
        lastSelfY = best.y
        haveSelf = true
    }

    // Two lowest holes above the ball + the support slab below it. Mirror of the
    // server's old fillView, reading PlatformSnap. holeWidth ~ 0 (a bridged/eased
    // slab) reads as solid, so the bot treats it as having no hole.
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

    // Boulders = boulder-sized circles. Velocity by nearest-match against the
    // previous frame's centers (no ids on the wire).
    private fun fillBoulders(entities: List<EntitySnap>, dt: Float) {
        var n = 0
        for (e in entities) {
            if (n >= view.boulderX.size) break
            if (e.kind != ShapeKind.CIRCLE) continue
            if (abs(e.sizeX - boulderSize) > sizeTol) continue   // not a boulder
            view.boulderX[n] = e.x
            view.boulderY[n] = e.y
            if (dt > 0f && prevBoulderN > 0) {
                var bestD = Float.MAX_VALUE
                var px = e.x
                var py = e.y
                for (j in 0 until prevBoulderN) {
                    val dx = e.x - prevBoulderX[j]
                    val dy = e.y - prevBoulderY[j]
                    val d = dx * dx + dy * dy
                    if (d < bestD) { bestD = d; px = prevBoulderX[j]; py = prevBoulderY[j] }
                }
                view.boulderVx[n] = (e.x - px) / dt
                view.boulderVy[n] = (e.y - py) / dt
            } else {
                view.boulderVx[n] = 0f
                view.boulderVy[n] = 0f
            }
            n++
        }
        view.boulderCount = n

        // Remember this frame's centers for the next diff.
        if (prevBoulderX.size < n) {
            prevBoulderX = FloatArray(n)
            prevBoulderY = FloatArray(n)
        }
        for (i in 0 until n) {
            prevBoulderX[i] = view.boulderX[i]
            prevBoulderY[i] = view.boulderY[i]
        }
        prevBoulderN = n
    }

    // Resting on a solid top (ground or a slab's non-hole half) with ~0 vertical
    // speed. The vertical-speed gate rejects the airborne apex (vy ~ 0 but no
    // surface beneath); the surface gate rejects free-fall over a hole.
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
        const val HOLE_EPS = 0.02f          // holeWidth at/below this reads as solid
        const val GROUNDED_VY_BAND = 0.8f   // m/s; |vy| under this counts as "not climbing/falling"
        const val GROUNDED_GAP = 0.08f      // m; ball bottom this close to a top counts as resting
    }
}
