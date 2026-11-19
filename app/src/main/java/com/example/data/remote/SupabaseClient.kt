package com.example.data.remote

import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.Order
import com.example.data.model.OrderLine
import com.example.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class SupabaseAuthResult(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String
)

data class CloudStartupPull(
    val orders: List<Order>,
    val customers: List<Customer>,
    val expenses: List<Expense>,
    val productsMetadata: List<Product>,
    val missingImageIds: List<String>,
    val fetchedImages: Map<String, String>
)

class SupabaseException(val code: Int, message: String) : Exception(message)

class SupabaseClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun isoTimestampNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    suspend fun signUp(baseUrl: String, apiKey: String, email: String, password: String): SupabaseAuthResult? =
        withContext(Dispatchers.IO) {
            val bodyJson = JSONObject().put("email", email).put("password", password).toString()
            val req = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/auth/v1/signup")
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toRequestBody(jsonMedia))
                .build()

            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw SupabaseException(resp.code, parseErrorMsg(raw, resp.message))
                }
                val json = JSONObject(raw)
                val access = json.optString("access_token")
                if (access.isBlank()) {
                    // Email confirmation required or no immediate session
                    return@withContext null
                }
                val userObj = json.optJSONObject("user")
                SupabaseAuthResult(
                    accessToken = access,
                    refreshToken = json.optString("refresh_token"),
                    userId = userObj?.optString("id").orEmpty(),
                    email = userObj?.optString("email").orEmpty().ifBlank { email }
                )
            }
        }

    suspend fun signIn(baseUrl: String, apiKey: String, email: String, password: String): SupabaseAuthResult =
        withContext(Dispatchers.IO) {
            val bodyJson = JSONObject().put("email", email).put("password", password).toString()
            val req = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/auth/v1/token?grant_type=password")
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toRequestBody(jsonMedia))
                .build()

            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw SupabaseException(resp.code, parseErrorMsg(raw, resp.message))
                }
                val json = JSONObject(raw)
                val userObj = json.optJSONObject("user")
                SupabaseAuthResult(
                    accessToken = json.getString("access_token"),
                    refreshToken = json.optString("refresh_token"),
                    userId = userObj?.optString("id").orEmpty(),
                    email = userObj?.optString("email").orEmpty().ifBlank { email }
                )
            }
        }

    suspend fun refreshSession(baseUrl: String, apiKey: String, refreshToken: String): SupabaseAuthResult =
        withContext(Dispatchers.IO) {
            val bodyJson = JSONObject().put("refresh_token", refreshToken).toString()
            val req = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toRequestBody(jsonMedia))
                .build()

            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw SupabaseException(resp.code, parseErrorMsg(raw, resp.message))
                }
                val json = JSONObject(raw)
                val userObj = json.optJSONObject("user")
                SupabaseAuthResult(
                    accessToken = json.getString("access_token"),
                    refreshToken = json.optString("refresh_token").ifBlank { refreshToken },
                    userId = userObj?.optString("id").orEmpty(),
                    email = userObj?.optString("email").orEmpty()
                )
            }
        }

    suspend fun signOut(baseUrl: String, apiKey: String, accessToken: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/auth/v1/logout")
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post("".toRequestBody(jsonMedia))
                    .build()
                client.newCall(req).execute().close()
            }
        }

    suspend fun upsertOrder(baseUrl: String, apiKey: String, accessToken: String, userId: String, order: Order) {
        val linesArr = JSONArray()
        order.lines.forEach { l ->
            linesArr.put(
                JSONObject()
                    .put("productId", l.productId)
                    .put("name", l.name)
                    .put("qty", l.qty)
                    .put("price", l.price)
                    .put("cost", l.cost)
            )
        }
        val payload = JSONObject()
            .put("id", order.id)
            .put("date", order.date)
            .put("customer_id", order.customerId)
            .put("lines", linesArr)
            .put("surcharge", order.surcharge)
            .put("ship_fee", order.shipFee)
            .put("extra_cost", order.extraCost)
            .put("note", order.note)
            .put("_u", order.updatedAtMs)
            .put("user_id", userId)
            .put("updated_at", isoTimestampNow())
        upsertRow(baseUrl, apiKey, accessToken, "orders", payload)
    }

    suspend fun upsertCustomer(baseUrl: String, apiKey: String, accessToken: String, userId: String, c: Customer) {
        val payload = JSONObject()
            .put("id", c.id)
            .put("name", c.name)
            .put("phone", c.phone)
            .put("area", c.area)
            .put("address", c.address)
            .put("note", c.note)
            .put("_u", c.updatedAtMs)
            .put("user_id", userId)
            .put("updated_at", isoTimestampNow())
        upsertRow(baseUrl, apiKey, accessToken, "customers", payload)
    }

    suspend fun upsertProduct(baseUrl: String, apiKey: String, accessToken: String, userId: String, p: Product) {
        val payload = JSONObject()
            .put("id", p.id)
            .put("name", p.name)
            .put("cost", p.cost)
            .put("price", p.price)
            .put("note", p.note)
            .put("image", p.image)
            .put("_u", p.updatedAtMs)
            .put("user_id", userId)
            .put("updated_at", isoTimestampNow())
        upsertRow(baseUrl, apiKey, accessToken, "products", payload)
    }

    suspend fun upsertExpense(baseUrl: String, apiKey: String, accessToken: String, userId: String, e: Expense) {
        val payload = JSONObject()
            .put("id", e.id)
            .put("date", e.date)
            .put("name", e.name)
            .put("category", e.category)
            .put("type", e.type)
            .put("amount", e.amount)
            .put("order_id", e.orderId)
            .put("note", e.note)
            .put("_u", e.updatedAtMs)
            .put("user_id", userId)
            .put("updated_at", isoTimestampNow())
        upsertRow(baseUrl, apiKey, accessToken, "expenses", payload)
    }

    private suspend fun upsertRow(
        baseUrl: String,
        apiKey: String,
        accessToken: String,
        table: String,
        payload: JSONObject
    ) = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/rest/v1/$table")
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw SupabaseException(resp.code, parseErrorMsg(raw, resp.message))
            }
        }
    }

    suspend fun deleteRow(
        baseUrl: String,
        apiKey: String,
        accessToken: String,
        userId: String,
        table: String,
        id: String
    ) = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/rest/v1/$table?id=eq.${enc(id)}&user_id=eq.${enc(userId)}")
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .delete()
            .build()

        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw SupabaseException(resp.code, parseErrorMsg(raw, resp.message))
            }
        }
    }

    suspend fun pullLightweight(
        baseUrl: String,
        apiKey: String,
        accessToken: String,
        userId: String,
        localProducts: List<Product>
    ): CloudStartupPull = coroutineScope {
        val ordersDef = async {
            fetchJsonArray(baseUrl, apiKey, accessToken, "orders", "*", userId)
        }
        val customersDef = async {
            fetchJsonArray(baseUrl, apiKey, accessToken, "customers", "*", userId)
        }
        val expensesDef = async {
            fetchJsonArray(baseUrl, apiKey, accessToken, "expenses", "*", userId)
        }
        val productsDef = async {
            fetchJsonArray(baseUrl, apiKey, accessToken, "products", "id,name,cost,price,note,_u", userId)
        }

        val ordersArr = ordersDef.await()
        val customersArr = customersDef.await()
        val expensesArr = expensesDef.await()
        val productsArr = productsDef.await()

        val orders = parseOrders(ordersArr)
        val customers = parseCustomers(customersArr)
        val expenses = parseExpenses(expensesArr)
        val oldMap = localProducts.associateBy { it.id }
        val missingImgIds = mutableListOf<String>()
        val productsMeta = (0 until productsArr.length()).map { idx ->
            val r = productsArr.getJSONObject(idx)
            val id = r.optString("id")
            val remoteU = r.optLong("_u", 0L)
            val localP = oldMap[id]
            val keptImage = if (localP != null && localP.image.isNotBlank() && localP.updatedAtMs == remoteU) {
                localP.image
            } else {
                missingImgIds += id
                localP?.image.orEmpty()
            }
            Product(
                id = id,
                name = r.optString("name"),
                cost = r.optLong("cost", 0L),
                price = r.optLong("price", 0L),
                note = r.optString("note"),
                image = keptImage,
                updatedAtMs = remoteU
            )
        }

        val fetchedImages = mutableMapOf<String, String>()
        if (missingImgIds.isNotEmpty()) {
            missingImgIds.chunked(20).forEach { chunk ->
                val inExpr = chunk.joinToString(",") { it }
                val imgUrl = "${baseUrl.trimEnd('/')}/rest/v1/products?select=id,image&id=in.(${enc(inExpr)})"
                val imgArr = fetchRawJsonArray(imgUrl, apiKey, accessToken)
                for (i in 0 until imgArr.length()) {
                    val row = imgArr.getJSONObject(i)
                    val id = row.optString("id")
                    val img = row.optString("image")
                    if (id.isNotBlank() && img.isNotBlank()) {
                        fetchedImages[id] = img
                    }
                }
            }
        }

        CloudStartupPull(
            orders = orders,
            customers = customers,
            expenses = expenses,
            productsMetadata = productsMeta.map { p ->
                val img = fetchedImages[p.id]
                if (!img.isNullOrBlank()) p.copy(image = img) else p
            },
            missingImageIds = missingImgIds,
            fetchedImages = fetchedImages
        )
    }

    private suspend fun fetchJsonArray(
        baseUrl: String,
        apiKey: String,
        accessToken: String,
        table: String,
        select: String,
        userId: String
    ): JSONArray {
        val url = "${baseUrl.trimEnd('/')}/rest/v1/$table?select=${enc(select)}&user_id=eq.${enc(userId)}"
        return fetchRawJsonArray(url, apiKey, accessToken)
    }

    private suspend fun fetchRawJsonArray(
        url: String,
        apiKey: String,
        accessToken: String
    ): JSONArray = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw SupabaseException(resp.code, parseErrorMsg(raw, resp.message))
            }
            if (raw.isBlank()) JSONArray() else JSONArray(raw)
        }
    }

    private fun parseOrders(arr: JSONArray): List<Order> {
        return (0 until arr.length()).map { i ->
            val r = arr.getJSONObject(i)
            val linesJson = r.optJSONArray("lines") ?: JSONArray()
            val lines = (0 until linesJson.length()).map { j ->
                val l = linesJson.getJSONObject(j)
                OrderLine(
                    productId = l.optString("productId"),
                    name = l.optString("name"),
                    qty = l.optDouble("qty", 1.0),
                    price = l.optLong("price", 0L),
                    cost = l.optLong("cost", 0L)
                )
            }
            Order(
                id = r.optString("id"),
                date = r.optString("date"),
                customerId = r.optString("customer_id"),
                lines = lines,
                surcharge = r.optLong("surcharge", 0L),
                shipFee = r.optLong("ship_fee", 0L),
                extraCost = r.optLong("extra_cost", 0L),
                note = r.optString("note"),
                updatedAtMs = r.optLong("_u", 0L)
            )
        }
    }

    private fun parseCustomers(arr: JSONArray): List<Customer> {
        return (0 until arr.length()).map { i ->
            val r = arr.getJSONObject(i)
            Customer(
                id = r.optString("id"),
                name = r.optString("name"),
                phone = r.optString("phone"),
                area = r.optString("area"),
                address = r.optString("address"),
                note = r.optString("note"),
                updatedAtMs = r.optLong("_u", 0L)
            )
        }
    }

    private fun parseErrorMsg(raw: String, fallback: String): String {
        return runCatching {
            val o = JSONObject(raw)
            o.optString("error_description")
                .ifBlank { o.optString("msg") }
                .ifBlank { o.optString("message") }
                .ifBlank { o.optString("hint") }
                .ifBlank { fallback }
        }.getOrDefault(fallback.ifBlank { "Lỗi kết nối máy chủ" })
    }

    fun parseExpenses(arr: JSONArray): List<Expense> {
        return (0 until arr.length()).map { i ->
            val r = arr.getJSONObject(i)
            Expense(
                id = r.optString("id"),
                date = r.optString("date"),
                name = r.optString("name"),
                category = r.optString("category", "Khác"),
                type = r.optString("type", "other"),
                amount = r.optLong("amount", 0L),
                orderId = r.optString("order_id"),
                note = r.optString("note"),
                updatedAtMs = r.optLong("_u", 0L)
            )
        }
    }
}
