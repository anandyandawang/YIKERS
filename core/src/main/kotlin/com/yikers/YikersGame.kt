package com.yikers

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Bare game. Move coral square with arrow keys.
 * No binary assets — draw shapes direct. Good seed to grow on.
 */
class YikersGame : ApplicationAdapter() {

    private lateinit var shapeRenderer: ShapeRenderer

    private var x = 0f
    private var y = 0f
    private val size = 64f
    private val speed = 300f // px per second

    override fun create() {
        shapeRenderer = ShapeRenderer()
        // start centered
        x = Gdx.graphics.width / 2f - size / 2f
        y = Gdx.graphics.height / 2f - size / 2f
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) x -= speed * dt
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) x += speed * dt
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) y -= speed * dt
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) y += speed * dt

        // keep square inside window
        x = x.coerceIn(0f, Gdx.graphics.width - size)
        y = y.coerceIn(0f, Gdx.graphics.height - size)

        Gdx.gl.glClearColor(0.10f, 0.12f, 0.16f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.CORAL
        shapeRenderer.rect(x, y, size, size)
        shapeRenderer.end()
    }

    override fun resize(width: Int, height: Int) {
        // re-clamp so square not stuck off-screen after shrink
        x = x.coerceIn(0f, width - size)
        y = y.coerceIn(0f, height - size)
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
