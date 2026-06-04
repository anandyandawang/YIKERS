package com.yikers.ecs.resource

import com.badlogic.gdx.physics.box2d.Body

// Static world bodies (not entities). Walls follow the scroll line.
class Arena(
    val ground: Body,
    val leftWall: Body,
    val rightWall: Body,
)
