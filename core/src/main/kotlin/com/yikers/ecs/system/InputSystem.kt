package com.yikers.ecs.system

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.yikers.config.RunConfig
import com.yikers.ecs.component.FootSensor
import com.yikers.ecs.component.Physics
import com.yikers.ecs.component.Player
import com.yikers.ecs.resource.RunState

// arrows = horizontal velocity (old accelerometer tilt feel). key/tap = jump,
// gated on foot contact. Keeps x-momentum on jump.
class InputSystem(
    private val cfg: RunConfig = inject(),
    private val runState: RunState = inject(),
) : IteratingSystem(family { all(Player, Physics, FootSensor) }) {
    override fun onTickEntity(entity: Entity) {
        if (runState.dead) return
        val body = entity[Physics].body
        val grounded = entity[FootSensor].contacts > 0

        val left = Gdx.input.isKeyPressed(Input.Keys.LEFT)
        val right = Gdx.input.isKeyPressed(Input.Keys.RIGHT)
        val vx = when {
            left && !right -> -cfg.horizontalSpeed
            right && !left -> cfg.horizontalSpeed
            else -> 0f
        }
        body.setLinearVelocity(vx, body.linearVelocity.y)

        val jump = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
            Gdx.input.justTouched()
        if (jump && grounded) {
            body.setLinearVelocity(body.linearVelocity.x, cfg.jumpVelocity)
        }
    }
}
