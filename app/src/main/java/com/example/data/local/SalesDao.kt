package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {
    @Query("SELECT * FROM orders ORDER BY date DESC, updatedAtMs DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, updatedAtMs DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM orders")
    suspend fun getAllOrders(): List<OrderEntity>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrders(orders: List<OrderEntity>)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrderById(id: String)

    @Query("DELETE FROM orders")
    suspend fun clearOrders()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomers(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpenses(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("DELETE FROM expenses WHERE orderId = :orderId")
    suspend fun deleteExpensesByOrderId(orderId: String)

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Transaction
    suspend fun replaceAllData(
        orders: List<OrderEntity>,
        customers: List<CustomerEntity>,
        products: List<ProductEntity>,
        expenses: List<ExpenseEntity>
    ) {
        clearOrders()
        clearCustomers()
        clearProducts()
        clearExpenses()
        upsertOrders(orders)
        upsertCustomers(customers)
        upsertProducts(products)
        upsertExpenses(expenses)
    }

    @Transaction
    suspend fun replaceOrders(orders: List<OrderEntity>) {
        clearOrders()
        upsertOrders(orders)
    }

    @Transaction
    suspend fun replaceCustomers(customers: List<CustomerEntity>) {
        clearCustomers()
        upsertCustomers(customers)
    }

    @Transaction
    suspend fun replaceProducts(products: List<ProductEntity>) {
        clearProducts()
        upsertProducts(products)
    }

    @Transaction
    suspend fun replaceExpenses(expenses: List<ExpenseEntity>) {
        clearExpenses()
        upsertExpenses(expenses)
    }
}
