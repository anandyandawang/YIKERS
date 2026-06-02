package com.yikers.physics

import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.Manifold
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.yikers.ecs.UD_BALL
import com.yikers.ecs.UD_FOOT
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Lethal
import com.yikers.ecs.resource.RunState

// foot <-> solid => grounded count. ball <-> lethal => death flag.
// No world/body mutation here — only counters/flags. Systems act next tick.
class PlayContactListener(
    private val world: World,
    private val runState: RunState,
) : ContactListener {

    override fun beginContact(contact: Contact) = handle(contact, begin = true)
    override fun endContact(contact: Contact) = handle(contact, begin = false)
    override fun preSolve(contact: Contact, oldManifold: Manifold) = Unit
    override fun postSolve(contact: Contact, impulse: ContactImpulse) = Unit

    private fun handle(contact: Contact, begin: Boolean) {
        val a = contact.fixtureA
        val b = contact.fixtureB

        val foot = footOf(a, b)
        if (foot != null) {
            val other = if (foot === a) b else a
            if (!other.isSensor) {
                val player = foot.body.userData as? Entity ?: return
                with(world) {
                    val fs = player[FootSensor]
                    fs.contacts = (fs.contacts + if (begin) 1 else -1).coerceAtLeast(0)
                }
            }
            return
        }

        if (begin) {
            val ball = ballOf(a, b)
            if (ball != null) {
                val other = if (ball === a) b else a
                val otherEntity = other.body.userData as? Entity ?: return
                if (isLethal(otherEntity)) {
                    val ballEntity = ball.body.userData as? Entity ?: return
                    runState.lethalHits.add(ballEntity.id)
                }
            }
        }
    }

    private fun footOf(a: Fixture, b: Fixture): Fixture? = when {
        a.userData == UD_FOOT -> a
        b.userData == UD_FOOT -> b
        else -> null
    }

    private fun ballOf(a: Fixture, b: Fixture): Fixture? = when {
        a.userData == UD_BALL -> a
        b.userData == UD_BALL -> b
        else -> null
    }

    private fun isLethal(e: Entity): Boolean = with(world) { e.getOrNull(Lethal) != null }
}
