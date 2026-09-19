package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.MazeGem
import com.example.model.MazeHole
import com.example.model.MazeLevel
import com.example.model.SpeedBooster
import com.example.model.WallSegment
import com.example.model.WorldTheme
import com.example.physics.VisualParticle
import com.example.viewmodel.GameUiState
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D-perspective Canvas rendering the maze labyrinth, extruded walls, specular chrome ball,
 * trap holes, animated goal vortex, and particle effects.
 */
@Composable
fun LabyrinthCanvas(
    uiState: GameUiState,
    particles: List<VisualParticle>,
    onDragTilt: (Float, Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // 3D perspective rotation tied to tilt
    val rotX = (-uiState.tiltY * 11f).coerceIn(-15f, 15f)
    val rotY = (uiState.tiltX * 11f).coerceIn(-15f, 15f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .graphicsLayer {
                rotationX = rotX
                rotationY = rotY
                cameraDistance = 14f * density
                shadowElevation = 16f
                shape = RoundedCornerShape(18.dp)
                clip = true
            }
            .shadow(16.dp, RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val normX = ((offset.x / size.width) - 0.5f) * 2.2f
                        val normY = ((offset.y / size.height) - 0.5f) * 2.2f
                        onDragTilt(normX.coerceIn(-1f, 1f), normY.coerceIn(-1f, 1f), true)
                    },
                    onDragEnd = {
                        onDragTilt(0f, 0f, false)
                    },
                    onDragCancel = {
                        onDragTilt(0f, 0f, false)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val normX = ((change.position.x / size.width) - 0.5f) * 2.2f
                        val normY = ((change.position.y / size.height) - 0.5f) * 2.2f
                        onDragTilt(normX.coerceIn(-1f, 1f), normY.coerceIn(-1f, 1f), true)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val theme = uiState.theme
            val level = uiState.level

            // 1. Render Themed Board Floor
            drawBoardBackground(theme, canvasW, canvasH)

            // 2. Render Boosters
            drawSpeedBoosters(level.speedBoosters, canvasW, canvasH, theme)

            // 3. Render Hazard Holes & Goal Hole
            drawHoles(level.hazardHoles, level.goalHole, canvasW, canvasH, theme, uiState.elapsedTime)

            // 4. Render Collectible Gems / Stars
            drawGems(level.gems, canvasW, canvasH, uiState.elapsedTime)

            // 5. Render 3D Extruded Walls
            drawExtrudedWalls(level.walls, canvasW, canvasH, theme)

            // 6. Render Active Particles
            drawParticles(particles, canvasW, canvasH)

            // 7. Render 3D Specular Chrome Metal Ball
            drawChromeBall(
                uiState.ballX,
                uiState.ballY,
                uiState.ballRadius,
                canvasW,
                canvasH,
                theme,
                uiState.rollAngleX,
                uiState.rollAngleY,
                uiState.isFalling,
                uiState.fallProgress,
                uiState.tiltX,
                uiState.tiltY
            )

            // 8. Board Beveled Rim Frame
            drawBoardRim(theme, canvasW, canvasH)
        }
    }
}

/**
 * Draws the rich textured background for each world theme.
 */
private fun DrawScope.drawBoardBackground(theme: WorldTheme, w: Float, h: Float) {
    // Base gradient
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.boardBackground,
                theme.boardBorder
            ),
            center = Offset(w * 0.5f, h * 0.5f),
            radius = w * 0.8f
        ),
        size = size
    )

    // Thematic floor details
    when (theme) {
        WorldTheme.CLASSIC_TEAK -> {
            // Wood plank lines
            val plankCount = 12
            for (i in 1..plankCount) {
                val py = (h / plankCount) * i
                drawLine(
                    color = theme.floorLineColor,
                    start = Offset(0f, py),
                    end = Offset(w, py),
                    strokeWidth = 1.5f
                )
            }
        }
        WorldTheme.CYBER_NEON, WorldTheme.CYBER_BIOSPHERE -> {
            // High-tech digital grid
            val gridStep = 48f
            var x = 0f
            while (x < w) {
                drawLine(
                    color = theme.floorLineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = 0f
            while (y < h) {
                drawLine(
                    color = theme.floorLineColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }
        }
        WorldTheme.ANCIENT_TEMPLE -> {
            // Sandstone paver grid
            val stoneStep = 64f
            var x = stoneStep
            while (x < w) {
                drawLine(theme.floorLineColor, Offset(x, 0f), Offset(x, h), strokeWidth = 2f)
                x += stoneStep
            }
            var y = stoneStep
            while (y < h) {
                drawLine(theme.floorLineColor, Offset(0f, y), Offset(w, y), strokeWidth = 2f)
                y += stoneStep
            }
        }
        WorldTheme.DEEP_SPACE -> {
            // Twinkling cosmic stardust
            val starCount = 35
            for (i in 0 until starCount) {
                val sx = (Math.sin(i * 13.5).toFloat() * 0.5f + 0.5f) * w
                val sy = (Math.cos(i * 17.3).toFloat() * 0.5f + 0.5f) * h
                val sAlpha = (Math.sin(i * 4.1).toFloat() * 0.3f + 0.5f)
                drawCircle(
                    color = Color.White.copy(alpha = sAlpha),
                    radius = (i % 3 + 1.2f),
                    center = Offset(sx, sy)
                )
            }
        }
        WorldTheme.STEAMPUNK_FOUNDRY -> {
            // Riveted copper plates
            val plateW = w * 0.33f
            val plateH = h * 0.25f
            for (r in 0..3) {
                for (c in 0..2) {
                    val px = c * plateW
                    val py = r * plateH
                    drawRect(
                        color = Color.Black.copy(alpha = 0.25f),
                        topLeft = Offset(px, py),
                        size = Size(plateW, plateH),
                        style = Stroke(1.5f)
                    )
                    // Rivet dots at corners
                    drawCircle(Color(0xFFB45309), 3f, Offset(px + 8f, py + 8f))
                }
            }
        }
        WorldTheme.GLACIAL_ICE -> {
            // Ice crystal cracks
            drawLine(Color(0x30FFFFFF), Offset(w * 0.2f, 0f), Offset(w * 0.45f, h * 0.5f), 1.5f)
            drawLine(Color(0x30FFFFFF), Offset(w * 0.45f, h * 0.5f), Offset(w * 0.8f, h), 1.5f)
            drawLine(Color(0x30FFFFFF), Offset(w * 0.45f, h * 0.5f), Offset(w * 0.1f, h * 0.8f), 1.2f)
        }
        WorldTheme.VOLCANIC_CORE -> {
            // Magma fissures beneath cracks
            drawLine(Color(0x44EF4444), Offset(w * 0.15f, h * 0.1f), Offset(w * 0.85f, h * 0.9f), 3f)
            drawLine(Color(0x55F59E0B), Offset(w * 0.85f, h * 0.2f), Offset(w * 0.2f, h * 0.8f), 2.5f)
        }
        WorldTheme.ZEN_GARDEN -> {
            // Concentric zen sand ripple rings
            for (r in 1..5) {
                drawCircle(
                    color = theme.floorLineColor,
                    radius = r * 55f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(1.5f)
                )
            }
        }
        WorldTheme.CELESTIAL_OLYMPUS -> {
            // Divine marble gold veins
            drawLine(Color(0x30FDE68A), Offset(w * 0.1f, 0f), Offset(w * 0.9f, h), 2f)
            drawLine(Color(0x25F59E0B), Offset(0f, h * 0.35f), Offset(w, h * 0.65f), 1.5f)
        }
    }
}

/**
 * Renders speed boost pads with animated arrows.
 */
private fun DrawScope.drawSpeedBoosters(
    boosters: List<SpeedBooster>,
    w: Float,
    h: Float,
    theme: WorldTheme
) {
    for (booster in boosters) {
        val bx = booster.x * w
        val by = booster.y * h
        val bw = booster.width * w
        val bh = booster.height * h

        // Glow plate
        drawRoundRect(
            color = theme.wallColor.copy(alpha = 0.35f),
            topLeft = Offset(bx, by),
            size = Size(bw, bh),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = theme.wallBevelColor.copy(alpha = 0.8f),
            topLeft = Offset(bx, by),
            size = Size(bw, bh),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            style = Stroke(2f)
        )

        // Direction chevron
        val cx = bx + bw * 0.5f
        val cy = by + bh * 0.5f
        val arrowDir = booster.dirY
        val path = Path().apply {
            moveTo(cx - bw * 0.25f, cy - arrowDir * bh * 0.15f)
            lineTo(cx, cy + arrowDir * bh * 0.2f)
            lineTo(cx + bw * 0.25f, cy - arrowDir * bh * 0.15f)
        }
        drawPath(
            path = path,
            color = theme.wallBevelColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Draws hazard holes and the animated goal vortex.
 */
private fun DrawScope.drawHoles(
    hazardHoles: List<MazeHole>,
    goalHole: MazeHole,
    w: Float,
    h: Float,
    theme: WorldTheme,
    elapsedTime: Float
) {
    // Hazard holes
    for (hole in hazardHoles) {
        val cx = hole.x * w
        val cy = hole.y * h
        val r = hole.radius * w

        // Suction aura if vortex
        if (hole.isVortex) {
            val suctionR = hole.suctionRadius * w
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, theme.holeRimColor.copy(alpha = 0.3f)),
                    center = Offset(cx, cy),
                    radius = suctionR
                ),
                radius = suctionR,
                center = Offset(cx, cy)
            )
        }

        // Deep dark hole interior
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black, theme.holeColor),
                center = Offset(cx, cy),
                radius = r
            ),
            radius = r,
            center = Offset(cx, cy)
        )

        // Outer rim border with shadow
        drawCircle(
            color = theme.holeRimColor,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 3.5f)
        )
    }

    // Goal Hole (Animated glowing portal vortex)
    val gcx = goalHole.x * w
    val gcy = goalHole.y * h
    val gr = goalHole.radius * w

    // Outer pulsating halo
    val pulse = (sin(elapsedTime * 4.5f) * 0.15f + 0.85f)
    drawCircle(
        color = theme.goalGlowColor.copy(alpha = 0.45f * pulse),
        radius = gr * 1.55f * pulse,
        center = Offset(gcx, gcy)
    )

    // Portal body
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(theme.goalGlowColor, theme.goalColor, theme.holeColor),
            center = Offset(gcx, gcy),
            radius = gr
        ),
        radius = gr,
        center = Offset(gcx, gcy)
    )

    // Rotating energy rings inside goal
    rotate(degrees = elapsedTime * 85f, pivot = Offset(gcx, gcy)) {
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = gr * 0.65f,
            center = Offset(gcx, gcy),
            style = Stroke(width = 2.5f)
        )
        // Diamond core
        val path = Path().apply {
            moveTo(gcx, gcy - gr * 0.4f)
            lineTo(gcx + gr * 0.4f, gcy)
            lineTo(gcx, gcy + gr * 0.4f)
            lineTo(gcx - gr * 0.4f, gcy)
            close()
        }
        drawPath(path, Color.White, style = Fill)
    }
}

