package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppPage
import com.example.ui.SalesViewModel
import com.example.ui.components.LoadingOverlay
import com.example.ui.components.OrderDetailDialog
import com.example.ui.components.SyncStatusBadge
import com.example.ui.screens.CreateOrderScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueSoft
import com.example.ui.theme.MyApplicationTheme
import com.example.util.Formatters
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SalesAppRoot()
            }
        }
    }
}

@Composable
fun SalesAppRoot(viewModel: SalesViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val loadingMessage by viewModel.loadingMessage.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val reportMode by viewModel.reportMode.collectAsStateWithLifecycle()
    val reportDate by viewModel.reportDate.collectAsStateWithLifecycle()
    val customerFilter by viewModel.customerFilter.collectAsStateWithLifecycle()
    val isCreatingOrder by viewModel.isCreatingOrder.collectAsStateWithLifecycle()
    val selectedOrderId by viewModel.selectedOrderId.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    BackHandler(enabled = drawerState.isOpen || (currentPage != AppPage.DASHBOARD && !isCreatingOrder)) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            currentPage != AppPage.DASHBOARD -> viewModel.navigateTo(AppPage.DASHBOARD)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedTablet = maxWidth >= 900.dp

        val mainContent: @Composable () -> Unit = {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.statusBarsPadding()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!isExpandedTablet) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier
                                            .background(BrandBlueSoft, MaterialTheme.shapes.small)
                                            .testTag("open_drawer_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Mở menu",
                                            tint = BrandBlue
                                        )
                                    }
                                }
                                Text(
                                    text = "🤑 Kiếm được 10 tỷ sẽ nghỉ làm",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                SyncStatusBadge(
                                    status = uiState.syncStatus,
                                    onClick = { viewModel.navigateTo(AppPage.SETTINGS) }
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Page Sub-header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Formatters.todayVietnameseBanner(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = currentPage.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        if (currentPage == AppPage.DASHBOARD || currentPage == AppPage.ORDERS) {
                            Button(
                                onClick = { viewModel.openCreateOrder() },
                                modifier = Modifier.testTag("quick_add_order_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text(" Tạo đơn hàng")
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentPage) {
                            AppPage.DASHBOARD -> DashboardScreen(
                                uiState = uiState,
                                onNavigate = { viewModel.navigateTo(it) },
                                onOpenOrderDetail = { viewModel.openOrderDetail(it) }
                            )
                            AppPage.ORDERS -> OrdersScreen(
                                uiState = uiState,
                                onOrderClick = { viewModel.openOrderDetail(it.id) }
                            )
                            AppPage.CUSTOMERS -> CustomersScreen(
                                uiState = uiState,
                                filter = customerFilter,
                                onFilterChange = { viewModel.setCustomerFilter(it) },
                                onSaveCustomer = { id, name, phone, area, addr, note ->
                                    viewModel.saveCustomer(id, name, phone, area, addr, note)
                                },
                                onDeleteCustomer = { viewModel.deleteCustomer(it) }
                            )
                            AppPage.PRODUCTS -> ProductsScreen(
                                uiState = uiState,
                                onSaveProduct = { id, name, cost, price, note, img ->
                                    viewModel.saveProduct(id, name, cost, price, note, img)
                                },
                                onDeleteProduct = { viewModel.deleteProduct(it) }
                            )
                            AppPage.EXPENSES -> ExpensesScreen(
                                uiState = uiState,
                                onSaveExpense = { date, name, cat, type, amt, ordId, note ->
                                    viewModel.saveExpense(date, name, cat, type, amt, ordId, note) {}
                                },
                                onDeleteExpense = { viewModel.deleteExpense(it) }
                            )
                            AppPage.REPORTS -> ReportsScreen(
                                uiState = uiState,
                                mode = reportMode,
                                anchorDate = reportDate,
                                onModeChange = { viewModel.setReportMode(it) },
                                onAnchorDateChange = { viewModel.setReportDate(it) },
                                onShift = { viewModel.shiftReport(it) },
                                onResetToday = { viewModel.resetReportToday() },
                                onDrillToDay = { viewModel.drillReportToDay(it) },
                                onOrderClick = { viewModel.openOrderDetail(it.id) }
                            )
                            AppPage.SETTINGS -> SettingsScreen(
                                uiState = uiState,
                                onSaveConfig = { u, k -> viewModel.saveSupabaseConfig(u, k) },
                                onSignUp = { e, p -> viewModel.signUp(e, p) },
                                onSignIn = { e, p -> viewModel.signIn(e, p) },
                                onSignOut = { viewModel.signOut() },
                                onRefreshCloud = { viewModel.refreshFromCloud() },
                                onGenerateBackupJson = { viewModel.generateBackupJson() },
                                onRestoreBackupJson = { viewModel.restoreBackupJson(it) },
                                onGenerateOrdersCsv = { viewModel.generateOrdersCsv() },
                                onNotify = { viewModel.showToast(it) }
                            )
                        }
                    }
                }
            }
        }

        if (isExpandedTablet) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(260.dp)) {
                        DrawerMenuBody(
                            currentPage = currentPage,
                            onSelectPage = { viewModel.navigateTo(it) }
                        )
                    }
                },
                content = mainContent
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                        DrawerMenuBody(
                            currentPage = currentPage,
                            onSelectPage = { page ->
                                viewModel.navigateTo(page)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                },
                content = mainContent
            )
        }

        if (isCreatingOrder) {
            CreateOrderScreen(
                uiState = uiState,
                onClose = { viewModel.closeCreateOrder() },
                onQuickAddCustomer = { name, phone, area, addr, onSaved ->
                    viewModel.saveCustomer(null, name, phone, area, addr, "", onSaved)
                },
                onSaveOrder = { date, custId, lines, sur, ship, note ->
                    viewModel.saveOrder(date, custId, lines, sur, ship, note) {
                        viewModel.closeCreateOrder()
                    }
                }
            )
        }

        selectedOrderId?.let { ordId ->
            val order = uiState.orders.find { it.id == ordId }
            if (order != null) {
                val cust = uiState.customers.find { it.id == order.customerId }
                OrderDetailDialog(
                    order = order,
                    customer = cust,
                    allExpenses = uiState.expenses,
                    onDismiss = { viewModel.closeOrderDetail() },
                    onDelete = { viewModel.deleteOrder(it) }
                )
            }
        }

        LoadingOverlay(message = loadingMessage)
    }
}

