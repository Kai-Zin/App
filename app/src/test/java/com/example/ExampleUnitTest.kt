package com.example

import com.example.data.model.Expense
import com.example.data.model.Order
import com.example.data.model.OrderLine
import com.example.domain.SalesAnalytics
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun orderRevenueCostAndProfit_calculatesCorrectlyWithDecimalKgAndExpenses() {
        val order = Order(
            id = "DH-01",
            date = "2026-11-18",
            customerId = "KH-01",
            lines = listOf(
                OrderLine(productId = "SP-1", name = "Sản phẩm A", qty = 1.5, price = 100_000L, cost = 60_000L),
                OrderLine(productId = "SP-2", name = "Sản phẩm B", qty = 2.0, price = 50_000L, cost = 30_000L)
            ),
            surcharge = 20_000L,
            shipFee = 15_000L,
            extraCost = 15_000L
        )
        val expenses = listOf(
            Expense(id = "CP-1", date = "2026-11-18", name = "Đóng gói", category = "Đóng gói", type = "other", amount = 5_000L, orderId = "DH-01"),
            Expense(id = "CP-2", date = "2026-11-18", name = "Ads", category = "Quảng cáo", type = "ads", amount = 30_000L, orderId = "")
        )

        // Revenue = (150,000 + 100,000) + 20,000 = 270,000
        assertEquals(270_000L, SalesAnalytics.orderRevenue(order))
        // Cost = (90,000 + 60,000) = 150,000
        assertEquals(150_000L, SalesAnalytics.orderCost(order))
        // Order Profit = (270,000 - 150,000) - 15,000 (extraCost) - 5,000 (linked expense) = 100,000
        assertEquals(100_000L, SalesAnalytics.orderProfit(order, expenses))

        val totals = SalesAnalytics.totals(listOf(order), expenses)
        assertEquals(270_000L, totals.revenue)
        assertEquals(150_000L, totals.cost)
        // Total expenses = 35,000 (list) + 15,000 (extraCost) = 50,000
        assertEquals(50_000L, totals.expense)
        assertEquals(70_000L, totals.profit)

        val breakdown = SalesAnalytics.expenseBreakdown(listOf(order), expenses)
        assertEquals(30_000L, breakdown.ads)
        assertEquals(20_000L, breakdown.other)
    }

    @Test
    fun formatters_parseAndFormatMoneyAndQty() {
        assertEquals("1.250.000", Formatters.formatMoneyInput("1250000"))
        assertEquals(1250000L, Formatters.parseMoney("1.250.000"))
        assertEquals(1.5, Formatters.parseQty("1,5"), 0.001)
        assertEquals("18/11/2026", Formatters.dateVi("2026-11-18"))
    }
}
