package ua.kpi.iot_lighting.prac3.ui.screens.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.iot_lighting.prac3.domain.model.LightDevice
import ua.kpi.iot_lighting.prac3.domain.model.Role
import ua.kpi.iot_lighting.prac3.ui.components.AddDeviceDialog
import ua.kpi.iot_lighting.prac3.ui.components.ConfirmDialog
import ua.kpi.iot_lighting.prac3.ui.components.ErrorState
import ua.kpi.iot_lighting.prac3.ui.components.LoadingState
import ua.kpi.iot_lighting.prac3.ui.components.PowerStatusText
import ua.kpi.iot_lighting.prac3.ui.components.PowerSwitch
import ua.kpi.iot_lighting.prac3.ui.theme.AmberLightPrimary
import ua.kpi.iot_lighting.prac3.ui.theme.TextMuted
import ua.kpi.iot_lighting.prac3.ui.util.formatDateTime
import ua.kpi.iot_lighting.prac3.ui.util.formatKwh
import ua.kpi.iot_lighting.prac3.ui.util.formatMoney
import ua.kpi.iot_lighting.prac3.ui.viewmodel.DeviceFilter
import ua.kpi.iot_lighting.prac3.ui.viewmodel.DeviceListUiState
import ua.kpi.iot_lighting.prac3.ui.viewmodel.DeviceListViewModel
import ua.kpi.iot_lighting.prac3.ui.viewmodel.DeviceSort

/**
 * Головний екран: рекомендація за погодою, агрегати, фільтри/сортування
 * та список світильників з кешу Room, синхронізованого з сервером.
 * Адміністратор бачить додавання/видалення (ролі з токена — ПЗ11).
 *
 * Адаптивність: портрет — одна колонка з обмеженням ширини 720 dp;
 * ландшафт — дві панелі (довідка ліворуч, список праворуч), щоб список
 * не «зникав» за високою шапкою на низьких екранах.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceListViewModel = viewModel(factory = DeviceListViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // rememberSaveable: діалоги переживають поворот екрана
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var deleteCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }
    val deleteCandidate = state.devices.find { it.id == deleteCandidateId }

    // Snackbar при помилці дії
    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissActionError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Освітлення", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    if (state.user?.role == Role.ADMIN) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Додати світильник")
                        }
                    }
                    // під час оновлення — видимий індикатор замість іконки
                    if (state.isRefreshing) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = AmberLightPrimary,
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Оновити")
                        }
                    }
                    IconButton(onClick = onOpenAbout) {
                        Icon(Icons.Default.Info, contentDescription = "Про застосунок")
                    }
                    // явна кнопка виходу з акаунта (не ховається у «Про застосунок»)
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Вийти з акаунта")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isLandscape = maxWidth > maxHeight
                val contentModifier = if (isLandscape) {
                    Modifier.fillMaxHeight().widthIn(max = 1100.dp)
                } else {
                    Modifier.fillMaxSize().widthIn(max = 720.dp)
                }

                Column(modifier = contentModifier) {
                    // позначка офлайн-кешу (ПЗ10/ПЗ12)
                    if (state.isOfflineCache) {
                        Text(
                            text = "Офлайн-дані" + (state.lastSyncAt?.let { " від ${formatDateTime(it)}" } ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberLightPrimary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    val loadError = state.loadError
                    when {
                        state.isLoading -> LoadingState(
                            "Завантаження з сервера…",
                            modifier = Modifier.fillMaxSize(),
                        )
                        loadError != null -> ErrorState(
                            message = loadError,
                            onRetry = { viewModel.refresh() },
                            modifier = Modifier.fillMaxSize(),
                        )
                        isLandscape -> LandscapeContent(
                            state = state,
                            viewModel = viewModel,
                            onDeleteRequest = { deleteCandidateId = it.id },
                        )
                        else -> PortraitContent(
                            state = state,
                            viewModel = viewModel,
                            onDeleteRequest = { deleteCandidateId = it.id },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDeviceDialog(
            onAdd = viewModel::addDevice,
            onDismiss = { showAddDialog = false },
        )
    }
    if (showLogoutDialog) {
        ConfirmDialog(
            title = "Вийти з акаунта?",
            text = "Поточний токен буде видалено — для подальшої роботи знадобиться повторний вхід.",
            confirmLabel = "Вийти",
            onConfirm = { viewModel.logout() },
            onDismiss = { showLogoutDialog = false },
        )
    }
    deleteCandidate?.let { device ->
        ConfirmDialog(
            title = "Видалити світильник?",
            text = "«${device.name}» буде видалено з сервера і кешу.",
            confirmLabel = "Видалити",
            onConfirm = { viewModel.deleteDevice(device.id) },
            onDismiss = { deleteCandidateId = null },
        )
    }
}

/** Портрет: усе в одному списку (шапка + рядки). */
@Composable
private fun PortraitContent(
    state: DeviceListUiState,
    viewModel: DeviceListViewModel,
    onDeleteRequest: (LightDevice) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { WeatherSection(state) }
        item { TotalsSection(state) }
        item { FilterSection(state, viewModel) }
        DeviceRows(state, viewModel, onDeleteRequest)
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/**
 * Ландшафт: ліва панель — погода/агрегати/фільтри, права — список.
 * Так на низькому екрані список завжди видно поряд із шапкою.
 */
@Composable
private fun LandscapeContent(
    state: DeviceListUiState,
    viewModel: DeviceListViewModel,
    onDeleteRequest: (LightDevice) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(end = 12.dp),
        ) {
            WeatherSection(state)
            TotalsSection(state)
            FilterSection(state, viewModel)
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outline)
        LazyColumn(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(start = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DeviceRows(state, viewModel, onDeleteRequest)
        }
    }
}

