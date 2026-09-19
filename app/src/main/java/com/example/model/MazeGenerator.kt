package com.example.model

import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic procedural generator that creates 100 uniquely crafted, solvable labyrinths.
 * Difficulty scales smoothly from accessible early tutorials to intricate master challenges.
 */
object MazeGenerator {

    fun generate(levelNumber: Int): MazeLevel {
        val level = levelNumber.coerceIn(1, 100)
        val theme = WorldTheme.forLevel(level)

        // Seeded RNG ensures every level is identical on each replay
        val seed = (level * 104729L) xor 0x5DEECE66DL
        val rng = Random(seed)

        // Progressive grid dimensions
        val cols = when {
            level <= 5 -> 6
            level <= 10 -> 7
            level <= 20 -> 8
            level <= 35 -> 9
            level <= 50 -> 10
            level <= 65 -> 11
            level <= 80 -> 12
            level <= 90 -> 13
            else -> 14
        }
        val rows = when {
            level <= 5 -> 8
            level <= 10 -> 9
            level <= 20 -> 11
            level <= 35 -> 12
            level <= 50 -> 14
            level <= 65 -> 15
            level <= 80 -> 16
            level <= 90 -> 18
            else -> 20
        }

        // Cell dimensions in normalized 0..1 space
        val marginX = 0.04f
        val marginY = 0.04f
        val playWidth = 1f - 2f * marginX
        val playHeight = 1f - 2f * marginY
        val cellW = playWidth / cols
        val cellH = playHeight / rows

        // Ball radius sized comfortably to roll through passages
        val passageMinDim = min(cellW, cellH)
        val ballRadius = passageMinDim * 0.28f

        // Cell representation: horizontal and vertical walls
        // horizWalls[r][c] = wall between (r, c) and (r+1, c) for r in 0 until rows - 1
        val horizWalls = Array(rows - 1) { BooleanArray(cols) { true } }
        // vertWalls[r][c] = wall between (r, c) and (r, c+1) for c in 0 until cols - 1
        val vertWalls = Array(rows) { BooleanArray(cols - 1) { true } }

        // Recursive backtracker maze generation
        val visited = Array(rows) { BooleanArray(cols) { false } }
        val stack = ArrayDeque<Pair<Int, Int>>()

        // Start cell (top-left or near top)
        val startR = 0
        val startC = 0
        visited[startR][startC] = true
        stack.addLast(startR to startC)

        val directions = arrayOf(
            Pair(-1, 0), // Up
            Pair(1, 0),  // Down
            Pair(0, -1), // Left
            Pair(0, 1)   // Right
        )

        while (stack.isNotEmpty()) {
            val (r, c) = stack.last()
            // Find unvisited neighbors
            val neighbors = mutableListOf<Triple<Int, Int, Int>>() // r, c, dirIdx
            for (i in directions.indices) {
                val nr = r + directions[i].first
                val nc = c + directions[i].second
                if (nr in 0 until rows && nc in 0 until cols && !visited[nr][nc]) {
                    neighbors.add(Triple(nr, nc, i))
                }
            }

            if (neighbors.isNotEmpty()) {
                val chosen = neighbors[rng.nextInt(neighbors.size)]
                val nr = chosen.first
                val nc = chosen.second
                val dir = chosen.third

                // Tear down wall
                when (dir) {
                    0 -> horizWalls[nr][nc] = false // Up: wall between nr and r
                    1 -> horizWalls[r][c] = false   // Down: wall between r and nr
                    2 -> vertWalls[nr][nc] = false  // Left: wall between nc and c
                    3 -> vertWalls[r][c] = false    // Right: wall between c and nc
                }

                visited[nr][nc] = true
                stack.addLast(nr to nc)
            } else {
                stack.removeLast()
            }
        }

        // Add 6% to 15% open loops so player has pathing choices & tactical shortcuts
        val loopChance = 0.08f + (level / 100f) * 0.07f
        for (r in 0 until rows - 1) {
            for (c in 0 until cols) {
                if (horizWalls[r][c] && rng.nextFloat() < loopChance) {
                    horizWalls[r][c] = false
                }
            }
        }
        for (r in 0 until rows) {
            for (c in 0 until cols - 1) {
                if (vertWalls[r][c] && rng.nextFloat() < loopChance) {
                    vertWalls[r][c] = false
                }
            }
        }

        // Build list of wall line segments
        val wallSegments = mutableListOf<WallSegment>()
        val wallThickness = passageMinDim * 0.12f

        // Outer border walls
        wallSegments.add(WallSegment(marginX, marginY, marginX + playWidth, marginY, wallThickness * 1.5f, true))
        wallSegments.add(WallSegment(marginX + playWidth, marginY, marginX + playWidth, marginY + playHeight, wallThickness * 1.5f, true))
        wallSegments.add(WallSegment(marginX + playWidth, marginY + playHeight, marginX, marginY + playHeight, wallThickness * 1.5f, true))
        wallSegments.add(WallSegment(marginX, marginY + playHeight, marginX, marginY, wallThickness * 1.5f, true))

        // Convert horizontal inner walls (combining adjacent col walls into continuous segments)
        for (r in 0 until rows - 1) {
            val y = marginY + (r + 1) * cellH
            var startCol = -1
            for (c in 0 until cols) {
                if (horizWalls[r][c]) {
                    if (startCol == -1) startCol = c
                } else {
                    if (startCol != -1) {
                        val x1 = marginX + startCol * cellW
                        val x2 = marginX + c * cellW
                        wallSegments.add(WallSegment(x1, y, x2, y, wallThickness))
                        startCol = -1
                    }
                }
            }
            if (startCol != -1) {
                val x1 = marginX + startCol * cellW
                val x2 = marginX + cols * cellW
                wallSegments.add(WallSegment(x1, y, x2, y, wallThickness))
            }
        }

        // Convert vertical inner walls (combining adjacent row walls)
        for (c in 0 until cols - 1) {
            val x = marginX + (c + 1) * cellW
            var startRow = -1
            for (r in 0 until rows) {
                if (vertWalls[r][c]) {
                    if (startRow == -1) startRow = r
                } else {
                    if (startRow != -1) {
                        val y1 = marginY + startRow * cellH
                        val y2 = marginY + r * cellH
                        wallSegments.add(WallSegment(x, y1, x, y2, wallThickness))
                        startRow = -1
                    }
                }
            }
            if (startRow != -1) {
                val y1 = marginY + startRow * cellH
                val y2 = marginY + rows * cellH
                wallSegments.add(WallSegment(x, y1, x, y2, wallThickness))
            }
        }

        // Goal position (bottom-right cell or bottom center)
        val goalR = rows - 1
        val goalC = cols - 1
        val goalX = marginX + (goalC + 0.5f) * cellW
        val goalY = marginY + (goalR + 0.5f) * cellH
        val goalRadius = passageMinDim * 0.36f
        val goalHole = MazeHole(
            id = 999,
            x = goalX,
            y = goalY,
            radius = goalRadius,
            isGoal = true,
            suctionRadiusMultiplier = 1.6f
        )

        // Ball start position (cell center)
        val startX = marginX + (startC + 0.5f) * cellW
        val startY = marginY + (startR + 0.5f) * cellH

        // Hazard holes count scales from 2 (lvl 1) to 15 (lvl 100)
        val hazardCount = (2 + (level * 13) / 100).coerceIn(2, 16)
        val hazardHoles = mutableListOf<MazeHole>()

        // Candidate cells for holes: dead-ends and interior corridors away from start and goal
        val candidateCells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                // Must not be start or goal cell or immediate adjacent
                val distFromStart = kotlin.math.abs(r - startR) + kotlin.math.abs(c - startC)
                val distFromGoal = kotlin.math.abs(r - goalR) + kotlin.math.abs(c - goalC)
                if (distFromStart > 2 && distFromGoal > 1) {
                    candidateCells.add(r to c)
                }
            }
        }
        candidateCells.shuffle(rng)

        val holeRadius = passageMinDim * 0.30f
        val isCosmicVortex = theme == WorldTheme.DEEP_SPACE || theme == WorldTheme.CYBER_BIOSPHERE

        val selectedHoleCells = mutableSetOf<Pair<Int, Int>>()
        for (i in 0 until min(hazardCount, candidateCells.size)) {
            val cell = candidateCells[i]
            selectedHoleCells.add(cell)
            val hx = marginX + (cell.second + 0.5f) * cellW
            val hy = marginY + (cell.first + 0.5f) * cellH
            hazardHoles.add(
                MazeHole(
                    id = i + 1,
                    x = hx,
                    y = hy,
                    radius = holeRadius,
                    isGoal = false,
                    isVortex = isCosmicVortex && (i % 2 == 0),
                    suctionRadiusMultiplier = if (isCosmicVortex) 2.2f else 1.8f
                )
            )
        }

        // Place 3 Bonus Gems in non-hole cells
        val gems = mutableListOf<MazeGem>()
        val gemCandidates = candidateCells.filter { it !in selectedHoleCells }
        val gemRadius = passageMinDim * 0.22f
        for (i in 0 until min(3, gemCandidates.size)) {
            val cell = gemCandidates[i]
            val gx = marginX + (cell.second + 0.5f) * cellW
            val gy = marginY + (cell.first + 0.5f) * cellH
            gems.add(MazeGem(id = i, x = gx, y = gy, radius = gemRadius))
        }

        // Speed boosters for Cyber and Steampunk themes
        val speedBoosters = mutableListOf<SpeedBooster>()
        if (theme == WorldTheme.CYBER_NEON || theme == WorldTheme.STEAMPUNK_FOUNDRY) {
            val boosterCells = gemCandidates.drop(3).take(2)
            for (cell in boosterCells) {
                val bx = marginX + (cell.second + 0.2f) * cellW
                val by = marginY + (cell.first + 0.2f) * cellH
                val bw = cellW * 0.6f
                val bh = cellH * 0.6f
                val dirY = if (cell.first < rows / 2) 1f else -1f
                speedBoosters.add(SpeedBooster(bx, by, bw, bh, 0f, dirY, 0.02f))
            }
        }

        // Target completion times based on grid size
        val parTime = (12f + cols * 1.5f + rows * 1.2f).coerceIn(15f, 65f)
        val threeStarTime = parTime * 0.75f

        return MazeLevel(
            levelNumber = level,
            theme = theme,
            gridCols = cols,
            gridRows = rows,
            startX = startX,
            startY = startY,
            ballRadius = ballRadius,
            goalHole = goalHole,
            hazardHoles = hazardHoles,
            walls = wallSegments,
            gems = gems,
            speedBoosters = speedBoosters,
            targetTimeSeconds = threeStarTime,
            parTimeSeconds = parTime
        )
    }
}
