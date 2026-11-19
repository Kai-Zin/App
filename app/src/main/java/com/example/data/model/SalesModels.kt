package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrderLine(
    val productId: String = "",
    val name: String = "",
    val qty: Double = 1.0,
    val price: Long = 0L,
    val cost: Long = 0L
)

data class Customer(
    val id: String,
    val name: String,
    val phone: String = "",
    val area: String = "",
    val address: String = "",
    val note: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class Product(
    val id: String,
    val name: String,
    val cost: Long = 0L,
    val price: Long = 0L,
    val note: String = "",
    val image: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class Order(
    val id: String,
    val date: String,
    val customerId: String = "",
    val lines: List<OrderLine> = emptyList(),
    val surcharge: Long = 0L,
    val shipFee: Long = 0L,
    val extraCost: Long = 0L,
    val note: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class Expense(
    val id: String,
    val date: String,
    val name: String,
    val category: String = "Khác",
    val type: String = "other",
    val amount: Long = 0L,
    val orderId: String = "",
    val note: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

enum class AppPage(val title: String) {
    DASHBOARD("Tổng quan"),
    ORDERS("Đơn hàng"),
    CUSTOMERS("Khách hàng"),
    PRODUCTS("Sản phẩm"),
    EXPENSES("Chi phí"),
    REPORTS("Báo cáo"),
    SETTINGS("Dữ liệu")
}

enum class SyncStatus {
    OK,
    BUSY,
    ERR,
    OUT,
    NONE,
    OFFLINE
}

enum class ReportMode(val label: String) {
    DAY("Ngày"),
    WEEK("Tuần"),
    MONTH("Tháng")
}

enum class CustomerFilter(val label: String) {
    ALL("Tất cả"),
    D15("≥ 15 ngày"),
    D30("≥ 30 ngày"),
    D60("≥ 60 ngày"),
    NEVER("Chưa có đơn")
}
