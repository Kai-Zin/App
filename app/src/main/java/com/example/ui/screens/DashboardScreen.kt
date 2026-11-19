package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppPage
import com.example.domain.SalesAnalytics
import com.example.ui.SalesUiState
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.ExpandablePanel
import com.example.ui.components.SalesAnalyticsChart
import com.example.ui.components.StatusTag
import com.example.ui.components.SummaryStatRow
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueSoft
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.util.Formatters

@Composable
fun DashboardScreen(
    uiState: SalesUiState,
    onNavigate: (AppPage) -> Unit,
    onOpenOrderDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { Formatters.todayIso() }
    val monthPrefix = remember { Formatters.currentMonthPrefix() }

    val monthOrders = remember(uiState.orders, monthPrefix) {
        uiState.orders.filter { it.date.startsWith(monthPrefix) }
    }
    val monthExpenses = remember(uiState.expenses, monthPrefix) {
        uiState.expenses.filter { it.date.startsWith(monthPrefix) }
    }
    val monthTotals = remember(monthOrders, monthExpenses) {
        SalesAnalytics.totals(monthOrders, monthExpenses)
    }
    val monthBreakdown = remember(monthOrders, monthExpenses) {
        SalesAnalytics.expenseBreakdown(monthOrders, monthExpenses)
    }
    val recentOrders = remember(uiState.orders) {
        uiState.orders.sortedWith(compareByDescending<com.example.data.model.Order> { it.date }.thenByDescending { it.updatedAtMs }).take(5)
    }
    val careCustomers = remember(uiState.customers, uiState.orders) {
        SalesAnalytics.buildCustomerCareList(uiState.customers, uiState.orders)
            .filter { (it.daysSinceLastOrder ?: -1) >= 30 }
            .sortedByDescending { it.daysSinceLastOrder ?: 0 }
    }
    val chart7Days = remember(uiState.orders, uiState.expenses) {
        SalesAnalytics.last7DaysChart(uiState.orders, uiState.expenses)
    }

    val customerMap = remember(uiState.customers) {
        uiState.customers.associateBy { it.id }
    }

    val statRows = remember(uiState.orders, uiState.expenses, today) {
        listOf(
            "Hôm nay" to 0,
            "7 ngày" to 6,
            "30 ngày" to 29
        ).map { (label, backDays) ->
            val from = Formatters.addDaysIso(today, -backDays)
            val os = uiState.orders.filter { it.date in from..today }
            val es = uiState.expenses.filter { it.date in from..today }
            label to SalesAnalytics.totals(os, es)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isWide) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        MonthlyFinancePanel(
                            monthTotals = monthTotals,
                            adsCost = monthBreakdown.ads,
                            otherCost = monthBreakdown.other,
                            onViewReports = { onNavigate(AppPage.REPORTS) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickStatsPanel(
                            statRows = statRows,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                item {
                    MonthlyFinancePanel(
                        monthTotals = monthTotals,
                        adsCost = monthBreakdown.ads,
                        otherCost = monthBreakdown.other,
                        onViewReports = { onNavigate(AppPage.REPORTS) }
                    )
                }
                item {
                    QuickStatsPanel(statRows = statRows)
                }
            }

            item {
                ExpandablePanel(title = "📊 Biểu đồ 7 ngày gần đây") {
                    SalesAnalyticsChart(
                        points = chart7Days,
                        includeProfitLine = true,
                        includeCostBar = true,
                        height = 200.dp
                    )
                }
            }

            item {
                ExpandablePanel(title = "🧾 Đơn hàng gần đây") {
                    if (recentOrders.isEmpty()) {
                        EmptyPlaceholder("Chưa có đơn hàng. Hãy tạo đơn đầu tiên.")
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ngày", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.9f))
                            Text("Khách hàng", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f))
                            Text("Doanh thu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("Lợi nhuận", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        recentOrders.forEach { order ->
                            val customerName = customerMap[order.customerId]?.name ?: "Khách lẻ"
                            val rev = SalesAnalytics.orderRevenue(order)
                            val prof = SalesAnalytics.orderProfit(order, uiState.expenses)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenOrderDetail(order.id) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Formatters.dateVi(order.date).take(5),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(0.9f)
                                )
                                Text(
                                    text = customerName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1.2f)
                                )
                                Text(
                                    text = Formatters.money(rev),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = Formatters.money(prof),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (prof < 0) LossRed else ProfitGreen,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                        TextButton(
                            onClick = { onNavigate(AppPage.ORDERS) },
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(top = 4.dp)
                        ) {
                            Text("Xem tất cả →")
                        }
                    }
                }
            }

            item {
                ExpandablePanel(title = "📞 Khách hàng cần chăm sóc") {
                    if (careCustomers.isEmpty()) {
                        EmptyPlaceholder("Chưa có khách nào quá 30 ngày chưa mua.")
                    } else {
                        careCustomers.forEach { summary ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = summary.customer.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = summary.customer.area.ifBlank {
                                            summary.customer.address.ifBlank { "Chưa có địa chỉ" }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusTag(
                                    text = "${summary.daysSinceLastOrder ?: 0} ngày",
                                    isWarning = true
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyFinancePanel(
    monthTotals: com.example.domain.PeriodTotals,
    adsCost: Long,
    otherCost: Long,
    onViewReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExpandablePanel(title = "📅 Tài chính tháng này", modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Doanh thu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Formatters.money(monthTotals.revenue),
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandBlue,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Lợi nhuận",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Formatters.money(monthTotals.profit),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (monthTotals.profit < 0) LossRed else ProfitGreen,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }

        SummaryStatRow("Số đơn hàng", monthTotals.orderCount.toString())
        SummaryStatRow("Giá vốn hàng bán", Formatters.money(monthTotals.cost))
        SummaryStatRow("Chi phí quảng cáo", Formatters.money(adsCost))
        SummaryStatRow("Chi phí khác", Formatters.money(otherCost))

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onViewReports,
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandBlueSoft,
                contentColor = BrandBlue
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_view_reports_button")
        ) {
            Text("Xem báo cáo chi tiết →", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickStatsPanel(
    statRows: List<Pair<String, com.example.domain.PeriodTotals>>,
    modifier: Modifier = Modifier
) {
    ExpandablePanel(title = "📋 Thống kê", modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("--", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.8f))
            Text("Doanh thu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1.1f))
            Text("Lợi nhuận", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1.1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        statRows.forEach { (label, t) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = Formatters.money(t.revenue),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = Formatters.money(t.profit),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (t.profit < 0) LossRed else ProfitGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.1f)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}
