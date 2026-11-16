package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FactorScores
import com.example.data.model.GlobalIndex
import com.example.data.model.TimeRange
import com.example.ui.theme.CrimsonVolatility
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldAlpha
import com.example.ui.theme.SovereignGold
import kotlin.math.roundToInt

val indexPaletteMap = mapOf(
    "SPX" to EmeraldAlpha,
    "NDX" to ElectricCyan,
    "N225" to SovereignGold,
    "DAX" to ElectricViolet,
    "SENSEX" to Color(0xFFFF7A59),
    "FTSE" to Color(0xFF94A3B8)
)

@Composable
fun SparklineChart(
    points: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val minVal = points.minOrNull() ?: 0.0
        val maxVal = points.maxOrNull() ?: 1.0
        val range = (maxVal - minVal).takeIf { it > 0.0001 } ?: 1.0

        val path = Path()
        val fillPath = Path()
        val stepX = size.width / (points.size - 1)

        points.forEachIndexed { idx, value ->
            val x = idx * stepX
            val y = size.height - (((value - minVal) / range).toFloat() * (size.height * 0.85f)) - size.height * 0.075f
            if (idx == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.0f))
            )
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun GlobalDivergenceChart(
    indices: List<GlobalIndex>,
    selectedCodes: Set<String>,
    timeRange: TimeRange,
    modifier: Modifier = Modifier
) {
    val activeIndices = remember(indices, selectedCodes) {
        indices.filter { it.code in selectedCodes }
    }
    var scrubbedIndex by remember(timeRange, activeIndices) { mutableStateOf<Int?>(null) }

    val allSeries = remember(activeIndices, timeRange) {
        activeIndices.mapNotNull { idx ->
            idx.historicalSeries[timeRange]?.let { pts -> idx to pts }
        }
    }
    val pointCount = allSeries.firstOrNull()?.second?.size ?: 0
    val displayIdx = (scrubbedIndex ?: (pointCount - 1)).coerceIn(0, (pointCount - 1).coerceAtLeast(0))

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (allSeries.isNotEmpty() && pointCount > 1) {
            // Scrubbed readout header
            val timeLabel = allSeries.first().second.getOrNull(displayIdx)?.label ?: ""
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (scrubbedIndex != null) "Crosshair: $timeLabel" else "Normalized Return (${timeRange.label})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (allSeries.size >= 2) {
                    val valuesAtPoint = allSeries.map { (_, pts) -> pts[displayIdx].normalizedPct }
                    val spread = (valuesAtPoint.maxOrNull() ?: 0.0) - (valuesAtPoint.minOrNull() ?: 0.0)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Spread ${String.format("%.1f", spread)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            val gridColor = MaterialTheme.colorScheme.outlineVariant
            val zeroLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .pointerInput(pointCount) {
                        detectTapGestures { offset ->
                            val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            scrubbedIndex = (ratio * (pointCount - 1)).roundToInt()
                        }
                    }
                    .pointerInput(pointCount) {
                        detectDragGestures(
                            onDragEnd = { },
                            onDrag = { change, _ ->
                                change.consume()
                                val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                scrubbedIndex = (ratio * (pointCount - 1)).roundToInt()
                            }
                        )
                    }
            ) {
                val allValues = allSeries.flatMap { it.second.map { p -> p.normalizedPct } }
                val rawMin = allValues.minOrNull() ?: -5.0
                val rawMax = allValues.maxOrNull() ?: 5.0
                val pad = ((rawMax - rawMin) * 0.12).coerceAtLeast(1.0)
                val minVal = rawMin - pad
                val maxVal = rawMax + pad
                val valRange = (maxVal - minVal).coerceAtLeast(1.0)

                fun yFor(v: Double): Float {
                    return size.height - (((v - minVal) / valRange).toFloat() * size.height)
                }

                // Grid lines
                for (step in 1..3) {
                    val y = size.height * (step / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Zero baseline
                if (0.0 in minVal..maxVal) {
                    val zeroY = yFor(0.0)
                    drawLine(
                        color = zeroLineColor,
                        start = Offset(0f, zeroY),
                        end = Offset(size.width, zeroY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                }

                val stepX = size.width / (pointCount - 1).coerceAtLeast(1)

                allSeries.forEach { (index, pts) ->
                    val lineColor = indexPaletteMap[index.code] ?: EmeraldAlpha
                    val path = Path()
                    pts.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val y = yFor(pt.normalizedPct)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    val activePt = pts.getOrNull(displayIdx)
                    if (activePt != null) {
                        val cx = displayIdx * stepX
                        val cy = yFor(activePt.normalizedPct)
                        drawCircle(
                            color = lineColor.copy(alpha = 0.25f),
                            radius = 7.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 3.8.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }

                // Vertical crosshair line
                val crossX = displayIdx * stepX
                drawLine(
                    color = zeroLineColor,
                    start = Offset(crossX, 0f),
                    end = Offset(crossX, size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            }

            // Legend pills with exact point values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allSeries.take(3).forEach { (idx, pts) ->
                    val v = pts.getOrNull(displayIdx)?.normalizedPct ?: 0.0
                    LegendValuePill(
                        code = idx.code,
                        valuePct = v,
                        color = indexPaletteMap[idx.code] ?: EmeraldAlpha,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (allSeries.size > 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allSeries.drop(3).forEach { (idx, pts) ->
                        val v = pts.getOrNull(displayIdx)?.normalizedPct ?: 0.0
                        LegendValuePill(
                            code = idx.code,
                            valuePct = v,
                            color = indexPaletteMap[idx.code] ?: EmeraldAlpha,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - (allSeries.size - 3).coerceAtMost(3)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            Text(
                text = "Select at least one exchange above to render historical divergence.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }
    }
}

@Composable
private fun LegendValuePill(
    code: String,
    valuePct: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = code,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            val sign = if (valuePct >= 0) "+" else ""
            Text(
                text = "$sign${String.format("%.1f", valuePct)}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (valuePct >= 0) EmeraldAlpha else CrimsonVolatility
            )
        }
    }
}

@Composable
fun VolatilityConeChart(
    annualVolPct: Double,
    modifier: Modifier = Modifier
) {
    val coneColor = if (annualVolPct > 18.0) CrimsonVolatility else EmeraldAlpha
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val histPts = listOf(0.52f, 0.49f, 0.54f, 0.47f, 0.50f, 0.45f, 0.48f)
        val splitX = size.width * 0.52f
        val histStep = splitX / (histPts.size - 1)

        // Baseline
        drawLine(
            color = outlineColor,
            start = Offset(0f, size.height * 0.5f),
            end = Offset(size.width, size.height * 0.5f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )

        val histPath = Path()
        histPts.forEachIndexed { idx, normY ->
            val x = idx * histStep
            val y = size.height * normY
            if (idx == 0) histPath.moveTo(x, y) else histPath.lineTo(x, y)
        }
        drawPath(
            path = histPath,
            color = ElectricCyan,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )

        val anchorY = size.height * histPts.last()
        val spread = ((annualVolPct / 30.0).toFloat().coerceIn(0.18f, 0.44f)) * size.height
        val conePath = Path().apply {
            moveTo(splitX, anchorY)
            lineTo(size.width, (anchorY - spread).coerceAtLeast(8f))
            lineTo(size.width, (anchorY + spread).coerceAtMost(size.height - 8f))
            close()
        }
        drawPath(
            path = conePath,
            brush = Brush.horizontalGradient(
                colors = listOf(coneColor.copy(alpha = 0.35f), coneColor.copy(alpha = 0.08f)),
                startX = splitX,
                endX = size.width
            )
        )
        // Median projected path
        drawLine(
            color = coneColor,
            start = Offset(splitX, anchorY),
            end = Offset(size.width, (anchorY - spread * 0.28f).coerceAtLeast(10f)),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        )
    }
}

@Composable
fun PortfolioAllocationDonut(
    allocations: List<Pair<String, Double>>,
    totalValueFormatted: String,
    modifier: Modifier = Modifier
) {
    val sliceColors = listOf(EmeraldAlpha, ElectricCyan, SovereignGold, ElectricViolet, Color(0xFFFF7A59))
    val total = allocations.sumOf { it.second }.coerceAtLeast(0.01)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(124.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(116.dp)) {
                val strokeWidth = 16.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                var startAngle = -90f
                allocations.forEachIndexed { idx, (_, value) ->
                    val sweep = ((value / total) * 360.0).toFloat()
                    drawArc(
                        color = sliceColors[idx % sliceColors.size],
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2f).coerceAtLeast(2f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TOTAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = totalValueFormatted,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            allocations.forEachIndexed { idx, (label, value) ->
                val pct = (value / total) * 100.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(sliceColors[idx % sliceColors.size])
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", pct)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FactorBarCluster(
    factors: FactorScores,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FactorRow("Value", factors.value, ElectricCyan)
        FactorRow("Momentum", factors.momentum, EmeraldAlpha)
        FactorRow("Macro Tailwind", factors.macroTailwind, SovereignGold)
        FactorRow("Volatility Shield", factors.volatilityShield, ElectricViolet)
    }
}

@Composable
private fun FactorRow(label: String, score: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(108.dp)
        )
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(50))
        )
        Text(
            text = "$score",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(26.dp)
        )
    }
}
