package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.Order
import com.example.data.model.OrderLine
import com.example.data.model.Product
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object OrderLineSerializer {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, OrderLine::class.java)
    private val adapter = moshi.adapter<List<OrderLine>>(listType)

    fun toJson(lines: List<OrderLine>): String = adapter.toJson(lines) ?: "[]"

    fun fromJson(json: String): List<OrderLine> {
        if (json.isBlank()) return emptyList()
        return runCatching { adapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())
    }
}

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val area: String,
    val address: String,
    val note: String,
    val updatedAtMs: Long
) {
    fun toModel() = Customer(id, name, phone, area, address, note, updatedAtMs)

    companion object {
        fun fromModel(m: Customer) = CustomerEntity(m.id, m.name, m.phone, m.area, m.address, m.note, m.updatedAtMs)
    }
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cost: Long,
    val price: Long,
    val note: String,
    val image: String,
    val updatedAtMs: Long
) {
    fun toModel() = Product(id, name, cost, price, note, image, updatedAtMs)

    companion object {
        fun fromModel(m: Product) = ProductEntity(m.id, m.name, m.cost, m.price, m.note, m.image, m.updatedAtMs)
    }
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val date: String,
    val customerId: String,
    val linesJson: String,
    val surcharge: Long,
    val shipFee: Long,
    val extraCost: Long,
    val note: String,
    val updatedAtMs: Long
) {
    fun toModel() = Order(
        id = id,
        date = date,
        customerId = customerId,
        lines = OrderLineSerializer.fromJson(linesJson),
        surcharge = surcharge,
        shipFee = shipFee,
        extraCost = extraCost,
        note = note,
        updatedAtMs = updatedAtMs
    )

    companion object {
        fun fromModel(m: Order) = OrderEntity(
            id = m.id,
            date = m.date,
            customerId = m.customerId,
            linesJson = OrderLineSerializer.toJson(m.lines),
            surcharge = m.surcharge,
            shipFee = m.shipFee,
            extraCost = m.extraCost,
            note = m.note,
            updatedAtMs = m.updatedAtMs
        )
    }
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val date: String,
    val name: String,
    val category: String,
    val type: String,
    val amount: Long,
    val orderId: String,
    val note: String,
    val updatedAtMs: Long
) {
    fun toModel() = Expense(id, date, name, category, type, amount, orderId, note, updatedAtMs)

    companion object {
        fun fromModel(m: Expense) = ExpenseEntity(m.id, m.date, m.name, m.category, m.type, m.amount, m.orderId, m.note, m.updatedAtMs)
    }
}
