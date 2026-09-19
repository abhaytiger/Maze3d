package com.example.model

/**
 * Geometric segment representing a solid wall in the maze.
 * Positions are normalized coordinates [0f..1f] relative to maze board width & height.
 */
data class WallSegment(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val thickness: Float = 0.024f,
    val isOuterBorder: Boolean = false
) {
    val isHorizontal: Boolean = kotlin.math.abs(y1 - y2) < 0.0001f
    val length: Float = kotlin.math.hypot(x2 - x1, y2 - y1)
}

/**
 * A circular pit / hole in the maze floor.
 * If [isGoal] is true, falling in triggers level completion!
 * If false, it's a hazard pit that swallows the ball and restarts the attempt.
 */
data class MazeHole(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float = 0.038f,
    val isGoal: Boolean = false,
    val isVortex: Boolean = false, // Stronger suction pull
    val suctionRadiusMultiplier: Float = 1.9f
) {
    val suctionRadius: Float = radius * suctionRadiusMultiplier
}

/**
 * Collectible star / gem scattered in branching dead-ends.
 */
data class MazeGem(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float = 0.025f,
    var isCollected: Boolean = false
)

/**
 * Directional booster pad on the floor.
 */
data class SpeedBooster(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val dirX: Float,
    val dirY: Float,
    val force: Float = 0.015f
)

/**
 * Complete data model for a single maze level.
 */
data class MazeLevel(
    val levelNumber: Int,
    val theme: WorldTheme,
    val gridCols: Int,
    val gridRows: Int,
    val startX: Float,
    val startY: Float,
    val ballRadius: Float,
    val goalHole: MazeHole,
    val hazardHoles: List<MazeHole>,
    val walls: List<WallSegment>,
    val gems: List<MazeGem>,
    val speedBoosters: List<SpeedBooster>,
    val targetTimeSeconds: Float,
    val parTimeSeconds: Float
)