/**
 * Draws collectible stars/gems with sparkling rotation.
 */
private fun DrawScope.drawGems(gems: List<MazeGem>, w: Float, h: Float, elapsedTime: Float) {
    for (gem in gems) {
        if (gem.isCollected) continue

        val cx = gem.x * w
        val cy = gem.y * h
        val r = gem.radius * w
        val floatOffset = sin(elapsedTime * 3.5f + gem.id) * 3f
        val actualCenter = Offset(cx, cy + floatOffset)

        // Glowing backdrop
        drawCircle(
            color = Color(0x66FBBF24),
            radius = r * 1.4f,
            center = actualCenter
        )

        // 3D Gem diamond shape
        rotate(degrees = elapsedTime * 45f + gem.id * 60f, pivot = actualCenter) {
            val path = Path().apply {
                moveTo(actualCenter.x, actualCenter.y - r)
                lineTo(actualCenter.x + r * 0.85f, actualCenter.y)
                lineTo(actualCenter.x, actualCenter.y + r)
                lineTo(actualCenter.x - r * 0.85f, actualCenter.y)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFFBEB), Color(0xFFF59E0B), Color(0xFFD97706)),
                    start = Offset(actualCenter.x - r, actualCenter.y - r),
                    end = Offset(actualCenter.x + r, actualCenter.y + r)
                )
            )
            // Specular glint
            drawCircle(Color.White, radius = r * 0.25f, center = Offset(actualCenter.x - r * 0.25f, actualCenter.y - r * 0.25f))
        }
    }
}

