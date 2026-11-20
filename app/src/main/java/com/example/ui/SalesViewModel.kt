package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AppPage
import com.example.data.model.Customer
import com.example.data.model.CustomerFilter
import com.example.data.model.Expense
import com.example.data.model.Order
import com.example.data.model.OrderLine
import com.example.data.model.Product
import com.example.data.model.ReportMode
import com.example.data.model.SyncStatus
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.CloudSessionPrefs
import com.example.data.repository.SalesRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SalesUiState(
    val orders: List<Order> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val products: List<Product> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val cloudPrefs: CloudSessionPrefs = CloudSessionPrefs(),
    val syncStatus: SyncStatus = SyncStatus.OUT,
    val syncMessage: String = ""
)

class SalesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).salesDao()
    private val prefs = AppPreferences(application)
    private val repository = SalesRepository(application, dao, prefs)

    val uiState: StateFlow<SalesUiState> = combine(
        repository.ordersFlow,
        repository.customersFlow,
        repository.productsFlow,
        repository.expensesFlow,
        repository.cloudPrefsFlow,
        repository.syncStatus,
        repository.syncMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SalesUiState(
            orders = values[0] as List<Order>,
            customers = values[1] as List<Customer>,
            products = values[2] as List<Product>,
            expenses = values[3] as List<Expense>,
            cloudPrefs = values[4] as CloudSessionPrefs,
            syncStatus = values[5] as SyncStatus,
            syncMessage = values[6] as String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SalesUiState()
    )

    val uiScale: StateFlow<Float> = prefs.uiScaleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.90f
        )

    private val _currentPage = MutableStateFlow(AppPage.DASHBOARD)
    val currentPage: StateFlow<AppPage> = _currentPage.asStateFlow()

    private val _loadingMessage = MutableStateFlow<String?>(null)
    val loadingMessage: StateFlow<String?> = _loadingMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _reportMode = MutableStateFlow(ReportMode.DAY)
    val reportMode: StateFlow<ReportMode> = _reportMode.asStateFlow()

    private val _reportDate = MutableStateFlow(Formatters.todayIso())
    val reportDate: StateFlow<String> = _reportDate.asStateFlow()

    private val _customerFilter = MutableStateFlow(CustomerFilter.ALL)
    val customerFilter: StateFlow<CustomerFilter> = _customerFilter.asStateFlow()

    private val _isCreatingOrder = MutableStateFlow(false)
    val isCreatingOrder: StateFlow<Boolean> = _isCreatingOrder.asStateFlow()

    private val _selectedOrderId = MutableStateFlow<String?>(null)
    val selectedOrderId: StateFlow<String?> = _selectedOrderId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.refreshSyncBadge()
            repository.pullFromCloud()
        }
    }

    fun setUiScale(scale: Float) {
        viewModelScope.launch {
            prefs.saveUiScale(scale)
        }
    }

    fun navigateTo(page: AppPage) {
        _currentPage.value = page
    }

    fun openOrderDetail(orderId: String) {
        _selectedOrderId.value = orderId
    }

    fun closeOrderDetail() {
        _selectedOrderId.value = null
    }

    fun openCreateOrder() {
        _isCreatingOrder.value = true
    }

    fun closeCreateOrder() {
        _isCreatingOrder.value = false
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun setCustomerFilter(filter: CustomerFilter) {
        _customerFilter.value = filter
    }

    fun setReportMode(mode: ReportMode) {
        _reportMode.value = mode
    }

    fun setReportDate(isoDate: String) {
        if (isoDate.isNotBlank()) _reportDate.value = isoDate
    }

    fun shiftReport(delta: Int) {
        val current = _reportDate.value
        _reportDate.value = when (_reportMode.value) {
            ReportMode.DAY -> Formatters.addDaysIso(current, delta)
            ReportMode.WEEK -> Formatters.addDaysIso(current, delta * 7)
            ReportMode.MONTH -> Formatters.addMonthsIso(current, delta)
        }
    }

    fun resetReportToday() {
        _reportDate.value = Formatters.todayIso()
    }

    fun drillReportToDay(isoDate: String) {
        _reportMode.value = ReportMode.DAY
        _reportDate.value = isoDate
    }

    fun saveCustomer(
        existingId: String? = null,
        name: String,
        phone: String,
        area: String,
        address: String,
        note: String,
        onSaved: (Customer) -> Unit = {}
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _toastMessage.value = "Vui lòng nhập tên khách hàng"
            return
        }
        viewModelScope.launch {
            _loadingMessage.value = "Đang lưu & đồng bộ khách hàng..."
            try {
                val customer = Customer(
                    id = existingId ?: Formatters.newId("KH"),
                    name = trimmedName,
                    phone = phone.trim(),
                    area = area.trim(),
                    address = address.trim(),
                    note = note.trim(),
                    updatedAtMs = System.currentTimeMillis()
                )
                repository.upsertCustomer(customer)
                onSaved(customer)
                _toastMessage.value = "✅ Đã lưu khách hàng!"
            } finally {
                _loadingMessage.value = null
            }
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            _loadingMessage.value = "Đang xóa & đồng bộ..."
            try {
                repository.deleteCustomer(customerId)
                _toastMessage.value = "🗑 Đã xóa khách hàng!"
            } finally {
                _loadingMessage.value = null
            }
        }
    }

    fun saveProduct(
        existingId: String? = null,
        name: String,
        cost: Long,
        price: Long,
        note: String,
        image: String,
        onDone: () -> Unit = {}
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _toastMessage.value = "Vui lòng nhập tên sản phẩm"
            return
        }
        viewModelScope.launch {
            _loadingMessage.value = if (existingId == null) "Đang lưu sản phẩm..." else "Đang cập nhật sản phẩm..."
            try {
                val product = Product(
                    id = existingId ?: Formatters.newId("SP"),
                    name = trimmedName,
                    cost = cost,
                    price = price,
                    note = note.trim(),
                    image = image,
                    updatedAtMs = System.currentTimeMillis()
                )
                repository.upsertProduct(product)
                onDone()
                _toastMessage.value = if (existingId == null) "✅ Đã lưu sản phẩm!" else "✅ Đã cập nhật sản phẩm!"
            } finally {
                _loadingMessage.value = null
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _loadingMessage.value = "Đang xóa & đồng bộ..."
            try {
                repository.deleteProduct(productId)
                _toastMessage.value = "🗑 Đã xóa sản phẩm!"
            } finally {
                _loadingMessage.value = null
            }
        }
    }

    fun saveOrder(
        date: String,
        customerId: String,
        lines: List<OrderLine>,
        surcharge: Long,
        shipFee: Long,
        note: String,
        onSuccess: () -> Unit
    ) {
        val validLines = lines.filter { it.name.trim().isNotEmpty() && it.qty > 0.0 }
        if (validLines.isEmpty()) {
            _toastMessage.value = "Thêm ít nhất một sản phẩm có tên và số lượng > 0"
            return
        }
        viewModelScope.launch {
            _loadingMessage.value = "Đang lưu & đồng bộ đơn hàng..."
            try {
                val order = Order(
                    id = Formatters.newId("DH"),
                    date = date.ifBlank { Formatters.todayIso() },
                    customerId = customerId,
                    lines = validLines.map { it.copy(name = it.name.trim()) },
                    surcharge = surcharge,
                    shipFee = shipFee,
                    extraCost = shipFee,
                    note = note.trim(),
                    updatedAtMs = System.currentTimeMillis()
                )
                repository.upsertOrder(order)
                onSuccess()
                _toastMessage.value = "✅ Đã tạo và đồng bộ đơn hàng!"
            } finally {
                _loadingMessage.value = null
            }
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            _loadingMessage.value = "Đang xóa & đồng bộ..."
            try {
                repository.deleteOrder(orderId)
                if (_selectedOrderId.value == orderId) {
                    _selectedOrderId.value = null
                }
                _toastMessage.value = "🗑 Đã xóa đơn hàng!"
            } finally {
                _loadingMessage.value = null
            }
        }
    }

    fun saveExpense(
        date: String,
        name: String,
        category: String,
        type: String,
        amount: Long,
        orderId: String,
        note: String,
        onDone: () -> Unit
    ) {
        if (name.trim().isEmpty() || amount <= 0L) {
            _toastMessage.value = "Nhập tên khoản chi và số tiền hợp lệ"
            return
        }
        viewModelScope.launch {
            _loadingMessage.value = "Đang lưu & đồng bộ chi phí..."
            try {
                val expense = Expense(
                    id = Formatters.newId("CP"),
                    date = date.ifBlank { Formatters.todayIso() },
                    name = name.trim(),
                    category = category,
                    type = type,
                    amount = amount,
                    orderId = orderId,
                    note = note.trim(),
                    updatedAtMs = System.currentTimeMillis()
                )
                repository.upsertExpense(expense)
                onDone()
                _toastMessage.value = "✅ Đã lưu chi phí!"
            } finally {
                _loadingMessage.value = null
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _loadingMessage.value = "Đang xóa & đồng bộ..."
            try {
                repository.deleteExpense(expenseId)
                _toastMessage.value = "🗑 Đã xóa chi phí!"
            } finally {
                _loadingMessage.value = null
            }
        }
    }

    fun saveSupabaseConfig(url: String, key: String) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanKey = key.trim()
        if (!cleanUrl.startsWith("https://") || cleanKey.isEmpty()) {
            _toastMessage.value = "Nhập Project URL (https://...) và key hợp lệ"
            return
        }
        viewModelScope.launch {
            repository.saveSupabaseConfig(cleanUrl, cleanKey)
            _toastMessage.value = "Đã lưu cấu hình. Tiếp theo hãy bấm Đăng nhập & tải dữ liệu."
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _loadingMessage.value = "Đang tạo tài khoản..."
            try {
                val msg = repository.signUp(email.trim(), password)
                _toastMessage.value = msg
            } catch (e: Exception) {
                _toastMessage.value = "Lỗi tạo tài khoản: ${e.message}"
            } finally {
                _loadingMessage.value = null
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _loadingMessage.value = "Đang đăng nhập & tải dữ liệu mới..."
            try {
                repository.signInAndPull(email.trim(), password)
                _toastMessage.value = "✅ Đăng nhập & đồng bộ thành công!"
            } catch (e: Exception) {
                _toastMessage.value = "Lỗi đăng nhập: ${e.message}"
            } finally {
                _loadingMessage.value = null
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _toastMessage.value = "Đã đăng xuất"
        }
    }

    fun refreshFromCloud() {
        viewModelScope.launch {
            _loadingMessage.value = "Đang tải dữ liệu mới từ Cloud..."
            try {
                val ok = repository.pullFromCloud()
                _toastMessage.value = if (ok) "✅ Đã làm mới dữ liệu" else "⚠ Không thể tải dữ liệu từ Cloud"
            } finally {
                _loadingMessage.value = null
            }
        }
    }

    suspend fun generateBackupJson(): String = repository.exportBackupJson()

    suspend fun generateOrdersCsv(): String = repository.exportOrdersCsv()

    fun restoreBackupJson(jsonText: String) {
        viewModelScope.launch {
            _loadingMessage.value = "Đang khôi phục & đồng bộ dữ liệu..."
            try {
                val ok = repository.importBackupJson(jsonText)
                _toastMessage.value = if (ok) {
                    "✅ Đã khôi phục và đồng bộ xong!"
                } else {
                    "⚠ Đã khôi phục trên máy, nhưng đồng bộ Cloud bị gián đoạn."
                }
            } catch (e: Exception) {
                _toastMessage.value = "File sao lưu không hợp lệ: ${e.message}"
            } finally {
                _loadingMessage.value = null
            }
        }
    }
}
