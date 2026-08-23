package com.skooldev.shweep.screens

import kotlin.math.abs
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

    fun step(
        sheepList: List<SheepItem>,
        screenWidth: Float,
        screenHeight: Float,
        playAreaStartY: Float,
        sheepBaseSizePx: Float,
        targetScale: Float,
        deltaSeconds: Float
    ): List<SheepItem> {
        val dt = deltaSeconds.coerceIn(0f, MAX_DELTA_SECONDS)
        val result = ArrayList<SheepItem>(sheepList.size)

        // Movement and boundary reflection.
        for (sheep in sheepList) {
            val size = sheepBaseSizePx * targetScale

            var x = sheep.x + sheep.vx * dt
            var y = sheep.y + sheep.vy * dt
            var vx = sheep.vx
            var vy = sheep.vy

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

            result.add(
                sheep.copy(
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy,
                    scale = targetScale,
                    // Uses pre-collision velocity: overlap correction must not
                    // make resting sheep appear to step.
                    gaitPhaseRadians = SheepGait.advancePhase(
                        currentPhaseRadians = sheep.gaitPhaseRadians,
                        speedPxPerSecond = SheepGait.speed(sheep.vx, sheep.vy),
                        deltaSeconds = dt,
                        sheepBaseSizePx = sheepBaseSizePx
                    )
                )
            )
        }

        resolveCollisions(result, sheepBaseSizePx, screenWidth, screenHeight, playAreaStartY)

        return result
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
            val radiusA = baseSizePx * a.scale / 2f
            for (j in i + 1 until n) {
                val b = sheep[j]
                val radiusB = baseSizePx * b.scale / 2f

                val minDistance = radiusA + radiusB
                val dx = centerX(b, baseSizePx) - centerX(a, baseSizePx)
                val dy = centerY(b, baseSizePx) - centerY(a, baseSizePx)
                val distanceSquared = dx * dx + dy * dy
                if (distanceSquared >= minDistance * minDistance) continue

                val nx: Float
                val ny: Float
                val distance: Float
                if (distanceSquared <= 0f) {
                    // Identical centers: pick an arbitrary but stable normal.
                    nx = 1f
                    ny = 0f
                    distance = 0f
                } else {
                    distance = sqrt(distanceSquared)
                    nx = dx / distance
                    ny = dy / distance
                }

                // Separate the overlap symmetrically.
                val halfOverlap = (minDistance - distance) / 2f
                var movedA = a.copy(
                    x = a.x - nx * halfOverlap,
                    y = a.y - ny * halfOverlap
                )
                var movedB = b.copy(
                    x = b.x + nx * halfOverlap,
                    y = b.y + ny * halfOverlap
                )

                // Impulse only while approaching: relative normal velocity < 0.
                val relativeNormalVelocity =
                    (movedB.vx - movedA.vx) * nx + (movedB.vy - movedA.vy) * ny
                if (relativeNormalVelocity < 0f) {
                    val normalVelocityA = movedA.vx * nx + movedA.vy * ny
                    val normalVelocityB = movedB.vx * nx + movedB.vy * ny
                    // Equal masses exchange their normal components.
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
        val size = baseSizePx * sheep.scale
        return sheep.copy(
            x = sheep.x.coerceIn(0f, maxOf(0f, screenWidth - size)),
            y = sheep.y.coerceIn(playAreaStartY, maxOf(playAreaStartY, screenHeight - size))
        )
    }

    private fun centerX(sheep: SheepItem, baseSizePx: Float): Float =
        sheep.x + baseSizePx * sheep.scale / 2f

    private fun centerY(sheep: SheepItem, baseSizePx: Float): Float =
        sheep.y + baseSizePx * sheep.scale / 2f
}