/**
 * Draws 3D extruded walls with drop shadow, face color, and beveled top highlight.
 */
private fun DrawScope.drawExtrudedWalls(
    walls: List<WallSegment>,
    w: Float,
    h: Float,
    theme: WorldTheme
) {
    val wallHeightOffset = 5.5f // 3D extrusion height

    for (wall in walls) {
        val x1 = wall.x1 * w
        val y1 = wall.y1 * h
        val x2 = wall.x2 * w
        val y2 = wall.y2 * h
        val thick = wall.thickness * w

        // 1. Cast shadow onto floor (offset slightly down and right)
        drawLine(
            color = theme.wallShadowColor,
            start = Offset(x1 + 3f, y1 + 5f),
            end = Offset(x2 + 3f, y2 + 5f),
            strokeWidth = thick * 1.2f,
            cap = StrokeCap.Round
        )

        // 2. Extruded wall body (darker base)
        drawLine(
            color = theme.wallColor,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = thick,
            cap = StrokeCap.Round
        )

        // 3. Beveled top highlight (raised towards directional light)
        drawLine(
            color = theme.wallBevelColor,
            start = Offset(x1, y1 - wallHeightOffset),
            end = Offset(x2, y2 - wallHeightOffset),
            strokeWidth = thick * 0.55f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Draws sparks, dust, and aura particles.
 */
private fun DrawScope.drawParticles(particles: List<VisualParticle>, w: Float, h: Float) {
    for (p in particles) {
        val px = p.x * w
        val py = p.y * h
        val pr = p.size * w
        val color = Color(p.r, p.g, p.b, p.alpha)
        drawCircle(color = color, radius = pr, center = Offset(px, py))
    }
}

/**
 * Renders the tactile, heavy 3D Chrome Metal Ball with dynamic specular highlight,
 * cast shadow, and realistic surface reflection.
 */
private fun DrawScope.drawChromeBall(
    bx: Float,
    by: Float,
    radiusNorm: Float,
    w: Float,
    h: Float,
    theme: WorldTheme,
    rollAngleX: Float,
    rollAngleY: Float,
    isFalling: Boolean,
    fallProgress: Float,
    tiltX: Float,
    tiltY: Float
) {
    val cx = bx * w
    val cy = by * h
    val scale = if (isFalling) (1f - fallProgress).coerceIn(0f, 1f) else 1f
    val r = radiusNorm * w * scale
    if (r <= 0.5f) return

    // 1. Ball Drop Shadow (shifts dynamically with tilt and light source)
    val shadowDist = 8f * scale
    val shadowOffset = Offset(
        cx + (shadowDist * 0.8f - tiltX * 6f),
        cy + (shadowDist * 1.1f - tiltY * 6f)
    )
    drawCircle(
        color = Color(0x60000000),
        radius = r * 1.05f,
        center = shadowOffset
    )

    // 2. Base 3D Chrome Sphere with Radial Gradient
    // Deep metallic rim (Fresnel reflection) transitioning to bright brushed metal
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                theme.ballTint,
                Color(0xFF94A3B8),
                Color(0xFF475569),
                Color(0xFF1E293B)
            ),
            center = Offset(cx - r * 0.3f, cy - r * 0.35f),
            radius = r * 1.25f
        ),
        radius = r,
        center = Offset(cx, cy)
    )

    // 3. Dynamic Specular Hotspot (reacts opposite to tilt and light direction)
    val specX = cx - r * 0.38f - (tiltX * r * 0.25f)
    val specY = cy - r * 0.42f - (tiltY * r * 0.25f)
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = r * 0.28f,
        center = Offset(specX, specY)
    )
    drawCircle(
        color = Color.White,
        radius = r * 0.12f,
        center = Offset(specX - r * 0.05f, specY - r * 0.05f)
    )

    // 4. Subtle rolling texture stripe to visualize the ball physically rolling!
    val rollStripOffsetX = (sin(rollAngleX) * r * 0.45f)
    val rollStripOffsetY = (sin(rollAngleY) * r * 0.45f)
    drawCircle(
        color = Color.White.copy(alpha = 0.25f),
        radius = r * 0.22f,
        center = Offset(cx + rollStripOffsetX, cy + rollStripOffsetY)
    )

    // 5. Outer subtle ambient rim
    drawCircle(
        color = Color.White.copy(alpha = 0.35f),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(1.5f)
    )
}

/**
 * Draws the beveled outer border frame around the board.
 */
private fun DrawScope.drawBoardRim(theme: WorldTheme, w: Float, h: Float) {
    // Inner shadow vignette along outer boundary
    drawRect(
        color = theme.boardBorder.copy(alpha = 0.85f),
        topLeft = Offset.Zero,
        size = Size(w, h),
        style = Stroke(8f)
    )
    // Beveled rim top highlight
    drawRect(
        color = Color.White.copy(alpha = 0.15f),
        topLeft = Offset(4f, 4f),
        size = Size(w - 8f, h - 8f),
        style = Stroke(2f)
    )
}
