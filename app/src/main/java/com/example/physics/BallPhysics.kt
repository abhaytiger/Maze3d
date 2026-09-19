package com.example.physics

import com.example.model.MazeGem
import com.example.model.MazeHole
import com.example.model.MazeLevel
import com.example.model.SpeedBooster
import com.example.model.WallSegment
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Particle emitted during impacts, star pickups, or hole suction.
 */
data class VisualParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float = 1f,
    var size: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val maxLife: Float,
    var currentLife: Float = 0f
) {
    val isDead: Boolean get() = currentLife >= maxLife
}

/**
 * Event produced during physics step for haptic & audio feedback.
 */
sealed class PhysicsEvent {
    data class WallHit(val intensity: Float) : PhysicsEvent()
    data class StarCollected(val gemId: Int) : PhysicsEvent()
    data object FellInHole : PhysicsEvent()
    data object ReachedGoal : PhysicsEvent()
    data object SpeedBoosted : PhysicsEvent()
}

/**
 * High-precision, sub-stepped physics simulation for a tactile rolling metal ball.
 */
class BallPhysics(private var level: MazeLevel) {

    // Ball state (normalized 0..1 coordinates)
    var x: Float = level.startX
    var y: Float = level.startY
    var vx: Float = 0f
    var vy: Float = 0f

    // 3D Visual rolling angles (radians)
    var rollAngleX: Float = 0f
    var rollAngleY: Float = 0f

    // Falling state (when falling into a pit or goal)
    var isFalling: Boolean = false
    var isGoalReached: Boolean = false
    var fallProgress: Float = 0f // 0f -> 1f
    private var fallTargetHole: MazeHole? = null

    // Particle manager
    val particles = mutableListOf<VisualParticle>()

    // Accumulated events for current step
    val pendingEvents = mutableListOf<PhysicsEvent>()

    // Base acceleration scaling
    private val gravityScale = 0.0035f

    fun resetToStart() {
        x = level.startX
        y = level.startY
        vx = 0f
        vy = 0f
        isFalling = false
        isGoalReached = false
        fallProgress = 0f
        fallTargetHole = null
        particles.clear()
        pendingEvents.clear()
    }

    fun updateLevel(newLevel: MazeLevel) {
        level = newLevel
        resetToStart()
    }

    /**
     * Advance simulation by dt seconds with sub-stepping for tunneling prevention.
     */
    fun step(tiltX: Float, tiltY: Float, dt: Float = 0.016f) {
        pendingEvents.clear()

        // Update active particles
        updateParticles(dt)

        if (isFalling) {
            // Animate spiral drop into hole
            val hole = fallTargetHole ?: return
            fallProgress += dt * 2.2f
            val scale = max(0f, 1f - fallProgress)
            val spiralAngle = fallProgress * 14f
            val spiralDist = (1f - fallProgress) * 0.015f

            x = hole.x + cos(spiralAngle) * spiralDist
            y = hole.y + sin(spiralAngle) * spiralDist

            // Swirl particles into hole
            if (particles.size < 40 && fallProgress < 0.8f) {
                particles.add(
                    VisualParticle(
                        x = x + (Math.random().toFloat() - 0.5f) * 0.02f,
                        y = y + (Math.random().toFloat() - 0.5f) * 0.02f,
                        vx = (hole.x - x) * 2f,
                        vy = (hole.y - y) * 2f,
                        alpha = 0.8f,
                        size = 0.006f,
                        r = if (hole.isGoal) 0.2f else 0.8f,
                        g = if (hole.isGoal) 0.9f else 0.2f,
                        b = if (hole.isGoal) 0.5f else 0.2f,
                        maxLife = 0.3f
                    )
                )
            }

            if (fallProgress >= 1f) {
                if (hole.isGoal) {
                    isGoalReached = true
                    pendingEvents.add(PhysicsEvent.ReachedGoal)
                } else {
                    resetToStart()
                }
            }
            return
        }

        // Sub-step physics (4 sub-steps per frame ensures rock-solid wall collisions)
        val subSteps = 4
        val subDt = dt / subSteps
        val ax = tiltX * gravityScale
        val ay = tiltY * gravityScale

        val friction = level.theme.rollingFriction
        val restitution = level.theme.bounceRestitution
        val radius = level.ballRadius

        for (step in 0 until subSteps) {
            // Apply tilt acceleration
            vx += ax
            vy += ay

            // Apply friction
            vx *= friction
            vy *= friction

            // Integrate velocity to position
            x += vx
            y += vy

            // Update 3D roll angle
            val speed = hypot(vx, vy)
            if (speed > 0.00001f && radius > 0.0001f) {
                rollAngleX += (vx / radius)
                rollAngleY += (vy / radius)
            }

            // Check booster pads
            for (booster in level.speedBoosters) {
                if (x in booster.x..(booster.x + booster.width) &&
                    y in booster.y..(booster.y + booster.height)
                ) {
                    vx += booster.dirX * booster.force * 0.25f
                    vy += booster.dirY * booster.force * 0.25f
                    pendingEvents.add(PhysicsEvent.SpeedBoosted)
                }
            }

            // Check wall collisions
            var maxImpactSpeed = 0f
            for (wall in level.walls) {
                val impact = resolveWallCollision(wall, radius, restitution)
                if (impact > maxImpactSpeed) {
                    maxImpactSpeed = impact
                }
            }

            if (maxImpactSpeed > 0.002f) {
                pendingEvents.add(PhysicsEvent.WallHit((maxImpactSpeed * 250f).coerceIn(0.1f, 1.0f)))
                // Spawn sparks on hard collision
                spawnWallSparks(x, y, maxImpactSpeed)
            }

            // Check hole suction & falling
            val allHoles = level.hazardHoles + level.goalHole
            for (hole in allHoles) {
                val dx = hole.x - x
                val dy = hole.y - y
                val dist = hypot(dx, dy)

                if (dist < hole.suctionRadius) {
                    // Gravitational pull toward hole center
                    val pullIntensity = if (hole.isVortex) 0.0045f else 0.0022f
                    val pullFactor = (1f - (dist / hole.suctionRadius)) * pullIntensity
                    vx += (dx / dist) * pullFactor
                    vy += (dy / dist) * pullFactor

                    // Fall threshold: ball center passes into hole opening
                    val fallThreshold = hole.radius * 0.55f
                    if (dist < fallThreshold) {
                        isFalling = true
                        fallTargetHole = hole
                        fallProgress = 0f
                        vx = 0f
                        vy = 0f
                        pendingEvents.add(if (hole.isGoal) PhysicsEvent.ReachedGoal else PhysicsEvent.FellInHole)
                        break
                    }
                }
            }
            if (isFalling) break

            // Check gem collection
            for (gem in level.gems) {
                if (!gem.isCollected) {
                    val gDist = hypot(gem.x - x, gem.y - y)
                    if (gDist < radius + gem.radius) {
                        gem.isCollected = true
                        pendingEvents.add(PhysicsEvent.StarCollected(gem.id))
                        spawnGemBurst(gem.x, gem.y)
                    }
                }
            }
        }
    }

