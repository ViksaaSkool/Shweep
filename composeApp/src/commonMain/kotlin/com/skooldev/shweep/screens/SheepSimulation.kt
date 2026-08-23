package com.skooldev.shweep.screens

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Deterministic, frame-rate independent sheep physics.
 *
 * Velocities are expressed in pixels per second. One [step] advances the
 * simulation by [deltaSeconds], which must be derived from a frame clock so
 * movement speed is identical on 60 Hz and 120 Hz displays.
 */
internal object SheepSimulation {

    /** Largest simulated time step in seconds; larger gaps are clamped to this. */
    const val MAX_DELTA_SECONDS = 0.05f

    /** Zig-zag turn angle in radians (~20 degrees). */
    const val ZIGZAG_ANGLE_RADIANS = (20.0 * PI / 180.0).toFloat()

    fun step(
        sheepList: List<SheepItem>,
        screenWidth: Float,
        screenHeight: Float,
        playAreaStartY: Float,
        sheepBaseSizePx: Float,
        deltaSeconds: Float
    ): List<SheepItem> {
        val dt = deltaSeconds.coerceIn(0f, MAX_DELTA_SECONDS)
        val result = ArrayList<SheepItem>(sheepList.size)

        for (sheep in sheepList) {
            result.add(
                if (sheep.isDriftingAway) {
                    stepDriftingSheep(sheep, screenWidth, screenHeight, playAreaStartY, sheepBaseSizePx, dt)
                } else {
                    stepActiveSheep(sheep, screenWidth, screenHeight, playAreaStartY, sheepBaseSizePx, dt)
                }
            )
        }

        resolveCollisions(result, sheepBaseSizePx, screenWidth, screenHeight, playAreaStartY)

        return result
    }

    private fun stepActiveSheep(
        sheep: SheepItem,
        screenWidth: Float,
        screenHeight: Float,
        playAreaStartY: Float,
        sheepBaseSizePx: Float,
        dt: Float
    ): SheepItem {
        val age = sheep.ageSeconds + dt

        if (age >= sheep.lifetimeSeconds) {
            return startDrifting(sheep.copy(ageSeconds = age), screenWidth, sheepBaseSizePx)
        }

        var vx = sheep.vx
        var vy = sheep.vy
        var nextTurn = sheep.nextZigzagTurnIn - dt

        if (nextTurn <= 0f) {
            val angle = ZIGZAG_ANGLE_RADIANS * sheep.zigzagDirection
            val cosA = cos(angle.toDouble()).toFloat()
            val sinA = sin(angle.toDouble()).toFloat()
            val newVx = vx * cosA - vy * sinA
            val newVy = vx * sinA + vy * cosA
            val speed = sqrt(vx * vx + vy * vy)
            val newSpeed = sqrt(newVx * newVx + newVy * newVy)
            if (newSpeed > 0f && speed > 0f) {
                vx = newVx * speed / newSpeed
                vy = newVy * speed / newSpeed
            }
            nextTurn = sheep.zigzagTurnInterval
        }

        val size = sheepBaseSizePx
        var x = sheep.x + vx * dt
        var y = sheep.y + vy * dt

        if (x <= 0f) {
            x = 0f
            vx = abs(vx)
        } else if (x >= screenWidth - size) {
            x = screenWidth - size
            vx = -abs(vx)
        }

        if (y <= playAreaStartY) {
            y = playAreaStartY
            vy = abs(vy)
        } else if (y >= screenHeight - size) {
            y = screenHeight - size
            vy = -abs(vy)
        }

        return sheep.copy(
            x = x,
            y = y,
            vx = vx,
            vy = vy,
            ageSeconds = age,
            zigzagDirection = -sheep.zigzagDirection,
            nextZigzagTurnIn = nextTurn,
            gaitPhaseRadians = SheepGait.advancePhase(
                currentPhaseRadians = sheep.gaitPhaseRadians,
                speedPxPerSecond = SheepGait.speed(vx, vy),
                deltaSeconds = dt,
                sheepBaseSizePx = sheepBaseSizePx
            )
        )
    }

    private fun stepDriftingSheep(
        sheep: SheepItem,
        screenWidth: Float,
        screenHeight: Float,
        playAreaStartY: Float,
        sheepBaseSizePx: Float,
        dt: Float
    ): SheepItem {
        val size = sheepBaseSizePx
        var x = sheep.x
        var y = sheep.y
        var vy = sheep.vy
        var driftSpeedX = sheep.driftSpeedX

        if (driftSpeedX == 0f) {
            driftSpeedX = if (sheep.x + size / 2f <= screenWidth / 2f) -500f else 500f
        }

        x += driftSpeedX * dt
        y += vy * dt

        if (y < playAreaStartY) {
            y = playAreaStartY
            vy = abs(vy)
        } else if (y > screenHeight - size) {
            y = screenHeight - size
            vy = -abs(vy)
        }

        return sheep.copy(
            x = x,
            y = y,
            vy = vy,
            driftSpeedX = driftSpeedX,
            gaitPhaseRadians = SheepGait.advancePhase(
                currentPhaseRadians = sheep.gaitPhaseRadians,
                speedPxPerSecond = SheepGait.speed(driftSpeedX, vy),
                deltaSeconds = dt,
                sheepBaseSizePx = sheepBaseSizePx
            )
        )
    }

