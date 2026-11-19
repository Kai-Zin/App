package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.Order
import com.example.domain.SalesAnalytics
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.util.Formatters
import kotlin.math.roundToLong

@Composable
fun OrderDetailDialog(
    order: Order,
    customer: Customer?,
    allExpenses: List<Expense>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    var askDelete by remember { mutableStateOf(false) }
    val linked = remember(order.id, allExpenses) {
        allExpenses.filter { it.orderId == order.id }
    }
    val rev = SalesAnalytics.orderRevenue(order)
    val baseCost = SalesAnalytics.orderCost(order)
    val linkedCost = SalesAnalytics.expensesTotal(linked)
    val profit = SalesAnalytics.orderProfit(order, allExpenses)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Chi tiết đơn hàng", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = order.id,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StatusTag(Formatters.dateVi(order.date))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Khách hàng", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DetailKV("Tên khách hàng", customer?.name ?: "Khách lẻ / chưa lưu")
                    DetailKV("Số điện thoại", customer?.phone?.ifBlank { "—" } ?: "—")
                    DetailKV("Khu vực", customer?.area?.ifBlank { "—" } ?: "—")
                    DetailKV("Địa chỉ", customer?.address?.ifBlank { "—" } ?: "—")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text(
                    text = "Sản phẩm trong đơn (${order.lines.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                order.lines.forEach { line ->
                    val lineTotal = (line.price.toDouble() * line.qty).roundToLong()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = line.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Formatters.money(lineTotal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "SL: ${Formatters.qty(line.qty)} kg · Đơn giá: ${Formatters.money(line.price)} · Vốn: ${Formatters.money(line.cost)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text("Tổng kết tài chính", style = MaterialTheme.typography.titleSmall)
                SummaryStatRow("Doanh thu", Formatters.money(rev))
                if (order.surcharge != 0L) {
                    SummaryStatRow("Phụ phí (đã tính trong doanh thu)", Formatters.money(order.surcharge))
                }
                SummaryStatRow("Giá vốn hàng bán", Formatters.money(baseCost))
                SummaryStatRow("Phí vận chuyển / Chi phí riêng", Formatters.money(order.extraCost))
                SummaryStatRow("Chi phí khác gắn với đơn", Formatters.money(linkedCost))
                SummaryStatRow(
                    label = "Lợi nhuận thực tế",
                    value = Formatters.money(profit),
                    valueColor = if (profit < 0) LossRed else ProfitGreen,
                    boldLabel = true
                )

                if (linked.isNotEmpty()) {
                    Text("Các khoản chi gắn với đơn", style = MaterialTheme.typography.titleSmall)
                    linked.forEach { exp ->
                        SummaryStatRow(
                            label = "${exp.name} · ${Formatters.dateVi(exp.date)}",
                            value = Formatters.money(exp.amount)
                        )
                    }
                }

                if (order.note.isNotBlank()) {
                    Text("Ghi chú", style = MaterialTheme.typography.titleSmall)
                    Text(order.note, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { askDelete = true },
                    modifier = Modifier.testTag("delete_order_button")
                ) {
                    Text("Xóa đơn", color = LossRed)
                }
                Button(onClick = onDismiss) {
                    Text("Đóng")
                }
            }
        }
    )

    if (askDelete) {
        ConfirmDeleteDialog(
            message = "Bạn chắc chắn muốn xóa đơn hàng ${order.id}?",
            onConfirm = {
                askDelete = false
                onDelete(order.id)
            },
            onDismiss = { askDelete = false }
        )
    }
}

@Composable
private fun DetailKV(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.padding(horizontal = 6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
