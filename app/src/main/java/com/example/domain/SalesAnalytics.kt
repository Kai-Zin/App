package com.example.domain

import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.Order
import com.example.util.Formatters
import kotlin.math.roundToLong

data class PeriodTotals(
    val revenue: Long,
    val cost: Long,
    val expense: Long,
    val profit: Long,
    val orderCount: Int
)

data class ExpenseBreakdown(
    val ads: Long,
    val other: Long
)

data class ChartPoint(
    val key: String,
    val label: String,
    val revenue: Long,
    val totalCost: Long,
    val profit: Long,
    val orderCount: Int,
    val goodsCost: Long,
    val adsCost: Long,
    val otherCost: Long,
    val hasActivity: Boolean
)

data class CustomerCareSummary(
    val customer: Customer,
    val orders: List<Order>,
    val totalSpent: Long,
    val lastOrderDate: String?,
    val daysSinceLastOrder: Int?
)

object SalesAnalytics {

    fun orderRevenue(order: Order): Long {
        val goods = order.lines.sumOf { it.price.toDouble() * it.qty }.roundToLong()
        return goods + order.surcharge
    }

    fun orderCost(order: Order): Long {
        return order.lines.sumOf { it.cost.toDouble() * it.qty }.roundToLong()
    }

    fun expensesTotal(expenses: List<Expense>): Long {
        return expenses.sumOf { it.amount }
    }

    fun orderProfit(order: Order, allExpenses: List<Expense>): Long {
        val margin = order.lines.sumOf { (it.price - it.cost).toDouble() * it.qty }.roundToLong()
        val linkedExpenses = allExpenses.filter { it.orderId == order.id }.sumOf { it.amount }
        return margin + order.surcharge - order.extraCost - linkedExpenses
    }

    fun isAdsExpense(expense: Expense): Boolean {
        val resolvedType = expense.type.ifBlank {
            if (expense.category == "Quảng cáo") "ads" else "other"
        }
        return resolvedType == "ads"
    }

    fun totals(orders: List<Order>, expenses: List<Expense>): PeriodTotals {
        val rev = orders.sumOf { orderRevenue(it) }
        val cogs = orders.sumOf { orderCost(it) }
        val exp = expensesTotal(expenses) + orders.sumOf { it.extraCost }
        return PeriodTotals(
            revenue = rev,
            cost = cogs,
            expense = exp,
            profit = rev - cogs - exp,
            orderCount = orders.size
        )
    }

    fun expenseBreakdown(orders: List<Order>, expenses: List<Expense>): ExpenseBreakdown {
        val ads = expenses.filter { isAdsExpense(it) }.sumOf { it.amount }
        val other = expenses.filterNot { isAdsExpense(it) }.sumOf { it.amount } +
            orders.sumOf { it.extraCost }
        return ExpenseBreakdown(ads = ads, other = other)
    }

    fun last7DaysChart(orders: List<Order>, expenses: List<Expense>): List<ChartPoint> {
        val today = Formatters.todayIso()
        return (-6..0).map { offset ->
            val key = Formatters.addDaysIso(today, offset)
            val os = orders.filter { it.date == key }
            val es = expenses.filter { it.date == key }
            val t = totals(os, es)
            val b = expenseBreakdown(os, es)
            val label = "${key.takeLast(2)}/${key.substring(5, 7)}"
            ChartPoint(
                key = key,
                label = label,
                revenue = t.revenue,
                totalCost = t.cost + t.expense,
                profit = t.profit,
                orderCount = t.orderCount,
                goodsCost = t.cost,
                adsCost = b.ads,
                otherCost = b.other,
                hasActivity = os.isNotEmpty() || es.isNotEmpty()
            )
        }
    }

    fun rangeDailyPoints(
        fromIso: String,
        toIso: String,
        isWeekMode: Boolean,
        orders: List<Order>,
        expenses: List<Expense>
    ): List<ChartPoint> {
        val points = mutableListOf<ChartPoint>()
        var cursor = fromIso
        var guard = 0
        while (cursor <= toIso && guard < 40) {
            val os = orders.filter { it.date == cursor }
            val es = expenses.filter { it.date == cursor }
            val t = totals(os, es)
            val b = expenseBreakdown(os, es)
            val label = if (isWeekMode) {
                "${Formatters.weekdayShort(cursor)} ${cursor.takeLast(2)}"
            } else {
                cursor.takeLast(2)
            }
            points += ChartPoint(
                key = cursor,
                label = label,
                revenue = t.revenue,
                totalCost = t.cost + b.ads + b.other,
                profit = t.profit,
                orderCount = t.orderCount,
                goodsCost = t.cost,
                adsCost = b.ads,
                otherCost = b.other,
                hasActivity = os.isNotEmpty() || es.isNotEmpty()
            )
            cursor = Formatters.addDaysIso(cursor, 1)
            guard++
        }
        return points
    }

    fun buildCustomerCareList(
        customers: List<Customer>,
        orders: List<Order>
    ): List<CustomerCareSummary> {
        return customers.map { c ->
            val customerOrders = orders.filter { it.customerId == c.id }
            val lastDate = customerOrders.map { it.date }.maxOrNull()
            val days = Formatters.daysSince(lastDate)
            val spent = customerOrders.sumOf { orderRevenue(it) }
            CustomerCareSummary(
                customer = c,
                orders = customerOrders,
                totalSpent = spent,
                lastOrderDate = lastDate,
                daysSinceLastOrder = days
            )
        }
    }
}
