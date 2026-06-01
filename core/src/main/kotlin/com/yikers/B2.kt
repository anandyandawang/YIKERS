package com.yikers

// Box2D unit + step constants. Sim run in meters, render in pixels.
const val P2M = 0.01f          // pixels -> meters
const val M2P = 1f / P2M       // meters -> pixels
const val TIME_STEP = 1f / 300f
const val VELOCITY_ITERS = 6
const val POSITION_ITERS = 2