/** Рядки списку (або повідомлення, якщо показувати нічого). */
private fun LazyListScope.DeviceRows(
    state: DeviceListUiState,
    viewModel: DeviceListViewModel,
    onDeleteRequest: (LightDevice) -> Unit,
) {
    if (state.devices.isEmpty()) {
        item {
            Text(
                text = "Світильників немає. Оновіть дані або додайте новий.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
    } else {
        items(state.devices, key = { it.id }) { device ->
            DeviceRow(
                device = device,
                isAdmin = state.user?.role == Role.ADMIN,
                onToggle = { checked -> viewModel.toggle(device, checked) },
                onDelete = { onDeleteRequest(device) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

/** Секція погоди з рекомендацією — бізнес-логіка за даними Open-Meteo. */
@Composable
private fun WeatherSection(state: DeviceListUiState) {
    val weather = state.weather ?: return
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Денне світло: ${weather.daylightPercent}%",
            style = MaterialTheme.typography.titleSmall,
            color = AmberLightPrimary,
        )
        Text(
            text = weather.recommendation,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = "Кривий Ріг: ${weather.temperatureC.toInt()}°C, хмарність ${weather.cloudCoverPercent}%",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Агрегати: активна потужність і прогноз енергії/вартості за добу. */
@Composable
private fun TotalsSection(state: DeviceListUiState) {
    val totals = state.totals ?: return
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Увімкнено ${totals.poweredDevices} з ${totals.totalDevices} · " +
                "активна потужність ${totals.activePowerW} Вт",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Прогноз: ${formatKwh(totals.energyKwhPerDay)} · ${formatMoney(totals.costPerDay)} за добу",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Чипи фільтра й перемикачі сортування. */
@Composable
private fun FilterSection(
    state: DeviceListUiState,
    viewModel: DeviceListViewModel,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeviceFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { viewModel.setFilter(filter) },
                    label = { Text(filter.label) },
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(
                text = "Сортування:",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            DeviceSort.entries.forEach { sort ->
                TextButton(onClick = { viewModel.setSort(sort) }) {
                    Text(
                        text = sort.label,
                        color = if (state.sort == sort) AmberLightPrimary else TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/** Рядок світильника: назва, зона/потужність, статус, перемикач, видалення. */
@Composable
private fun DeviceRow(
    device: LightDevice,
    isAdmin: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "${device.zone} · ${device.powerW} Вт",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            PowerStatusText(device.isPoweredOn, modifier = Modifier.padding(top = 2.dp))
        }
        if (isAdmin) {
            TextButton(onClick = onDelete) {
                Text("Видалити", color = MaterialTheme.colorScheme.error)
            }
        }
        PowerSwitch(
            checked = device.isPoweredOn,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