    /**
     * Equal-mass elastic collision response along the contact normal.
     *
     * Only the velocity component along the normal is exchanged; tangential
     * motion is preserved so glancing hits deflect naturally instead of
     * swapping full velocity vectors. An impulse is applied only while the
     * sheep are approaching each other, which prevents repeated direction
     * flips for resting or separating overlaps.
     *
     * Positions are clamped back inside the play area after separation so
     * correction can never push a sheep through a wall.
     */
    private fun resolveCollisions(
        sheep: MutableList<SheepItem>,
        baseSizePx: Float,
        screenWidth: Float,
        screenHeight: Float,
        playAreaStartY: Float
    ) {
        val n = sheep.size
        for (i in 0 until n) {
            val a = sheep[i]
            if (a.isDriftingAway) continue
            val radiusA = baseSizePx / 2f
            for (j in i + 1 until n) {
                val b = sheep[j]
                if (b.isDriftingAway) continue
                val radiusB = baseSizePx / 2f

                val minDistance = radiusA + radiusB
                val dx = centerX(b, baseSizePx) - centerX(a, baseSizePx)
                val dy = centerY(b, baseSizePx) - centerY(a, baseSizePx)
                val distanceSquared = dx * dx + dy * dy
                if (distanceSquared >= minDistance * minDistance) continue

                val nx: Float
                val ny: Float
                val distance: Float
                if (distanceSquared <= 0f) {
                    nx = 1f
                    ny = 0f
                    distance = 0f
                } else {
                    distance = sqrt(distanceSquared)
                    nx = dx / distance
                    ny = dy / distance
                }

                val halfOverlap = (minDistance - distance) / 2f
                var movedA = a.copy(
                    x = a.x - nx * halfOverlap,
                    y = a.y - ny * halfOverlap
                )
                var movedB = b.copy(
                    x = b.x + nx * halfOverlap,
                    y = b.y + ny * halfOverlap
                )

                val relativeNormalVelocity =
                    (movedB.vx - movedA.vx) * nx + (movedB.vy - movedA.vy) * ny
                if (relativeNormalVelocity < 0f) {
                    val normalVelocityA = movedA.vx * nx + movedA.vy * ny
                    val normalVelocityB = movedB.vx * nx + movedB.vy * ny
                    val newNormalVelocityA = normalVelocityB
                    val newNormalVelocityB = normalVelocityA

                    movedA = movedA.copy(
                        vx = movedA.vx + (newNormalVelocityA - normalVelocityA) * nx,
                        vy = movedA.vy + (newNormalVelocityA - normalVelocityA) * ny
                    )
                    movedB = movedB.copy(
                        vx = movedB.vx + (newNormalVelocityB - normalVelocityB) * nx,
                        vy = movedB.vy + (newNormalVelocityB - normalVelocityB) * ny
                    )
                }

                sheep[i] = clampToPlayArea(movedA, baseSizePx, screenWidth, screenHeight, playAreaStartY)
                sheep[j] = clampToPlayArea(movedB, baseSizePx, screenWidth, screenHeight, playAreaStartY)
            }
        }
    }

    private fun clampToPlayArea(
        sheep: SheepItem,
        baseSizePx: Float,
        screenWidth: Float,
        screenHeight: Float,
        playAreaStartY: Float
    ): SheepItem {
        val size = baseSizePx
        return sheep.copy(
            x = sheep.x.coerceIn(0f, maxOf(0f, screenWidth - size)),
            y = sheep.y.coerceIn(playAreaStartY, maxOf(playAreaStartY, screenHeight - size))
        )
    }

    private fun centerX(sheep: SheepItem, baseSizePx: Float): Float =
        sheep.x + baseSizePx / 2f

    private fun centerY(sheep: SheepItem, baseSizePx: Float): Float =
        sheep.y + baseSizePx / 2f
}

internal fun startDrifting(sheep: SheepItem, screenWidth: Float, sheepBaseSizePx: Float): SheepItem {
    val centerX = sheep.x + sheepBaseSizePx / 2f
    val goLeft = centerX <= screenWidth / 2f
    return sheep.copy(
        isDriftingAway = true,
        driftSpeedX = if (goLeft) -500f else 500f
    )
}
