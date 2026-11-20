package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.data.model.CustomerFilter
import com.example.data.model.Order
import com.example.data.model.OrderLine
import com.example.data.model.Product
import com.example.domain.SalesAnalytics
import com.example.ui.SalesUiState
import com.example.ui.components.DatePickerField
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.MoneyInputField
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.StatusTag
import com.example.ui.components.SummaryStatRow
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueSoft
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.util.Formatters
import kotlin.math.round
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    uiState: SalesUiState,
    onClose: () -> Unit,
    onQuickAddCustomer: (String, String, String, String, (Customer) -> Unit) -> Unit,
    onSaveOrder: (String, String, List<OrderLine>, Long, Long, String) -> Unit
) {
    val lines = remember { mutableStateListOf<OrderLine>() }
    var selectedCustomerId by remember { mutableStateOf("") }
    var orderDate by remember { mutableStateOf(Formatters.todayIso()) }
    var note by remember { mutableStateOf("") }
    var surcharge by remember { mutableLongStateOf(0L) }
    var shipFee by remember { mutableLongStateOf(0L) }

    var showPicker by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var askDiscard by remember { mutableStateOf(false) }

    val productMap = remember(uiState.products) { uiState.products.associateBy { it.id } }
    val selectedCustomer = remember(uiState.customers, selectedCustomerId) {
        uiState.customers.find { it.id == selectedCustomerId }
    }

    val goodsTotal = lines.sumOf { (it.price.toDouble() * it.qty) }.roundToLong()
    val costTotal = lines.sumOf { (it.cost.toDouble() * it.qty) }.roundToLong()
    val qtyTotal = lines.sumOf { it.qty }
    val grandTotal = goodsTotal + surcharge
    val estimatedProfit = grandTotal - costTotal - shipFee

    fun attemptClose() {
        if (lines.isNotEmpty()) askDiscard = true else onClose()
    }

    BackHandler(enabled = !showPicker && !showCustomerPicker) {
        attemptClose()
    }

    if (showPicker) {
        ProductPickerScreen(
            products = uiState.products,
            initialSelectedIds = lines.mapNotNull { it.productId.takeIf(String::isNotBlank) }.toSet(),
            onBack = { showPicker = false },
            onApply = { pickedIds ->
                val kept = lines.filter { it.productId.isBlank() || pickedIds.contains(it.productId) }.toMutableList()
                pickedIds.forEach { pid ->
                    if (kept.none { it.productId == pid }) {
                        productMap[pid]?.let { p ->
                            kept += OrderLine(
                                productId = p.id,
                                name = p.name,
                                qty = 1.0,
                                price = p.price,
                                cost = p.cost
                            )
                        }
                    }
                }
                lines.clear()
                lines.addAll(kept)
                showPicker = false
            },
            onAddCustom = { pickedIds ->
                val kept = lines.filter { it.productId.isBlank() || pickedIds.contains(it.productId) }.toMutableList()
                pickedIds.forEach { pid ->
                    if (kept.none { it.productId == pid }) {
                        productMap[pid]?.let { p ->
                            kept += OrderLine(
                                productId = p.id,
                                name = p.name,
                                qty = 1.0,
                                price = p.price,
                                cost = p.cost
                            )
                        }
                    }
                }
                kept += OrderLine(productId = "", name = "", qty = 1.0, price = 0L, cost = 0L)
                lines.clear()
                lines.addAll(kept)
                showPicker = false
            }
        )
        return
    }

    if (showCustomerPicker) {
        CustomerPickerScreen(
            customers = uiState.customers,
            orders = uiState.orders,
            selectedCustomerId = selectedCustomerId,
            onBack = { showCustomerPicker = false },
            onSelectCustomer = { id ->
                selectedCustomerId = id
                showCustomerPicker = false
            },
            onQuickAddCustomer = { name, phone, area, addr ->
                onQuickAddCustomer(name, phone, area, addr) { saved ->
                    selectedCustomerId = saved.id
                    showCustomerPicker = false
                }
            }
        )
        return
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Thêm đơn bán", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { attemptClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onSaveOrder(orderDate, selectedCustomerId, lines.toList(), surcharge, shipFee, note)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandBlue),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_order_top_button")
                    ) {
                        Text("💾 Lưu", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tổng tiền", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = Formatters.money(grandTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandBlue
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hàng hóa", style = MaterialTheme.typography.titleMedium)
                            FilledTonalButton(
                                onClick = { showPicker = true },
                                modifier = Modifier.testTag("open_product_picker_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" Thêm")
                            }
                        }
                        if (lines.isEmpty()) {
                            EmptyPlaceholder("Chưa có hàng hóa. Bấm “＋ Thêm” để chọn.")
                        }
                    }
                }
            }

            itemsIndexed(lines) { index, line ->
                val prod = productMap[line.productId]
                OrderLineCard(
                    index = index,
                    line = line,
                    productImage = prod?.image.orEmpty(),
                    onUpdate = { updated -> lines[index] = updated },
                    onRemove = { lines.removeAt(index) }
                )
            }

            // Customer full-screen picker trigger card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Khách hàng", style = MaterialTheme.typography.titleMedium)
                            FilledTonalButton(
                                onClick = { showCustomerPicker = true },
                                modifier = Modifier.testTag("open_customer_picker_button")
                            ) {
                                Text("Chọn khách hàng")
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomerPicker = true }
                                .testTag("customer_selector_box")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                    Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedCustomer?.name ?: "Khách lẻ / chưa lưu",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val phoneTxt = selectedCustomer?.phone?.ifBlank { "Không có" } ?: "Bấm để chọn khách hàng"
                                    val addrTxt = listOf(selectedCustomer?.area.orEmpty(), selectedCustomer?.address.orEmpty())
                                        .filter { it.isNotBlank() }.joinToString(" · ")
                                    Text(
                                        text = if (selectedCustomer != null) {
                                            "📞 $phoneTxt" + if (addrTxt.isNotBlank()) " · 🏠 $addrTxt" else ""
                                        } else phoneTxt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Chọn khách hàng",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DatePickerField(
                            label = "Ngày bán",
                            isoDate = orderDate,
                            onDateSelected = { orderDate = it }
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Ghi chú") },
                            placeholder = { Text("Giao sáng trước 9h...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MoneyInputField(
                            label = "Phụ phí (thu thêm)",
                            amount = surcharge,
                            onAmountChange = { surcharge = it }
                        )
                        MoneyInputField(
                            label = "Phí vận chuyển (trừ đi)",
                            amount = shipFee,
                            onAmountChange = { shipFee = it }
                        )
                        Spacer(Modifier.height(2.dp))
                        SummaryStatRow("Số lượng", "${Formatters.qty(qtyTotal)} kg")
                        SummaryStatRow("Tiền hàng", Formatters.money(goodsTotal))
                        SummaryStatRow("Phụ phí", "+ ${Formatters.money(surcharge)}")
                        SummaryStatRow("Tổng khách trả", Formatters.money(grandTotal))
                        SummaryStatRow("Giá vốn", Formatters.money(costTotal))
                        SummaryStatRow("Phí vận chuyển", "− ${Formatters.money(shipFee)}")
                        SummaryStatRow(
                            label = "Lợi nhuận dự kiến",
                            value = Formatters.money(estimatedProfit),
                            valueColor = if (estimatedProfit < 0) LossRed else ProfitGreen,
                            boldLabel = true
                        )
                    }
                }
            }
        }
    }

    if (askDiscard) {
        AlertDialog(
            onDismissRequest = { askDiscard = false },
            title = { Text("Hủy tạo đơn?") },
            text = { Text("Bỏ đơn hàng đang tạo?") },
            confirmButton = {
                Button(
                    onClick = { askDiscard = false; onClose() },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed, contentColor = Color.White)
                ) {
                    Text("Bỏ đơn")
                }
            },
            dismissButton = {
                TextButton(onClick = { askDiscard = false }) { Text("Tiếp tục sửa") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPickerScreen(
    customers: List<Customer>,
    orders: List<Order>,
    selectedCustomerId: String,
    onBack: () -> Unit,
    onSelectCustomer: (String) -> Unit,
    onQuickAddCustomer: (String, String, String, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CustomerFilter.ALL) }
    var showQuickAdd by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    val summaries = remember(customers, orders, filter, query) {
        val q = query.trim().lowercase()
        SalesAnalytics.buildCustomerCareList(customers, orders)
            .filter { s ->
                when (filter) {
                    CustomerFilter.ALL -> true
                    CustomerFilter.D15 -> (s.daysSinceLastOrder ?: -1) >= 15
                    CustomerFilter.D30 -> (s.daysSinceLastOrder ?: -1) >= 30
                    CustomerFilter.D60 -> (s.daysSinceLastOrder ?: -1) >= 60
                    CustomerFilter.NEVER -> s.daysSinceLastOrder == null
                }
            }
            .filter { s ->
                if (q.isEmpty()) true
                else "${s.customer.name} ${s.customer.phone} ${s.customer.area} ${s.customer.address}".lowercase().contains(q)
            }
            .sortedBy { it.customer.name.lowercase() }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Chọn khách hàng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    Button(
                        onClick = { showQuickAdd = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandBlue),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("picker_add_customer_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Thêm mới", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Lọc theo tên, SĐT, khu vực, địa chỉ...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("customer_picker_search")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomerFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.label) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isGuestSelected = selectedCustomerId.isBlank()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGuestSelected) BrandBlueSoft else Color.White,
                        border = BorderStroke(1.dp, if (isGuestSelected) BrandBlue else MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCustomer("") }
                            .testTag("pick_guest_customer")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandBlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PersonOutline, contentDescription = null, tint = BrandBlue)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Khách lẻ / chưa lưu",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bán nhanh không gắn hồ sơ khách hàng",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isGuestSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Đang chọn", tint = BrandBlue)
                            }
                        }
                    }
                }

                if (summaries.isEmpty()) {
                    item {
                        EmptyPlaceholder("Không tìm thấy khách hàng phù hợp.")
                    }
                } else {
                    items(summaries, key = { it.customer.id }) { s ->
                        val c = s.customer
                        val isSelected = c.id == selectedCustomerId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BrandBlueSoft else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) BrandBlue else MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCustomer(c.id) }
                                .testTag("pick_customer_${c.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) BrandBlue else BrandBlueSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else BrandBlue
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = c.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "📞 ${c.phone.ifBlank { "Không có" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val loc = listOf(c.area, c.address).filter { it.isNotBlank() }.joinToString(" · ")
                                    Text(
                                        text = "🏠 ${loc.ifBlank { "Không có" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${s.orders.size} đơn · ${Formatters.money(s.totalSpent)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        when {
                                            s.daysSinceLastOrder == null -> StatusTag("Chưa có đơn", isWarning = true)
                                            s.daysSinceLastOrder >= 30 -> StatusTag("${s.daysSinceLastOrder} ngày chưa nhập", isWarning = true)
                                            else -> Text(
                                                text = "${s.daysSinceLastOrder} ngày trước",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuickAdd) {
        var newName by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newArea by remember { mutableStateOf("") }
        var newAddress by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showQuickAdd = false },
            title = { Text("Thêm khách hàng mới") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Tên khách hàng *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Số điện thoại") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newArea,
                        onValueChange = { newArea = it },
                        label = { Text("Khu vực / tỉnh thành") },
                        placeholder = { Text("Từ Sơn, Bắc Ninh") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Địa chỉ chi tiết") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.trim().isNotEmpty()) {
                            onQuickAddCustomer(newName, newPhone, newArea, newAddress)
                            showQuickAdd = false
                        }
                    }
                ) {
                    Text("Lưu & chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAdd = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun OrderLineCard(
    index: Int,
    line: OrderLine,
    productImage: String,
    onUpdate: (OrderLine) -> Unit,
    onRemove: () -> Unit
) {
    var qtyText by remember(line.productId, line.qty) {
        mutableStateOf(Formatters.qty(line.qty).replace(',', '.'))
    }
    val lineTotal = (line.price.toDouble() * line.qty).roundToLong()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            ProductThumbnail(imageDataUri = productImage, size = 50.dp)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (line.productId.isNotBlank()) {
                        Text(
                            text = "${index + 1}. ${line.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        OutlinedTextField(
                            value = line.name,
                            onValueChange = { onUpdate(line.copy(name = it)) },
                            placeholder = { Text("Tên sản phẩm ngoài...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Xóa dòng", tint = LossRed)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val next = round((line.qty - 1.0) * 100.0) / 100.0
                            val safe = if (next <= 0.0) 0.5 else next
                            onUpdate(line.copy(qty = safe))
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("−", style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { txt ->
                            qtyText = txt
                            val parsed = Formatters.parseQty(txt)
                            if (parsed > 0.0) {
                                onUpdate(line.copy(qty = parsed))
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.width(68.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            val next = round((line.qty + 1.0) * 100.0) / 100.0
                            onUpdate(line.copy(qty = next))
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }

                    Text("kg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.weight(1f))
                    Text(
                        text = Formatters.money(lineTotal),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandBlue
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoneyInputField(
                        label = "Đơn giá / 1kg",
                        amount = line.price,
                        onAmountChange = { onUpdate(line.copy(price = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    if (line.productId.isBlank()) {
                        MoneyInputField(
                            label = "Giá vốn / 1kg",
                            amount = line.cost,
                            onAmountChange = { onUpdate(line.copy(cost = it)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPickerScreen(
    products: List<Product>,
    initialSelectedIds: Set<String>,
    onBack: () -> Unit,
    onApply: (Set<String>) -> Unit,
    onAddCustom: (Set<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialSelectedIds) } }

    BackHandler { onBack() }

    val filtered = remember(products, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) products else products.filter { it.name.lowercase().contains(q) }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Chọn hàng hóa", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAddCustom(selected.toSet()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("＋ Sản phẩm ngoài")
                    }
                    Button(
                        onClick = { onApply(selected.toSet()) },
                        modifier = Modifier.weight(1f).testTag("picker_confirm_button")
                    ) {
                        Text("Tiếp tục (${selected.size})")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Lọc theo tên hàng...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
            if (filtered.isEmpty()) {
                EmptyPlaceholder("Chưa có sản phẩm.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { p ->
                        val isChecked = selected.contains(p.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isChecked) BrandBlueSoft else Color.White)
                                .clickable {
                                    if (isChecked) selected.remove(p.id) else selected.add(p.id)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                ProductThumbnail(imageDataUri = p.image, size = 50.dp)
                                if (isChecked) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(BrandBlue.copy(alpha = 0.8f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Đã chọn",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = p.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Formatters.money(p.price),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandBlue
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
