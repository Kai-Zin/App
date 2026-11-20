package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.ReportMode
import com.example.domain.SalesAnalytics
import com.example.ui.SalesUiState
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.SalesAnalyticsChart
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueSoft
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmberSoft
import com.example.ui.theme.WarningAmberText
import com.example.util.Formatters
import java.util.Calendar
import java.util.Locale

@Composable
fun ReportsScreen(
    uiState: SalesUiState,
    mode: ReportMode,
    anchorDate: String,
    onModeChange: (ReportMode) -> Unit,
    onAnchorDateChange: (String) -> Unit,
    onShift: (Int) -> Unit,
    onResetToday: () -> Unit,
    onDrillToDay: (String) -> Unit,
    onOrderClick: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (fromIso, toIso) = remember(mode, anchorDate) {
        when (mode) {
            ReportMode.DAY -> anchorDate to anchorDate
            ReportMode.WEEK -> {
                val f = Formatters.weekStartIso(anchorDate)
                f to Formatters.addDaysIso(f, 6)
            }
            ReportMode.MONTH -> Formatters.monthRangeIso(anchorDate)
        }
    }

    val rangeOrders = remember(uiState.orders, fromIso, toIso) {
        uiState.orders.filter { it.date in fromIso..toIso }
    }
    val rangeExpenses = remember(uiState.expenses, fromIso, toIso) {
        uiState.expenses.filter { it.date in fromIso..toIso }
    }
    val totals = remember(rangeOrders, rangeExpenses) {
        SalesAnalytics.totals(rangeOrders, rangeExpenses)
    }
    val breakdown = remember(rangeOrders, rangeExpenses) {
        SalesAnalytics.expenseBreakdown(rangeOrders, rangeExpenses)
    }
    val allCost = totals.cost + breakdown.ads + breakdown.other
    val customerMap = remember(uiState.customers) { uiState.customers.associateBy { it.id } }

    val periodLabel = remember(mode, anchorDate, fromIso, toIso) {
        when (mode) {
            ReportMode.DAY -> Formatters.dateVi(anchorDate)
            ReportMode.WEEK -> "Tuần ${Formatters.dateVi(fromIso)} – ${Formatters.dateVi(toIso)}"
            ReportMode.MONTH -> "Tháng ${anchorDate.substring(5, 7)}/${anchorDate.take(4)}"
        }
    }

    val dailyPoints = remember(mode, fromIso, toIso, rangeOrders, rangeExpenses) {
        if (mode == ReportMode.DAY) emptyList()
        else SalesAnalytics.rangeDailyPoints(
            fromIso = fromIso,
            toIso = toIso,
            isWeekMode = (mode == ReportMode.WEEK),
            orders = rangeOrders,
            expenses = rangeExpenses
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportMode.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { onModeChange(m) },
                        label = { Text(m.label) },
                        modifier = Modifier.testTag("report_mode_${m.name.lowercase()}")
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onShift(-1) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Trước")
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val cal = Formatters.parseCalendar(anchorDate)
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val picked = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                    onAnchorDateChange(picked)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = when (mode) {
                            ReportMode.DAY -> "Nhấn để chọn ngày"
                            ReportMode.WEEK -> "Nhấn để chọn ngày trong tuần"
                            ReportMode.MONTH -> "Nhấn để chọn tháng"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                FilledTonalButton(
                    onClick = { onShift(1) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sau")
                }
            }
        }

        item {
            OutlinedButton(onClick = onResetToday) {
                Text(
                    when (mode) {
                        ReportMode.DAY -> "Hôm nay"
                        ReportMode.WEEK -> "Tuần này"
                        ReportMode.MONTH -> "Tháng này"
                    }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard("Doanh thu", Formatters.money(totals.revenue), periodLabel, Modifier.weight(1f))
                    KpiCard("Tổng chi phí", Formatters.money(allCost), "Vốn + QC + khác", Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard("Chi phí quảng cáo", Formatters.money(breakdown.ads), "", Modifier.weight(1f))
                    KpiCard("Chi phí khác", Formatters.money(breakdown.other), "", Modifier.weight(1f))
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lợi nhuận ròng",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${totals.orderCount} đơn hàng",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = Formatters.money(totals.profit),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totals.profit < 0) LossRed else ProfitGreen
                    )
                }
            }
        }

        if (mode == ReportMode.DAY) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Đơn hàng trong ngày", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        val sorted = rangeOrders.sortedByDescending { it.updatedAtMs }
                        if (sorted.isEmpty()) {
                            EmptyPlaceholder("Không có đơn hàng")
                        } else {
                            sorted.forEach { order ->
                                OrderListRow(
                                    order = order,
                                    customerName = customerMap[order.customerId]?.name ?: "Khách lẻ",
                                    allExpenses = uiState.expenses,
                                    onClick = { onOrderClick(order) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Chi phí trong ngày", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (rangeExpenses.isEmpty()) {
                            EmptyPlaceholder("Không có chi phí ghi riêng")
                        } else {
                            rangeExpenses.forEach { e ->
                                val isAds = SalesAnalytics.isAdsExpense(e)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isAds) WarningAmberSoft else BrandBlueSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isAds) Icons.Default.Campaign else Icons.Default.Receipt,
                                            contentDescription = null,
                                            tint = if (isAds) WarningAmberText else BrandBlue
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(e.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (e.orderId.isNotBlank()) "Gắn với đơn ${e.orderId}" else "Chi phí chung",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(Formatters.money(e.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Doanh thu, lợi nhuận & chi phí theo ngày", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        SalesAnalyticsChart(
                            points = dailyPoints,
                            includeProfitLine = (mode == ReportMode.WEEK),
                            includeCostBar = (mode == ReportMode.WEEK),
                            height = 220.dp
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Chi tiết từng ngày", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        val activeDays = dailyPoints.filter { it.hasActivity }.reversed()
                        if (activeDays.isEmpty()) {
                            EmptyPlaceholder("Chưa có dữ liệu trong khoảng này")
                        } else {
                            activeDays.forEach { d ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onDrillToDay(d.key) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(BrandBlueSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = d.key.takeLast(2),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BrandBlue
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${Formatters.weekdayShort(d.key)} · ${Formatters.dateVi(d.key)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${d.orderCount} đơn · Vốn ${Formatters.money(d.goodsCost)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "QC ${Formatters.money(d.adsCost)} · Khác ${Formatters.money(d.otherCost)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Formatters.money(d.revenue),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "Lãi ${Formatters.money(d.profit)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (d.profit < 0) LossRed else ProfitGreen
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    note: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            if (note.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
