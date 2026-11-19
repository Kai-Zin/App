package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ChartPoint
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.util.Formatters
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class ChartSeriesType { BAR, LINE }

data class ChartSeriesSpec(
    val label: String,
    val color: Color,
    val type: ChartSeriesType,
    val selector: (ChartPoint) -> Long
)

@Composable
fun SalesAnalyticsChart(
    points: List<ChartPoint>,
    includeProfitLine: Boolean = true,
    includeCostBar: Boolean = true,
    height: Dp = 210.dp,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        EmptyPlaceholder("Chưa có dữ liệu để vẽ biểu đồ.")
        return
    }

    val series = buildList {
        add(ChartSeriesSpec("Doanh số", BrandBlue, ChartSeriesType.BAR) { it.revenue })
        if (includeCostBar) {
            add(ChartSeriesSpec("Chi phí", WarningAmber, ChartSeriesType.BAR) { it.totalCost })
        }
        if (includeProfitLine) {
            add(ChartSeriesSpec("Lợi nhuận", ProfitGreen, ChartSeriesType.LINE) { it.profit })
        } else {
            add(ChartSeriesSpec("Lợi nhuận", ProfitGreen, ChartSeriesType.BAR) { it.profit })
        }
    }

    val gridColor = MaterialTheme.colorScheme.outline
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val minChartWidth = max(maxWidth.value, points.size * 44f).dp
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(minChartWidth)
                        .height(height)
                        .padding(top = 6.dp, bottom = 4.dp)
                ) {
                    val leftPad = 48.dp.toPx()
                    val rightPad = 16.dp.toPx()
                    val topPad = 14.dp.toPx()
                    val bottomPad = 28.dp.toPx()

                    val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                    val plotH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                    val allVals = points.flatMap { p -> series.map { it.selector(p).toDouble() } }
                    val lo = min(0.0, allVals.minOrNull() ?: 0.0)
                    val hi = max(1.0, allVals.maxOrNull() ?: 1.0)
                    val span = (hi - lo).takeIf { it > 0.0 } ?: 1.0
                    val minV = lo - span * 0.12
                    val maxV = hi + span * 0.12

                    fun xPos(i: Int): Float {
                        return if (points.size == 1) {
                            leftPad + plotW / 2f
                        } else {
                            leftPad + i * (plotW / (points.size - 1).toFloat())
                        }
                    }

                    fun yPos(v: Double): Float {
                        return (topPad + ((maxV - v) / (maxV - minV)) * plotH).toFloat()
                    }

                    // Horizontal grid lines + Y-axis labels
                    for (step in 0..4) {
                        val v = maxV - (maxV - minV) * step / 4.0
                        val yy = yPos(v)
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPad, yy),
                            end = Offset(size.width - rightPad, yy),
                            strokeWidth = 1.dp.toPx()
                        )
                        val label = Formatters.chartCompact(v)
                        val measured = textMeasurer.measure(
                            text = label,
                            style = TextStyle(color = axisColor, fontSize = 10.sp)
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x = (leftPad - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                                y = yy - measured.size.height / 2f
                            )
                        )
                    }

                    // Bars
                    val bars = series.filter { it.type == ChartSeriesType.BAR }
                    val slotW = plotW / max(points.size, 1).toFloat()
                    val groupW = min(32.dp.toPx(), slotW * 0.56f)
                    val eachBarW = groupW / max(bars.size, 1).toFloat()
                    val baseY = yPos(0.0)

                    points.forEachIndexed { idx, point ->
                        bars.forEachIndexed { bIdx, spec ->
                            val v = spec.selector(point).toDouble()
                            val yy = yPos(v)
                            val topY = min(yy, baseY)
                            val barH = max(2f, abs(baseY - yy))
                            val barLeft = xPos(idx) + (bIdx - (bars.size - 1) / 2f) * eachBarW - (eachBarW * 0.42f)
                            drawRoundRect(
                                color = spec.color,
                                topLeft = Offset(barLeft, topY),
                                size = Size(eachBarW * 0.84f, barH),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    }

                    // Lines + Dots + Value Labels
                    series.filter { it.type == ChartSeriesType.LINE }.forEach { spec ->
                        val path = Path()
                        points.forEachIndexed { idx, point ->
                            val cx = xPos(idx)
                            val cy = yPos(spec.selector(point).toDouble())
                            if (idx == 0) path.moveTo(cx, cy) else path.lineTo(cx, cy)
                        }
                        drawPath(
                            path = path,
                            color = spec.color,
                            style = Stroke(
                                width = 2.6.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        points.forEachIndexed { idx, point ->
                            val rawVal = spec.selector(point).toDouble()
                            val cx = xPos(idx)
                            val cy = yPos(rawVal)
                            drawCircle(color = Color.White, radius = 4.5.dp.toPx(), center = Offset(cx, cy))
                            drawCircle(color = spec.color, radius = 3.2.dp.toPx(), center = Offset(cx, cy))

                            if (points.size <= 12 || idx % 2 == 0) {
                                val valLabel = Formatters.chartCompact(rawVal)
                                val measured = textMeasurer.measure(
                                    text = valLabel,
                                    style = TextStyle(
                                        color = spec.color,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                val labelY = (cy - measured.size.height - 4.dp.toPx()).coerceAtLeast(0f)
                                drawText(
                                    textLayoutResult = measured,
                                    topLeft = Offset(
                                        x = (cx - measured.size.width / 2f).coerceIn(leftPad, size.width - measured.size.width),
                                        y = labelY
                                    )
                                )
                            }
                        }
                    }

                    // X-axis labels
                    val stepLabel = when {
                        points.size <= 10 -> 1
                        points.size <= 20 -> 2
                        else -> 3
                    }
                    points.forEachIndexed { idx, point ->
                        if (idx % stepLabel == 0 || idx == points.lastIndex) {
                            val measured = textMeasurer.measure(
                                text = point.label,
                                style = TextStyle(color = axisColor, fontSize = 10.sp)
                            )
                            val cx = xPos(idx)
                            drawText(
                                textLayoutResult = measured,
                                topLeft = Offset(
                                    x = (cx - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width),
                                    y = size.height - measured.size.height - 2.dp.toPx()
                                )
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            series.forEach { spec ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(spec.color)
                    )
                    Text(
                        text = spec.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
