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
// a playerId on every player ball (and -1 on boulders), so identity is exact:
//   - self: the ball whose playerId == this client's slot
//   - boulders: the non-player circles (playerId < 0)
// Velocities (own vy, boulder vx/vy) are still derived by differencing consecutive
// frames, and grounded is inferred from ~0 vertical speed while resting on a solid
// top — the wire intentionally stays free of velocity/grounded state.
//
// Velocity is derived from how far things moved between DISTINCT snapshot ticks,
// divided by the sim time those ticks represent (tickDelta / SIM_HZ) — never by
// wall-clock. A client may pump faster or slower than the server broadcasts, so
// wall time between reads doesn't match sim time; re-reading the same frame would
// also self-difference to a spurious vy = 0. Keying off the authoritative tick makes
// velocity exact regardless of pump cadence, which the jump-arc math relies on.
class SnapshotPercept(private val runConfig: RunConfig) {
    val self = BotSelf()
    val view = BotView()

    private var lastTick = -1L
    private var haveSelf = false
    private var lastSelfY = 0f

    // Previous boulder centers keyed by stable entity id, for frame differencing.
    private val prevBoulderX = HashMap<Int, Float>()
    private val prevBoulderY = HashMap<Int, Float>()

    private val gravity = abs(GameConfig.GRAVITY * runConfig.gravityScale)

    // myId = this client's slot, used to find its own ball in the snapshot.
    fun update(snap: WorldSnapshot, myId: Int) {
        self.speed = runConfig.horizontalSpeed
        self.jumpVelocity = runConfig.jumpVelocity
        view.gravityPxS2 = gravity

        val advanced = snap.tick != lastTick
        // Sim seconds since the last distinct frame (>=1 tick), from the authoritative
        // tick counter — independent of how often this client pumps.
        val frameDt = if (lastTick < 0) 0f else (snap.tick - lastTick) / GameConfig.SIM_HZ.toFloat()

        locateSelf(snap, myId, advanced, frameDt)
        fillHoles(snap.platforms, self.y)
        view.distToKillLine = self.y - snap.scrollY
        fillBoulders(snap.entities, advanced, frameDt)
        self.grounded = inferGrounded(snap.platforms)

        if (advanced) lastTick = snap.tick
    }

    // Our own ball is the one tagged with our slot. Position updates every call;
    // vy only on a new frame, over the time since the last distinct frame.
    private fun locateSelf(snap: WorldSnapshot, myId: Int, advanced: Boolean, frameDt: Float) {
        val mine = snap.entities.firstOrNull { it.playerId == myId } ?: return  // not spawned yet
        self.x = mine.x
        self.y = mine.y
        if (advanced) {
            self.vy = if (haveSelf && frameDt > 0f) (mine.y - lastSelfY) / frameDt else 0f
            lastSelfY = mine.y
            haveSelf = true
        }
        // same frame: keep the last computed vy (don't divide a frame against itself)
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

    // Boulders = the non-player circles (playerId < 0). Positions update every call;
    // velocity only on a new frame, by differencing this frame's center against the
    // SAME boulder's last center (matched by stable entity id). A recycled boulder
    // teleports to a new platform; that jump is implausibly large, so clamp it to
    // zero rather than report a bogus spike. On a re-read of the same frame the prior
    // velocities are kept (no self-differencing).
    private fun fillBoulders(entities: List<EntitySnap>, advanced: Boolean, frameDt: Float) {
        var n = 0
        for (e in entities) {
            if (n >= view.boulderX.size) break
            if (e.kind != ShapeKind.CIRCLE || e.playerId >= 0) continue  // not a boulder
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
            // Remember this frame's centers (keyed by id) for the next diff.
            prevBoulderX.clear()
            prevBoulderY.clear()
            for (e in entities) {
                if (e.kind != ShapeKind.CIRCLE || e.playerId >= 0) continue
                prevBoulderX[e.id] = e.x
                prevBoulderY[e.id] = e.y
            }
        }
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
        // A boulder moves at most ~0.1m/frame; a recycle teleport jumps a whole
        // platform interval. Anything past this is a discontinuity, not motion.
        const val MAX_PLAUSIBLE_STEP = 0.5f // m, per frame
        const val HOLE_EPS = 0.02f          // holeWidth at/below this reads as solid
        const val GROUNDED_VY_BAND = 0.8f   // m/s; |vy| under this counts as "not climbing/falling"
        const val GROUNDED_GAP = 0.08f      // m; ball bottom this close to a top counts as resting
    }
}