@Composable
private fun DrawerMenuBody(
    currentPage: AppPage,
    onSelectPage: (AppPage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 12.dp, vertical = 18.dp)
    ) {
        Text(
            text = "🤑 Kiếm được 10 tỷ sẽ nghỉ làm",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Text(
            text = "Làm hết mình, nghỉ hưu thật sớm 😎",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(14.dp))

        AppPage.entries.forEach { p ->
            NavigationDrawerItem(
                label = { Text(p.title, fontWeight = FontWeight.SemiBold) },
                selected = currentPage == p,
                onClick = { onSelectPage(p) },
                icon = {
                    Icon(pageIcon(p), contentDescription = p.title)
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = BrandBlueSoft,
                    selectedIconColor = BrandBlue,
                    selectedTextColor = BrandBlue
                ),
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .testTag("nav_${p.name.lowercase()}")
            )
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = "💸 Mục tiêu: 10 tỷ rồi tính tiếp!",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
        )
    }
}

private fun pageIcon(page: AppPage): ImageVector = when (page) {
    AppPage.DASHBOARD -> Icons.Default.Dashboard
    AppPage.ORDERS -> Icons.Default.ReceiptLong
    AppPage.CUSTOMERS -> Icons.Default.People
    AppPage.PRODUCTS -> Icons.Default.Inventory2
    AppPage.EXPENSES -> Icons.Default.Receipt
    AppPage.REPORTS -> Icons.Default.Assessment
    AppPage.SETTINGS -> Icons.Default.Settings
}