    /**
     * Resolves continuous circle vs segment collision.
     * Returns impact velocity along normal if collision occurred.
     */
    private fun resolveWallCollision(wall: WallSegment, radius: Float, restitution: Float): Float {
        val segX = wall.x2 - wall.x1
        val segY = wall.y2 - wall.y1
        val segLenSq = segX * segX + segY * segY
        if (segLenSq < 0.0000001f) return 0f

        // Projection of ball onto segment clamped to [0, 1]
        val t = ((x - wall.x1) * segX + (y - wall.y1) * segY) / segLenSq
        val clampedT = t.coerceIn(0f, 1f)

        // Closest point on line segment
        val closestX = wall.x1 + clampedT * segX
        val closestY = wall.y1 + clampedT * segY

        val dx = x - closestX
        val dy = y - closestY
        val dist = hypot(dx, dy)
        val minDist = radius + wall.thickness * 0.5f

        if (dist < minDist) {
            // Collision detected! Determine collision normal
            val nx: Float
            val ny: Float
            if (dist > 0.00001f) {
                nx = dx / dist
                ny = dy / dist
            } else {
                // Ball center is exactly on the wall, push along arbitrary normal
                nx = if (wall.isHorizontal) 0f else 1f
                ny = if (wall.isHorizontal) 1f else 0f
            }

            // Penetration depth
            val penetration = minDist - dist
            x += nx * penetration
            y += ny * penetration

            // Velocity along collision normal
            val normalVel = vx * nx + vy * ny
            if (normalVel < 0f) {
                // Bounce with restitution
                vx -= (1f + restitution) * normalVel * nx
                vy -= (1f + restitution) * normalVel * ny
                return abs(normalVel)
            }
        }
        return 0f
    }

    private fun spawnWallSparks(px: Float, py: Float, impact: Float) {
        val count = (impact * 1500f).toInt().coerceIn(3, 10)
        for (i in 0 until count) {
            val angle = Math.random().toFloat() * 6.28f
            val speed = 0.015f + Math.random().toFloat() * 0.025f
            particles.add(
                VisualParticle(
                    x = px,
                    y = py,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    alpha = 1f,
                    size = 0.005f + Math.random().toFloat() * 0.004f,
                    r = 1.0f,
                    g = 0.85f,
                    b = 0.4f,
                    maxLife = 0.25f + Math.random().toFloat() * 0.15f
                )
            )
        }
    }

    private fun spawnGemBurst(gx: Float, gy: Float) {
        for (i in 0 until 18) {
            val angle = (i / 18f) * 6.28f
            val speed = 0.02f + Math.random().toFloat() * 0.03f
            particles.add(
                VisualParticle(
                    x = gx,
                    y = gy,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    alpha = 1f,
                    size = 0.008f,
                    r = 0.98f,
                    g = 0.75f,
                    b = 0.15f,
                    maxLife = 0.45f
                )
            )
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.currentLife += dt
            if (p.isDead) {
                iter.remove()
            } else {
                p.x += p.vx * dt * 60f
                p.y += p.vy * dt * 60f
                p.alpha = max(0f, 1f - (p.currentLife / p.maxLife))
            }
        }
    }
}
