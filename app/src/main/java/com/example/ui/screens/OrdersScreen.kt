package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense
import com.example.data.model.Order
import com.example.domain.SalesAnalytics
import com.example.ui.SalesUiState
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueSoft
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedSoft
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenSoft
import com.example.util.Formatters

@Composable
fun OrdersScreen(
    uiState: SalesUiState,
    onOrderClick: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val customerMap = remember(uiState.customers) { uiState.customers.associateBy { it.id } }

    val filteredOrders = remember(uiState.orders, customerMap, query) {
        val q = query.trim().lowercase()
        uiState.orders
            .sortedWith(compareByDescending<Order> { it.date }.thenByDescending { it.updatedAtMs })
            .filter { o ->
                if (q.isEmpty()) true
                else {
                    val cName = customerMap[o.customerId]?.name ?: "Khách lẻ"
                    val haystack = "$cName ${o.id} ${o.lines.joinToString(" ") { it.name }}".lowercase()
                    haystack.contains(q)
                }
            }
    }

    val summaryTotals = remember(filteredOrders, uiState.expenses) {
        val rev = filteredOrders.sumOf { SalesAnalytics.orderRevenue(it) }
        val prof = filteredOrders.sumOf { SalesAnalytics.orderProfit(it, uiState.expenses) }
        rev to prof
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Lọc theo mã đơn hàng, tên khách, sản phẩm...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
                .testTag("orders_search_input")
        )

        if (filteredOrders.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredOrders.size} đơn hàng",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Thu: ${Formatters.money(summaryTotals.first)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                        Text(
                            text = "Lãi: ${Formatters.money(summaryTotals.second)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (summaryTotals.second < 0) LossRed else ProfitGreen
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            if (filteredOrders.isEmpty()) {
                EmptyPlaceholder("Chưa có đơn hàng")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        val customerName = customerMap[order.customerId]?.name ?: "Khách lẻ"
                        OrderListRow(
                            order = order,
                            customerName = customerName,
                            allExpenses = uiState.expenses,
                            onClick = { onOrderClick(order) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderListRow(
    order: Order,
    customerName: String,
    allExpenses: List<Expense> = emptyList(),
    onClick: () -> Unit
) {
    val revenue = remember(order) { SalesAnalytics.orderRevenue(order) }
    val profit = remember(order, allExpenses) { SalesAnalytics.orderProfit(order, allExpenses) }
    val itemsSummary = remember(order.lines) {
        order.lines.joinToString(", ") { "${it.name} (${Formatters.qty(it.qty)}kg)" }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag("order_row_${order.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandBlueSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = customerName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${Formatters.dateVi(order.date)} · ${order.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (itemsSummary.isNotBlank()) {
                Text(
                    text = itemsSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = Formatters.money(revenue),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (profit < 0) LossRedSoft else ProfitGreenSoft
            ) {
                Text(
                    text = (if (profit < 0) "Lỗ " else "Lãi ") + Formatters.money(profit),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (profit < 0) LossRed else ProfitGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
