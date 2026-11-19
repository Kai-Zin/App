package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SalesDao
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.Order
import com.example.data.model.OrderLine
import com.example.data.model.Product
import com.example.data.model.SyncStatus
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.CloudSessionPrefs
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseException
import com.example.domain.SalesAnalytics
import com.example.util.Formatters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class SalesRepository(
    private val context: Context,
    private val dao: SalesDao,
    private val prefs: AppPreferences,
    private val supabase: SupabaseClient = SupabaseClient()
) {
    val ordersFlow: Flow<List<Order>> = dao.observeOrders().map { list -> list.map { it.toModel() } }
    val customersFlow: Flow<List<Customer>> = dao.observeCustomers().map { list -> list.map { it.toModel() } }
    val productsFlow: Flow<List<Product>> = dao.observeProducts().map { list -> list.map { it.toModel() } }
    val expensesFlow: Flow<List<Expense>> = dao.observeExpenses().map { list -> list.map { it.toModel() } }
    val cloudPrefsFlow: Flow<CloudSessionPrefs> = prefs.cloudSessionFlow

    private val _syncStatus = MutableStateFlow(SyncStatus.OUT)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun refreshSyncBadge() {
        val cfg = prefs.currentCloudSession()
        _syncStatus.value = when {
            !cfg.isConfigured -> SyncStatus.NONE
            !isOnline() -> SyncStatus.OFFLINE
            !cfg.isSignedIn -> SyncStatus.OUT
            else -> SyncStatus.OK
        }
    }

    suspend fun saveSupabaseConfig(url: String, key: String) {
        prefs.saveConfig(url, key)
        _syncMessage.value = ""
        refreshSyncBadge()
    }

    suspend fun signUp(email: String, password: String): String {
        val cfg = prefs.currentCloudSession()
        require(cfg.isConfigured) { "Vui lòng lưu cấu hình Supabase trước" }
        val result = supabase.signUp(cfg.supabaseUrl, cfg.supabaseKey, email, password)
        return if (result != null) {
            prefs.saveAuthSession(result.accessToken, result.refreshToken, result.userId, result.email)
            refreshSyncBadge()
            "Tạo tài khoản thành công!"
        } else {
            "Đã gửi email xác nhận. Vui lòng kiểm tra hộp thư trước khi đăng nhập."
        }
    }

    suspend fun signInAndPull(email: String, password: String) {
        val cfg = prefs.currentCloudSession()
        require(cfg.isConfigured) { "Vui lòng lưu cấu hình Supabase trước" }
        val result = supabase.signIn(cfg.supabaseUrl, cfg.supabaseKey, email, password)
        prefs.saveAuthSession(result.accessToken, result.refreshToken, result.userId, result.email)
        pullFromCloud()
    }

    suspend fun signOut() {
        val cfg = prefs.currentCloudSession()
        if (cfg.isSignedIn) {
            supabase.signOut(cfg.supabaseUrl, cfg.supabaseKey, cfg.accessToken)
        }
        prefs.clearAuthSession()
        refreshSyncBadge()
    }

    private suspend fun <T> withValidToken(block: suspend (CloudSessionPrefs) -> T): T {
        val cfg = prefs.currentCloudSession()
        return try {
            block(cfg)
        } catch (e: SupabaseException) {
            if (e.code == 401 && cfg.refreshToken.isNotBlank()) {
                val refreshed = supabase.refreshSession(cfg.supabaseUrl, cfg.supabaseKey, cfg.refreshToken)
                prefs.saveAuthSession(
                    refreshed.accessToken,
                    refreshed.refreshToken,
                    refreshed.userId.ifBlank { cfg.userId },
                    refreshed.email.ifBlank { cfg.userEmail }
                )
                val updatedCfg = prefs.currentCloudSession()
                block(updatedCfg)
            } else {
                throw e
            }
        }
    }

    suspend fun pullFromCloud(): Boolean {
        val cfg = prefs.currentCloudSession()
        if (!cfg.isConfigured) {
            _syncStatus.value = SyncStatus.NONE
            return false
        }
        if (!isOnline()) {
            _syncStatus.value = SyncStatus.OFFLINE
            return false
        }
        if (!cfg.isSignedIn) {
            _syncStatus.value = SyncStatus.OUT
            return false
        }
        _syncStatus.value = SyncStatus.BUSY
        return try {
            val localProducts = dao.getAllProducts().map { it.toModel() }
            val pull = withValidToken { active ->
                supabase.pullLightweight(
                    baseUrl = active.supabaseUrl,
                    apiKey = active.supabaseKey,
                    accessToken = active.accessToken,
                    userId = active.userId,
                    localProducts = localProducts
                )
            }
            dao.replaceAllData(
                orders = pull.orders.map { OrderEntity.fromModel(it) },
                customers = pull.customers.map { CustomerEntity.fromModel(it) },
                products = pull.productsMetadata.map { ProductEntity.fromModel(it) },
                expenses = pull.expenses.map { ExpenseEntity.fromModel(it) }
            )
            prefs.markLastSyncOk()
            _syncStatus.value = SyncStatus.OK
            _syncMessage.value = ""
            true
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERR
            _syncMessage.value = e.message ?: "Lỗi đồng bộ"
            false
        }
    }

    suspend fun upsertOrder(order: Order): Boolean {
        dao.upsertOrder(OrderEntity.fromModel(order))
        return syncItem { cfg ->
            supabase.upsertOrder(cfg.supabaseUrl, cfg.supabaseKey, cfg.accessToken, cfg.userId, order)
        }
    }

    suspend fun deleteOrder(orderId: String): Boolean {
        val linkedExpenses = dao.getAllExpenses().filter { it.orderId == orderId }
        dao.deleteExpensesByOrderId(orderId)
        dao.deleteOrderById(orderId)
        return syncItem { cfg ->
            linkedExpenses.forEach { exp ->
                supabase.deleteRow(cfg.supabaseUrl, cfg.supabaseKey, cfg.accessToken, cfg.userId, "expenses", exp.id)
            }
            supabase.deleteRow(cfg.supabaseUrl, cfg.supabaseKey, cfg.accessToken, cfg.userId, "orders", orderId)
        }
    }

    suspend fun upsertCustomer(customer: Customer): Boolean {
        dao.upsertCustomer(CustomerEntity.fromModel(customer))
        return syncItem { cfg ->
            supabase.upsertCustomer(cfg.supabaseUrl, cfg.supabaseKey, cfg.accessToken, cfg.userId, customer)
        }
    }

    suspend fun deleteCustomer(customerId: String): Boolean {
        dao.deleteCustomerById(customerId)
        return syncItem { cfg ->
            supabase.deleteRow(cfg.supabaseUrl, cfg.supabaseKey, cfg.accessToken, cfg.userId, "customers", customerId)
        }
    }

    suspend fun upsertProduct(product: Product): Boolean {
        dao.upsertProduct(ProductEntity.fromModel(product))
        return syncItem { cfg ->
            supabase.upsertProduct(cfg.supabaseUrl, cfg.supabaseKey, cfg.accessToken, cfg.userId, product)
        }
    }

    suspend fun deleteProduct(productId: String): Boolean {
        dao.deleteProductById(productId)
        return syncItem { cfg ->
            supabase.deleteRow(cfg.supabaseUrl, cfg.supabaseKey, cfg.accessToken, cfg.userId, "products", productId)
        }
    }

    suspend fun upsertExpense(expense: Expense): Boolean {
        dao.upsertExpense(ExpenseEntity.fromModel(expense))
        return syncItem { cfg ->
            supabase.upsertExpense(cfg.supabaseUrl, cfg.supabaseKey, cfg.accessToken, cfg.userId, expense)
        }
    }

    suspend fun deleteExpense(expenseId: String): Boolean {
        dao.deleteExpenseById(expenseId)
        return syncItem { cfg ->
            supabase.deleteRow(cfg.supabaseUrl, cfg.supabaseKey, cfg.accessToken, cfg.userId, "expenses", expenseId)
        }
    }

    private suspend fun syncItem(action: suspend (CloudSessionPrefs) -> Unit): Boolean {
        val cfg = prefs.currentCloudSession()
        if (!cfg.isConfigured) {
            _syncStatus.value = SyncStatus.NONE
            return false
        }
        if (!isOnline()) {
            _syncStatus.value = SyncStatus.OFFLINE
            return false
        }
        if (!cfg.isSignedIn) {
            _syncStatus.value = SyncStatus.OUT
            return false
        }
        _syncStatus.value = SyncStatus.BUSY
        return try {
            withValidToken { active -> action(active) }
            prefs.markLastSyncOk()
            _syncStatus.value = SyncStatus.OK
            _syncMessage.value = ""
            true
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERR
            _syncMessage.value = e.message ?: "Lỗi đồng bộ"
            false
        }
    }

    suspend fun exportBackupJson(): String {
        val orders = dao.getAllOrders().map { it.toModel() }
        val customers = dao.getAllCustomers().map { it.toModel() }
        val products = dao.getAllProducts().map { it.toModel() }
        val expenses = dao.getAllExpenses().map { it.toModel() }

        val root = JSONObject()
        root.put("orders", JSONArray().apply {
            orders.forEach { o ->
                put(JSONObject().apply {
                    put("id", o.id)
                    put("date", o.date)
                    put("customerId", o.customerId)
                    put("lines", JSONArray().apply {
                        o.lines.forEach { l ->
                            put(JSONObject().apply {
                                put("productId", l.productId)
                                put("name", l.name)
                                put("qty", l.qty)
                                put("price", l.price)
                                put("cost", l.cost)
                            })
                        }
                    })
                    put("surcharge", o.surcharge)
                    put("shipFee", o.shipFee)
                    put("extraCost", o.extraCost)
                    put("note", o.note)
                    put("_u", o.updatedAtMs)
                })
            }
        })
        root.put("customers", JSONArray().apply {
            customers.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("phone", c.phone)
                    put("area", c.area)
                    put("address", c.address)
                    put("note", c.note)
                    put("_u", c.updatedAtMs)
                })
            }
        })
        root.put("products", JSONArray().apply {
            products.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("cost", p.cost)
                    put("price", p.price)
                    put("note", p.note)
                    put("image", p.image)
                    put("_u", p.updatedAtMs)
                })
            }
        })
        root.put("expenses", JSONArray().apply {
            expenses.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("date", e.date)
                    put("name", e.name)
                    put("category", e.category)
                    put("type", e.type)
                    put("amount", e.amount)
                    put("orderId", e.orderId)
                    put("note", e.note)
                    put("_u", e.updatedAtMs)
                })
            }
        })
        return root.toString(2)
    }

    suspend fun importBackupJson(jsonText: String): Boolean {
        val root = JSONObject(jsonText)
        if (!root.has("orders") || !root.has("customers") || !root.has("products") || !root.has("expenses")) {
            throw IllegalArgumentException("File sao lưu không hợp lệ")
        }

        val ordersArr = root.getJSONArray("orders")
        val customersArr = root.getJSONArray("customers")
        val productsArr = root.getJSONArray("products")
        val expensesArr = root.getJSONArray("expenses")

        val orders = (0 until ordersArr.length()).map { i ->
            val o = ordersArr.getJSONObject(i)
            val linesArr = o.optJSONArray("lines") ?: JSONArray()
            val lines = (0 until linesArr.length()).map { j ->
                val l = linesArr.getJSONObject(j)
                OrderLine(
                    productId = l.optString("productId"),
                    name = l.optString("name"),
                    qty = l.optDouble("qty", 1.0),
                    price = l.optLong("price", 0L),
                    cost = l.optLong("cost", 0L)
                )
            }
            Order(
                id = o.optString("id").ifBlank { Formatters.newId("DH") },
                date = o.optString("date").ifBlank { Formatters.todayIso() },
                customerId = o.optString("customerId"),
                lines = lines,
                surcharge = o.optLong("surcharge", 0L),
                shipFee = o.optLong("shipFee", 0L),
                extraCost = o.optLong("extraCost", 0L),
                note = o.optString("note"),
                updatedAtMs = o.optLong("_u", System.currentTimeMillis())
            )
        }

        val customers = (0 until customersArr.length()).map { i ->
            val c = customersArr.getJSONObject(i)
            Customer(
                id = c.optString("id").ifBlank { Formatters.newId("KH") },
                name = c.optString("name"),
                phone = c.optString("phone"),
                area = c.optString("area"),
                address = c.optString("address"),
                note = c.optString("note"),
                updatedAtMs = c.optLong("_u", System.currentTimeMillis())
            )
        }

        val products = (0 until productsArr.length()).map { i ->
            val p = productsArr.getJSONObject(i)
            Product(
                id = p.optString("id").ifBlank { Formatters.newId("SP") },
                name = p.optString("name"),
                cost = p.optLong("cost", 0L),
                price = p.optLong("price", 0L),
                note = p.optString("note"),
                image = p.optString("image"),
                updatedAtMs = p.optLong("_u", System.currentTimeMillis())
            )
        }

        val expenses = (0 until expensesArr.length()).map { i ->
            val e = expensesArr.getJSONObject(i)
            Expense(
                id = e.optString("id").ifBlank { Formatters.newId("CP") },
                date = e.optString("date").ifBlank { Formatters.todayIso() },
                name = e.optString("name"),
                category = e.optString("category", "Khác"),
                type = e.optString("type", "other"),
                amount = e.optLong("amount", 0L),
                orderId = e.optString("orderId"),
                note = e.optString("note"),
                updatedAtMs = e.optLong("_u", System.currentTimeMillis())
            )
        }

        dao.replaceAllData(
            orders = orders.map { OrderEntity.fromModel(it) },
            customers = customers.map { CustomerEntity.fromModel(it) },
            products = products.map { ProductEntity.fromModel(it) },
            expenses = expenses.map { ExpenseEntity.fromModel(it) }
        )

        val cfg = prefs.currentCloudSession()
        if (cfg.isSignedIn && isOnline()) {
            _syncStatus.value = SyncStatus.BUSY
            return try {
                withValidToken { active ->
                    customers.forEach { supabase.upsertCustomer(active.supabaseUrl, active.supabaseKey, active.accessToken, active.userId, it) }
                    products.forEach { supabase.upsertProduct(active.supabaseUrl, active.supabaseKey, active.accessToken, active.userId, it) }
                    orders.forEach { supabase.upsertOrder(active.supabaseUrl, active.supabaseKey, active.accessToken, active.userId, it) }
                    expenses.forEach { supabase.upsertExpense(active.supabaseUrl, active.supabaseKey, active.accessToken, active.userId, it) }
                }
                prefs.markLastSyncOk()
                _syncStatus.value = SyncStatus.OK
                true
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERR
                _syncMessage.value = e.message ?: "Lỗi đẩy dữ liệu"
                false
            }
        }
        return true
    }

    suspend fun exportOrdersCsv(): String {
        val orders = dao.getAllOrders().map { it.toModel() }
        val customers = dao.getAllCustomers().associate { it.id to it.name }
        val expenses = dao.getAllExpenses().map { it.toModel() }
        val sb = StringBuilder("\uFEFF")
        sb.append("Ngày,Mã đơn,Khách hàng,Sản phẩm,Doanh thu,Giá vốn,Chi phí riêng,Lợi nhuận\n")
        orders.forEach { o ->
            val customerName = customers[o.customerId] ?: "Khách lẻ"
            val itemsSummary = o.lines.joinToString("; ") { "${it.name} x${Formatters.qty(it.qty)}" }
            val rev = SalesAnalytics.orderRevenue(o)
            val cost = SalesAnalytics.orderCost(o)
            val profit = SalesAnalytics.orderProfit(o, expenses)
            val cols = listOf(
                o.date,
                o.id,
                customerName,
                itemsSummary,
                rev.toString(),
                cost.toString(),
                o.extraCost.toString(),
                profit.toString()
            )
            sb.append(cols.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }).append("\n")
        }
        return sb.toString()
    }
}
