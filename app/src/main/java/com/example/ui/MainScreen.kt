package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.dimens
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    AGENDA("Agenda", Icons.Default.CalendarToday),
    CLIENTES("Clientes", Icons.Default.People),
    CATALOGO("Preços", Icons.Default.Inventory2),
    FINANCEIRO("Financeiro", Icons.Default.Paid)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PetViewModel,
    initialAppointmentId: Long? = null
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle()

    if (currentUser == null) {
        LoginScreen(viewModel = viewModel)
        return
    }

    var selectedTab by remember { mutableStateOf(AppTab.AGENDA) }

    LaunchedEffect(initialAppointmentId, allAppointments) {
        if (initialAppointmentId != null && allAppointments.isNotEmpty()) {
            val appointment = allAppointments.find { it.id == initialAppointmentId }
            if (appointment != null) {
                viewModel.setSelectedDate(appointment.dateMillis)
                selectedTab = AppTab.AGENDA
                // Could also trigger a detail view if needed
            }
        }
    }

    val showOnboardingTour by viewModel.showOnboardingTour.collectAsStateWithLifecycle()
    var tourStep by remember { mutableStateOf(0) }

    LaunchedEffect(tourStep, showOnboardingTour) {
        if (showOnboardingTour) {
            when (tourStep) {
                1 -> selectedTab = AppTab.AGENDA
                2 -> selectedTab = AppTab.CLIENTES
                3 -> selectedTab = AppTab.CATALOGO
                4 -> selectedTab = AppTab.FINANCEIRO
            }
        }
    }

    // Dialog state variables
    var showAddClientDialog by remember { mutableStateOf(false) }
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddAppointmentDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }

    // Edit states
    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    var serviceToEdit by remember { mutableStateOf<PetService?>(null) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var transactionToEdit by remember { mutableStateOf<FinancialTransaction?>(null) }
    var appointmentToEdit by remember { mutableStateOf<Appointment?>(null) }

    // Selected client for quick schedule shortcut
    var preselectedClientForAppointment by remember { mutableStateOf<Client?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(MaterialTheme.dimens.iconMedium)
                        )
                        Column {
                            Text(
                                "Agenda Pet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                currentUser?.email ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Sincronizado na Conta",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(MaterialTheme.dimens.iconSmall)
                        )
                        Text(
                            "Salvo na conta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("btn_logout")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sair",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Crossfade(targetState = selectedTab, label = "tab_fade") { tab ->
                when (tab) {
                    AppTab.AGENDA -> AgendaTab(
                        viewModel = viewModel,
                        onAddAppointmentClick = { showAddAppointmentDialog = true },
                        onEditAppointmentClick = { appointmentToEdit = it }
                    )
                    AppTab.CLIENTES -> ClientesTab(
                        viewModel = viewModel,
                        onAddClientClick = { showAddClientDialog = true },
                        onEditClientClick = { clientToEdit = it },
                        onQuickScheduleClick = {
                            preselectedClientForAppointment = it
                            showAddAppointmentDialog = true
                        }
                    )
                    AppTab.CATALOGO -> CatalogoTab(
                        viewModel = viewModel,
                        onAddServiceClick = { showAddServiceDialog = true },
                        onAddProductClick = { showAddProductDialog = true },
                        onEditServiceClick = { serviceToEdit = it },
                        onEditProductClick = { productToEdit = it }
                    )
                    AppTab.FINANCEIRO -> FinanceiroTab(
                        viewModel = viewModel,
                        onAddTransactionClick = { showAddTransactionDialog = true },
                        onEditTransactionClick = { transactionToEdit = it }
                    )
                }
            }
        }
    }

    if (showOnboardingTour) {
        OnboardingTourOverlay(
            step = tourStep,
            onNext = {
                if (tourStep < 5) {
                    tourStep++
                } else {
                    viewModel.completeOnboardingTour()
                    tourStep = 0
                }
            },
            onPrev = {
                if (tourStep > 0) {
                    tourStep--
                }
            },
            onSkip = {
                viewModel.completeOnboardingTour()
                tourStep = 0
            }
        )
    }

    // --- DIALOGS CONTROLLERS ---

    if (showAddClientDialog) {
        ClientFormDialog(
            title = "Novo Cliente",
            onDismiss = { showAddClientDialog = false },
            onSave = { name, phone, phoneSecondary, address, notes, pets ->
                val primaryPet = pets.firstOrNull()
                val mainPetName = primaryPet?.name ?: ""
                val mainPetBreed = primaryPet?.breed ?: ""
                val mainPetAge = primaryPet?.age ?: ""
                val mainPetBath = primaryPet?.bathPrice ?: 0.0
                val mainPetGroom = primaryPet?.groomingPrice ?: 0.0
                val mainPetTransport = primaryPet?.transportFee ?: 0.0
                val mainPetSize = primaryPet?.size ?: "Pequeno"
                val serializedAdditional = pets.drop(1).joinToString(";") { it.toSerializedString() }

                viewModel.addClient(
                    name = name,
                    phone = phone,
                    petName = mainPetName,
                    petBreed = mainPetBreed,
                    petAge = mainPetAge,
                    notes = notes,
                    address = address,
                    phoneSecondary = phoneSecondary,
                    additionalPets = serializedAdditional,
                    petBathPrice = mainPetBath,
                    petGroomingPrice = mainPetGroom,
                    petTransportFee = mainPetTransport,
                    petSize = mainPetSize
                )
                showAddClientDialog = false
            }
        )
    }

    clientToEdit?.let { client ->
        ClientFormDialog(
            title = "Editar Cliente",
            initialClient = client,
            onDismiss = { clientToEdit = null },
            onSave = { name, phone, phoneSecondary, address, notes, pets ->
                val primaryPet = pets.firstOrNull()
                val mainPetName = primaryPet?.name ?: ""
                val mainPetBreed = primaryPet?.breed ?: ""
                val mainPetAge = primaryPet?.age ?: ""
                val mainPetBath = primaryPet?.bathPrice ?: 0.0
                val mainPetGroom = primaryPet?.groomingPrice ?: 0.0
                val mainPetTransport = primaryPet?.transportFee ?: 0.0
                val mainPetSize = primaryPet?.size ?: "Pequeno"
                val serializedAdditional = pets.drop(1).joinToString(";") { it.toSerializedString() }

                viewModel.updateClient(
                    client.copy(
                        name = name,
                        phone = phone,
                        petName = mainPetName,
                        petBreed = mainPetBreed,
                        petAge = mainPetAge,
                        notes = notes,
                        address = address,
                        phoneSecondary = phoneSecondary,
                        additionalPets = serializedAdditional,
                        petBathPrice = mainPetBath,
                        petGroomingPrice = mainPetGroom,
                        petTransportFee = mainPetTransport,
                        petSize = mainPetSize
                    )
                )
                clientToEdit = null
            },
            onDelete = {
                viewModel.deleteClient(client)
                clientToEdit = null
            }
        )
    }

    if (showAddServiceDialog) {
        ServiceFormDialog(
            title = "Novo Serviço",
            onDismiss = { showAddServiceDialog = false },
            onSave = { name, price, description ->
                viewModel.addService(name, price, description)
                showAddServiceDialog = false
            }
        )
    }

    serviceToEdit?.let { service ->
        ServiceFormDialog(
            title = "Editar Serviço",
            initialService = service,
            onDismiss = { serviceToEdit = null },
            onSave = { name, price, description ->
                viewModel.updateService(service.copy(name = name, price = price, description = description))
                serviceToEdit = null
            },
            onDelete = {
                viewModel.deleteService(service)
                serviceToEdit = null
            }
        )
    }

    if (showAddProductDialog) {
        ProductFormDialog(
            title = "Novo Produto",
            onDismiss = { showAddProductDialog = false },
            onSave = { name, price, costPrice, stock ->
                viewModel.addProduct(name, price, costPrice, stock)
                showAddProductDialog = false
            }
        )
    }

    productToEdit?.let { product ->
        ProductFormDialog(
            title = "Editar Produto",
            initialProduct = product,
            onDismiss = { productToEdit = null },
            onSave = { name, price, costPrice, stock ->
                viewModel.updateProduct(product.copy(name = name, price = price, costPrice = costPrice, stock = stock))
                productToEdit = null
            },
            onDelete = {
                viewModel.deleteProduct(product)
                productToEdit = null
            }
        )
    }

    if (showAddAppointmentDialog) {
        AppointmentFormDialog(
            viewModel = viewModel,
            preselectedClient = preselectedClientForAppointment,
            onDismiss = {
                showAddAppointmentDialog = false
                preselectedClientForAppointment = null
            },
            onSave = { clientId, clientName, petName, serviceId, serviceName, servicePrice, date, time, paid, needsTrans, fee, payMethod, dPercent, dAmount ->
                viewModel.addAppointment(
                    clientId = clientId,
                    clientName = clientName,
                    petName = petName,
                    serviceId = serviceId,
                    serviceName = serviceName,
                    servicePrice = servicePrice,
                    dateMillis = date,
                    timeString = time,
                    paid = paid,
                    needsTransport = needsTrans,
                    transportationFee = fee,
                    paymentMethod = payMethod,
                    discount = dAmount,
                    discountPercentage = dPercent
                )
                showAddAppointmentDialog = false
                preselectedClientForAppointment = null
            }
        )
    }

    appointmentToEdit?.let { appointment ->
        AppointmentFormDialog(
            viewModel = viewModel,
            initialAppointment = appointment,
            onDismiss = { appointmentToEdit = null },
            onSave = { clientId, clientName, petName, serviceId, serviceName, servicePrice, date, time, paid, needsTrans, fee, payMethod, dPercent, dAmount ->
                viewModel.updateAppointment(
                    appointment.copy(
                        clientId = clientId,
                        clientName = clientName,
                        petName = petName,
                        serviceId = serviceId,
                        serviceName = serviceName,
                        servicePrice = servicePrice,
                        dateMillis = date,
                        timeString = time,
                        paid = paid,
                        needsTransport = needsTrans,
                        transportationFee = fee,
                        paymentMethod = payMethod,
                        discount = dAmount,
                        discountPercentage = dPercent
                    )
                )
                appointmentToEdit = null
            }
        )
    }

    if (showAddTransactionDialog) {
        TransactionFormDialog(
            title = "Nova Transação",
            onDismiss = { showAddTransactionDialog = false },
            onSave = { type, description, amount, date ->
                viewModel.addTransaction(type, description, amount, date)
                showAddTransactionDialog = false
            }
        )
    }

    transactionToEdit?.let { transaction ->
        TransactionFormDialog(
            title = "Editar Transação",
            initialTransaction = transaction,
            onDismiss = { transactionToEdit = null },
            onSave = { type, description, amount, date ->
                viewModel.updateTransaction(transaction.copy(type = type, description = description, amount = amount, dateMillis = date))
                transactionToEdit = null
            },
            onDelete = {
                viewModel.deleteTransaction(transaction)
                transactionToEdit = null
            }
        )
    }
}

// ==========================================
// 1. AGENDA TAB SCREEN
// ==========================================
@Composable
fun AgendaTab(
    viewModel: PetViewModel,
    onAddAppointmentClick: () -> Unit,
    onEditAppointmentClick: (Appointment) -> Unit
) {
    val selectedDate by viewModel.selectedDateMillis.collectAsStateWithLifecycle()
    val dailyAppointments by viewModel.appointmentsForDay.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val allServices by viewModel.services.collectAsStateWithLifecycle()
    val allProducts by viewModel.products.collectAsStateWithLifecycle()

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyTransportation by remember { mutableStateOf(false) }
    var appointmentForPaymentDialog by remember { mutableStateOf<Appointment?>(null) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    val clientAddressMap = remember(clients) { clients.associate { it.id to it.address } }

    val filteredAppointments = remember(dailyAppointments, searchQuery, clientAddressMap, showOnlyTransportation, statusFilter) {
        val baseList = if (searchQuery.isBlank()) {
            dailyAppointments
        } else {
            dailyAppointments.filter { app ->
                val address = clientAddressMap[app.clientId] ?: ""
                app.petName.contains(searchQuery, ignoreCase = true) ||
                    app.clientName.contains(searchQuery, ignoreCase = true) ||
                    address.contains(searchQuery, ignoreCase = true)
            }
        }
        val transList = if (showOnlyTransportation) {
            baseList.filter { it.needsTransport }
        } else {
            baseList
        }
        if (statusFilter != null) {
            transList.filter { it.status.equals(statusFilter, ignoreCase = true) }
        } else {
            transList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Date bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Agenda Diária",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatDateLong(selectedDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { showDatePickerDialog = true },
                modifier = Modifier.testTag("btn_select_calendar_date")
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Selecionar Data",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Horizontal Ribbon for quick date selection (last 3 days to next 7 days)
        HorizontalDateSelector(
            selectedDate = selectedDate,
            onDateSelected = { viewModel.setSelectedDate(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Financial & Stat Summary for the Day
        DailyOverviewStats(appointments = dailyAppointments)

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar and Filter Icon (3-line icon) Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("agenda_search_input"),
                placeholder = { Text("Buscar por pet, tutor ou endereço...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            IconButton(
                onClick = { showFilters = !showFilters },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (showFilters) MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .testTag("btn_toggle_filters")
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filtrar",
                    tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Opções de Filtro:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = showOnlyTransportation,
                            onClick = { showOnlyTransportation = !showOnlyTransportation },
                            label = { Text("Táxi Dog", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (showOnlyTransportation) Icons.Default.Check else Icons.Default.DirectionsCar,
                                    contentDescription = "Táxi Dog",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.testTag("filter_taxi_dog_chip")
                        )
                    }

                    val statusFilters = listOf(
                        null to "Todos os status",
                        "Agendado" to "Ainda não chegaram",
                        "Em Serviço" to "Em atendimento",
                        "Aguardando Busca" to "Aguardando busca",
                        "Finalizado" to "Entregues"
                    )
                    items(statusFilters) { (statusVal, label) ->
                        val isSelected = statusFilter == statusVal
                        FilterChip(
                            selected = isSelected,
                            onClick = { statusFilter = statusVal },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.testTag("filter_status_${statusVal ?: "all"}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Compromissos do Dia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            FilledTonalButton(
                onClick = onAddAppointmentClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_appointment_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agendar Pet", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Appointment list
        if (filteredAppointments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Nenhum resultado encontrado!" else "Nenhum banho ou tosa para hoje!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Tente buscar usando outro termo de pesquisa." else "Cadastre clientes e clique em 'Agendar Pet' para planejar o dia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredAppointments) { appointment ->
                    val matchingClient = remember(clients, appointment.clientId) {
                        clients.find { it.id == appointment.clientId }
                    }
                    AppointmentCard(
                        appointment = appointment,
                        clientPhone = matchingClient?.phone ?: "",
                        clientPhoneSecondary = matchingClient?.phoneSecondary ?: "",
                        clientAddress = matchingClient?.address ?: "",
                        onPayClick = { appointmentForPaymentDialog = appointment },
                        onCancelClick = { viewModel.cancelAppointment(appointment) },
                        onDeleteClick = { viewModel.deleteAppointment(appointment) },
                        onStatusChange = { newStatus ->
                            viewModel.updateAppointment(appointment.copy(status = newStatus))
                        },
                        allServices = allServices,
                        allProducts = allProducts,
                        onUpdateAppointment = { updated -> viewModel.updateAppointment(updated) },
                        onEditAppointmentClick = { onEditAppointmentClick(appointment) }
                    )
                }
            }
        }
    }

    if (showDatePickerDialog) {
        CustomCalendarPickerDialog(
            initialSelectedDate = selectedDate,
            onDismissRequest = { showDatePickerDialog = false },
            onDateSelected = {
                viewModel.setSelectedDate(it)
                showDatePickerDialog = false
            }
        )
    }

    appointmentForPaymentDialog?.let { app ->
        PaymentMethodDialog(
            appointment = app,
            onDismiss = { appointmentForPaymentDialog = null },
            onConfirm = { method, paid ->
                if (paid) {
                    viewModel.payAppointment(app, method)
                } else {
                    viewModel.updateAppointment(app.copy(status = "Finalizado", paid = false, paymentMethod = "Pendente", lastUpdated = System.currentTimeMillis()))
                }
                appointmentForPaymentDialog = null
            }
        )
    }
}

@Composable
fun HorizontalDateSelector(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val dates = remember {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -4)
        for (i in 0..14) {
            list.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(dates) { dateMillis ->
            val isSelected = PetViewModel.getStartOfDay(dateMillis) == PetViewModel.getStartOfDay(selectedDate)
            val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val dayNum = cal.get(Calendar.DAY_OF_MONTH).toString()
            val dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale("pt", "BR"))
                ?.uppercase()?.replace(".", "") ?: ""

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(62.dp)
                    .height(72.dp)
                    .clickable { onDateSelected(dateMillis) }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayOfWeek,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DailyOverviewStats(appointments: List<Appointment>) {
    val totalScheduled = appointments.size
    val totalDone = appointments.count { it.status == "Finalizado" }
    val totalEarned = appointments.sumOf { it.servicePrice }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalScheduled",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Agendamentos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Divider(
                modifier = Modifier
                    .height(36.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalDone",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Finalizados",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Divider(
                modifier = Modifier
                    .height(36.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatCurrency(totalEarned),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Receita Diária",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helpers for product tracking serialization/deserialization
fun parseProductsString(str: String): List<Triple<String, Double, Int>> {
    if (str.isBlank()) return emptyList()
    return str.split(";").mapNotNull { part ->
        val subparts = part.split("|")
        if (subparts.size >= 3) {
            val name = subparts[0]
            val price = subparts[1].toDoubleOrNull() ?: 0.0
            val qty = subparts[2].toIntOrNull() ?: 0
            Triple(name, price, qty)
        } else null
    }
}

fun serializeProductsList(list: List<Triple<String, Double, Int>>): String {
    return list.filter { it.third > 0 }.joinToString(";") { "${it.first}|${it.second}|${it.third}" }
}

@Composable
fun EditAppointmentServicesProductsDialog(
    appointment: Appointment,
    allServices: List<PetService>,
    allProducts: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (updatedAppointment: Appointment) -> Unit
) {
    // Determine initially selected services
    val initialSelectedServiceIds = remember(appointment, allServices) {
        val names = appointment.serviceName.split(", ").map { it.trim().lowercase() }
        allServices.filter { it.name.trim().lowercase() in names }.map { it.id }.toMutableStateList()
    }

    // Determine initially selected products
    val parsedProducts = remember(appointment) { parseProductsString(appointment.productsList) }
    val productQuantities = remember(allProducts, parsedProducts) {
        val map = mutableStateMapOf<Long, Int>()
        allProducts.forEach { map[it.id] = 0 }
        parsedProducts.forEach { triple ->
            val name = triple.first
            val qty = triple.third
            val foundProd = allProducts.find { it.name.trim().lowercase() == name.trim().lowercase() }
            if (foundProd != null) {
                map[foundProd.id] = qty
            }
        }
        map
    }

    var needsTransport by remember { mutableStateOf(appointment.needsTransport) }
    var transportationFee by remember { mutableStateOf(appointment.transportationFee.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Alterar Procedimentos & Produtos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Services (Procedures)
                item {
                    Text(
                        text = "Procedimentos (Serviços):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                items(allServices) { service ->
                    val isChecked = initialSelectedServiceIds.contains(service.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) {
                                    if (initialSelectedServiceIds.size > 1) {
                                        initialSelectedServiceIds.remove(service.id)
                                    }
                                } else {
                                    initialSelectedServiceIds.add(service.id)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    initialSelectedServiceIds.add(service.id)
                                } else {
                                    if (initialSelectedServiceIds.size > 1) {
                                        initialSelectedServiceIds.remove(service.id)
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(service.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(formatCurrency(service.price), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }

                // Section 2: Products sold by shop
                item {
                    Text(
                        text = "Venda de Produtos (Perfumes, acessórios etc.):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (allProducts.isEmpty()) {
                    item {
                        Text("Nenhum produto cadastrado no estoque.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(allProducts) { prod ->
                        val qty = productQuantities[prod.id] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${formatCurrency(prod.price)} | Estoque: ${prod.stock}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (qty > 0) productQuantities[prod.id] = qty - 1 },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Diminuir", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "$qty",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { productQuantities[prod.id] = qty + 1 },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Aumentar", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }

                // Section 3: Transportation (Taxi Dog)
                item {
                    Text(
                        text = "Transporte (Táxi Dog):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { needsTransport = !needsTransport }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = needsTransport,
                            onCheckedChange = { needsTransport = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Necessita de Transporte (Busca/Entrega)", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (needsTransport) {
                    item {
                        OutlinedTextField(
                            value = transportationFee,
                            onValueChange = { transportationFee = it },
                            label = { Text("Taxa de Transporte (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Summary calculations
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    val servicesSum by remember {
                        derivedStateOf { allServices.filter { initialSelectedServiceIds.contains(it.id) }.sumOf { it.price } }
                    }
                    val productsSum by remember {
                        derivedStateOf { allProducts.sumOf { (productQuantities[it.id] ?: 0) * it.price } }
                    }
                    val currentFee = transportationFee.toDoubleOrNull() ?: 0.0
                    val totalSum = servicesSum + productsSum + (if (needsTransport) currentFee else 0.0)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Text("Resumo de Valores:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Procedimentos:", style = MaterialTheme.typography.bodySmall)
                            Text(formatCurrency(servicesSum), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        if (productsSum > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Produtos:", style = MaterialTheme.typography.bodySmall)
                                Text(formatCurrency(productsSum), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (needsTransport) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Táxi Dog (Busca/Entrega):", style = MaterialTheme.typography.bodySmall)
                                Text(formatCurrency(currentFee), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Valor Final:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(formatCurrency(totalSum), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        confirmButton = {
            val selectedServices = allServices.filter { initialSelectedServiceIds.contains(it.id) }
            val combinedName = selectedServices.joinToString(", ") { it.name }
            val servicesSum = selectedServices.sumOf { it.price }
            
            val selectedProdsList = allProducts.mapNotNull { prod ->
                val q = productQuantities[prod.id] ?: 0
                if (q > 0) {
                    Triple(prod.name, prod.price, q)
                } else null
            }
            val productsSum = selectedProdsList.sumOf { it.second * it.third }
            val serializedProds = serializeProductsList(selectedProdsList)
            val currentFee = transportationFee.toDoubleOrNull() ?: 0.0
            
            // Recalculate discount if it was percentage based
            val subTotal = servicesSum + productsSum + currentFee
            val newDiscount = if (appointment.discountPercentage > 0) {
                (subTotal * appointment.discountPercentage) / 100.0
            } else {
                appointment.discount
            }

            val updatedApp = appointment.copy(
                serviceId = selectedServices.firstOrNull()?.id ?: appointment.serviceId,
                serviceName = combinedName,
                servicePrice = servicesSum,
                productsPrice = productsSum,
                productsList = serializedProds,
                needsTransport = needsTransport,
                transportationFee = currentFee,
                discount = newDiscount,
                lastUpdated = System.currentTimeMillis()
            )

            TextButton(
                onClick = {
                    onConfirm(updatedApp)
                }
            ) {
                Text("Confirmar")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    clientPhone: String,
    clientPhoneSecondary: String,
    clientAddress: String,
    onPayClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    allServices: List<PetService>,
    allProducts: List<Product>,
    onUpdateAppointment: (Appointment) -> Unit,
    onEditAppointmentClick: () -> Unit
) {
    var expandedActions by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    var behaviorRating by remember { mutableStateOf(appointment.behaviorRating) }
    var behaviorObs by remember { mutableStateOf(appointment.behaviorObservation) }
    var professionalNotes by remember { mutableStateOf(appointment.professionalNotes) }

    LaunchedEffect(appointment) {
        behaviorRating = appointment.behaviorRating
        behaviorObs = appointment.behaviorObservation
        professionalNotes = appointment.professionalNotes
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (appointment.status) {
                "Cancelado" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                "Finalizado" -> MaterialTheme.colorScheme.surface
                "Em Serviço" -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when (appointment.status) {
                    "Cancelado" -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    "Finalizado" -> Color(0xFF2E7D32).copy(alpha = 0.4f)
                    "Em Serviço" -> Color(0xFFF57F17).copy(alpha = 0.4f)
                    "Aguardando Busca" -> Color(0xFF006064).copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                }
            )
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimens.smallPadding)
            .clickable { expandedActions = !expandedActions }
            .testTag("appointment_item_${appointment.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = appointment.petName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (appointment.status == "Cancelado") TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(
                            text = "Dono: ${appointment.clientName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = appointment.timeString,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(status = appointment.status, paid = appointment.paid, paymentMethod = appointment.paymentMethod)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = appointment.serviceName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatCurrency(appointment.servicePrice),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (appointment.productsPrice > 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Produtos adicionais",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatCurrency(appointment.productsPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (appointment.needsTransport) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Serviço de Busca/Entrega (Táxi Dog)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatCurrency(appointment.transportationFee),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Show combined total if products exist OR transportation exists OR discount exists
            if (appointment.needsTransport || appointment.productsPrice > 0.0 || appointment.discount > 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                
                val subTotal = appointment.servicePrice + appointment.productsPrice + (if (appointment.needsTransport) appointment.transportationFee else 0.0)
                val finalTotal = subTotal - appointment.discount

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    if (appointment.discount > 0.0) {
                        Text(
                            text = "Subtotal: ${formatCurrency(subTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Text(
                            text = "Desconto: - ${formatCurrency(appointment.discount)} (${appointment.discountPercentage}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    }
                    Text(
                        text = "Total Geral: ${formatCurrency(finalTotal)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (appointment.behaviorRating >= 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Nota de comportamento",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFBC02D)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Comportamento: ${appointment.behaviorRating}/10" + if (appointment.behaviorObservation.isNotBlank()) " - ${appointment.behaviorObservation}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Expanded action options
            if (expandedActions) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // Informações de Contato do Tutor
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Contato do Tutor",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val phoneText = if (clientPhoneSecondary.isNotBlank()) {
                            "$clientPhone / $clientPhoneSecondary"
                        } else {
                            clientPhone
                        }
                        if (phoneText.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = phoneText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp).padding(top = 2.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (clientAddress.isNotBlank()) clientAddress else "Sem endereço cadastrado",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = if (clientAddress.isBlank()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                color = if (clientAddress.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // EDIT SERVICES & PRODUCTS BUTTON
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Produtos/Serviços", fontSize = 11.sp)
                    }
                    
                    Button(
                        onClick = onEditAppointmentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("edit_full_appointment_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar Dados", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // BEHAVIOR OBSERVATIONS & RATINGS FORM
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Avaliação de Comportamento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Nota de Comportamento (0 a 10):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items((0..10).toList()) { score ->
                                val isSel = behaviorRating == score
                                FilterChip(
                                    selected = isSel,
                                    onClick = { behaviorRating = score },
                                    label = { Text("$score", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = behaviorObs,
                            onValueChange = { behaviorObs = it },
                            label = { Text("Comportamento no Banho/Tosa", fontSize = 12.sp) },
                            placeholder = { Text("Ex: Ficou agitado na hora do soprador", fontSize = 12.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = professionalNotes,
                            onValueChange = { professionalNotes = it },
                            label = { Text("Observações Gerais do Profissional", fontSize = 12.sp) },
                            placeholder = { Text("Ex: Secar com ar morno", fontSize = 12.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    val updated = appointment.copy(
                                        behaviorRating = behaviorRating,
                                        behaviorObservation = behaviorObs,
                                        professionalNotes = professionalNotes
                                    )
                                    onUpdateAppointment(updated)
                                }
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salvar Observações", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (appointment.status != "Cancelado") {
                    Text(
                        text = "Alterar Status de Atendimento:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Esperando Chegar Button
                        val isEsperando = appointment.status == "Agendado"
                        OutlinedButton(
                            onClick = { onStatusChange("Agendado") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isEsperando) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (isEsperando) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isEsperando) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text("Esperando", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Em Serviço Button
                        val isEmServico = appointment.status == "Em Serviço"
                        OutlinedButton(
                            onClick = { onStatusChange("Em Serviço") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isEmServico) Color(0xFFFFF9C4) else Color.Transparent,
                                contentColor = if (isEmServico) Color(0xFFF57F17) else MaterialTheme.colorScheme.primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isEmServico) Color(0xFFF57F17) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text("Em Serviço", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Aguardando Busca Button
                        val isAguardandoBusca = appointment.status == "Aguardando Busca"
                        OutlinedButton(
                            onClick = { onStatusChange("Aguardando Busca") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isAguardandoBusca) Color(0xFFE0F7FA) else Color.Transparent,
                                contentColor = if (isAguardandoBusca) Color(0xFF006064) else MaterialTheme.colorScheme.primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAguardandoBusca) Color(0xFF006064) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text("Aguardando Busca", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // Entregue / Finalizar Button
                        val isEntregue = appointment.status == "Finalizado"
                        OutlinedButton(
                            onClick = { onPayClick() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isEntregue) Color(0xFFE8F5E9) else Color.Transparent,
                                contentColor = if (isEntregue) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isEntregue) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text("Entregue", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onDeleteClick()
                            expandedActions = false
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Agendamento", tint = MaterialTheme.colorScheme.error)
                    }

                    Row {
                        if (appointment.status != "Cancelado" && appointment.status != "Finalizado") {
                            OutlinedButton(
                                onClick = {
                                    onCancelClick()
                                    expandedActions = false
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                )
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancelar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditAppointmentServicesProductsDialog(
            appointment = appointment,
            allServices = allServices,
            allProducts = allProducts,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedApp ->
                onUpdateAppointment(updatedApp)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun StatusBadge(status: String, paid: Boolean, paymentMethod: String = "") {
    val bgColor = when {
        status == "Cancelado" -> Color.LightGray.copy(alpha = 0.4f)
        status == "Finalizado" && paid -> Color(0xFFE8F5E9)
        status == "Finalizado" && !paid -> Color(0xFFFFF3E0)
        status == "Em Serviço" -> Color(0xFFFFF9C4)
        status == "Aguardando Busca" -> Color(0xFFE0F7FA)
        else -> Color(0xFFE3F2FD)
    }

    val textColor = when {
        status == "Cancelado" -> Color.Gray
        status == "Finalizado" && paid -> Color(0xFF2E7D32)
        status == "Finalizado" && !paid -> Color(0xFFE65100)
        status == "Em Serviço" -> Color(0xFFF57F17)
        status == "Aguardando Busca" -> Color(0xFF006064)
        else -> Color(0xFF1565C0)
    }

    val label = when {
        status == "Cancelado" -> "Cancelado"
        status == "Finalizado" && paid -> {
            if (paymentMethod.isNotEmpty()) "Entregue (Pago - $paymentMethod)" else "Entregue & Pago"
        }
        status == "Finalizado" && !paid -> "Entregue (Pendente)"
        status == "Em Serviço" -> "Em Serviço"
        status == "Aguardando Busca" -> "Aguardando Busca"
        else -> "Esperando Chegar"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// ==========================================
// 2. CLIENTES TAB SCREEN
// ==========================================
@Composable
fun ClientesTab(
    viewModel: PetViewModel,
    onAddClientClick: () -> Unit,
    onEditClientClick: (Client) -> Unit,
    onQuickScheduleClick: (Client) -> Unit
) {
    val clientsList by viewModel.clients.collectAsStateWithLifecycle()
    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle()
    var searchTxt by remember { mutableStateOf("") }
    var clientForHistoryDialog by remember { mutableStateOf<Client?>(null) }

    val filteredClients = remember(clientsList, searchTxt) {
        if (searchTxt.isEmpty()) clientsList
        else {
            clientsList.filter { client ->
                client.name.contains(searchTxt, ignoreCase = true) ||
                client.address.contains(searchTxt, ignoreCase = true) ||
                client.phone.contains(searchTxt, ignoreCase = true) ||
                client.phoneSecondary.contains(searchTxt, ignoreCase = true) ||
                client.getFullPetList().any { pet ->
                    pet.name.contains(searchTxt, ignoreCase = true) ||
                    pet.breed.contains(searchTxt, ignoreCase = true)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.dimens.mediumPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.dimens.mediumPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Base de Clientes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${clientsList.size} cadastros no total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAddClientClick,
                modifier = Modifier.testTag("btn_add_client")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Novo")
            }
        }

        OutlinedTextField(
            value = searchTxt,
            onValueChange = { searchTxt = it },
            placeholder = { Text("Buscar cliente, pet ou raça...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchTxt.isNotEmpty()) {
                    IconButton(onClick = { searchTxt = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("client_search_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredClients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchTxt.isEmpty()) "Nenhum cliente cadastrado ainda" else "Nenhum cliente encontrado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchTxt.isEmpty()) "Clique em 'Novo' no topo para registrar seu primeiro cliente!" else "Tente buscar com termos diferentes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredClients) { client ->
                    ClientCard(
                        client = client,
                        onEditClick = { onEditClientClick(client) },
                        onQuickScheduleClick = { onQuickScheduleClick(client) },
                        onHistoryClick = { clientForHistoryDialog = client }
                    )
                }
            }
        }
    }

    if (clientForHistoryDialog != null) {
        PetHistoryDialog(
            client = clientForHistoryDialog!!,
            allAppointments = allAppointments,
            onDismiss = { clientForHistoryDialog = null }
        )
    }
}

@Composable
fun PetHistoryDialog(
    client: Client,
    allAppointments: List<Appointment>,
    onDismiss: () -> Unit
) {
    val clientPets = remember(client) { client.getFullPetList() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Histórico: ${client.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            if (clientPets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum cachorro cadastrado para este cliente.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                var selectedPetIndex by remember { mutableStateOf(0) }
                if (selectedPetIndex >= clientPets.size) {
                    selectedPetIndex = 0
                }
                val currentPet = clientPets[selectedPetIndex]

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Dog selector chips if multiple dogs
                    if (clientPets.size > 1) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(clientPets.size) { index ->
                                val pet = clientPets[index]
                                FilterChip(
                                    selected = selectedPetIndex == index,
                                    onClick = { selectedPetIndex = index },
                                    label = { Text(pet.name, fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Pets, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(currentPet.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Get all appointments for this currentPet
                    val petAppointments = remember(allAppointments, currentPet.name) {
                        allAppointments.filter { 
                            it.clientId == client.id && 
                            it.petName.trim().lowercase() == currentPet.name.trim().lowercase()
                        }.sortedWith(compareByDescending<Appointment> { it.dateMillis }.thenByDescending { it.timeString })
                    }

                    if (petAppointments.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhum atendimento registrado para ${currentPet.name}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val lastFinished = remember(petAppointments) {
                            petAppointments.find { it.status == "Finalizado" } ?: petAppointments.first()
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Highlights section
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Resumo do Último Atendimento", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        val dateStr = sdf.format(Date(lastFinished.dateMillis))
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Última visita:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            Text("$dateStr às ${lastFinished.timeString}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Procedimento:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            Text(lastFinished.serviceName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                        if (lastFinished.productsPrice > 0.0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Produtos adicionais:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                                Text(formatCurrency(lastFinished.productsPrice), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (lastFinished.behaviorRating >= 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Nota Comportamento:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("${lastFinished.behaviorRating}/10", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                                }
                                            }
                                            if (lastFinished.behaviorObservation.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Obs Comportamento: ${lastFinished.behaviorObservation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text("Todos os Atendimentos (${petAppointments.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }

                            items(petAppointments) { app ->
                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val dateStr = sdf.format(Date(app.dateMillis))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("$dateStr - ${app.timeString}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            StatusBadge(status = app.status, paid = app.paid)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(app.serviceName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            val finalAmount = app.servicePrice + app.productsPrice + (if (app.needsTransport) app.transportationFee else 0.0)
                                            Text(formatCurrency(finalAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }

                                        if (app.behaviorRating >= 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Nota: ${app.behaviorRating}/10" + if (app.behaviorObservation.isNotBlank()) " - ${app.behaviorObservation}" else "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                        
                                        if (app.professionalNotes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Obs Profissional: ${app.professionalNotes}",
                                                style = MaterialTheme.typography.bodySmall,
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ClientCard(
    client: Client,
    onEditClick: () -> Unit,
    onQuickScheduleClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("client_item_${client.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = client.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = client.phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (client.phoneSecondary.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${client.phoneSecondary} (Contato 2)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = onHistoryClick,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Ver Histórico do Cachorro", modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Cliente", modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onQuickScheduleClick,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Agendar este cliente", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Pet Info Cards
            val petsList = client.getFullPetList()
            if (petsList.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    petsList.forEach { pet ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Pets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pet.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (pet.age.isNotEmpty()) {
                                        Text(
                                            text = " (${pet.age})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "Raça: ${pet.breed} | Porte: ${pet.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val prices = mutableListOf<String>()
                                if (pet.bathPrice > 0.0) prices.add("Banho: ${formatCurrency(pet.bathPrice)}")
                                if (pet.groomingPrice > 0.0) prices.add("Tosa: ${formatCurrency(pet.groomingPrice)}")
                                if (pet.transportFee > 0.0) prices.add("Busca: ${formatCurrency(pet.transportFee)}")
                                if (prices.isNotEmpty()) {
                                    Text(
                                        text = prices.joinToString(" | "),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (client.address.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Endereço",
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Endereço: ${client.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (client.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = client.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. CATALOGO TAB SCREEN (PREÇOS)
// ==========================================
@Composable
fun CatalogoTab(
    viewModel: PetViewModel,
    onAddServiceClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onEditServiceClick: (PetService) -> Unit,
    onEditProductClick: (Product) -> Unit
) {
    val services by viewModel.services.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Services, 1 = Products

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.dimens.mediumPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.dimens.mediumPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Tabela de Preços",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Precifique seus produtos e serviços",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    if (selectedSubTab == 0) onAddServiceClick() else onAddProductClick()
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adicionar")
            }
        }

        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.Transparent,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Serviços (Banho/Tosa)", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Produtos à Venda", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedSubTab == 0) {
            if (services.isEmpty()) {
                EmptyCatalogState("Nenhum serviço registrado", "Registre serviços como 'Banho Simples' ou 'Tosa Completa' com seus preços correspondentes.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(services) { service ->
                        ServiceCard(service = service, onClick = { onEditServiceClick(service) })
                    }
                }
            }
        } else {
            if (products.isEmpty()) {
                EmptyCatalogState("Nenhum produto em estoque", "Registre acessórios, shampoos e outros produtos para acompanhamento de vendas.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(products) { product ->
                        ProductCard(product = product, onClick = { onEditProductClick(product) })
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCatalogState(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Default.Category,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ServiceCard(service: PetService, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (service.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = service.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = formatCurrency(service.price),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stock Tag
                    val stockColor = when {
                        product.stock == 0 -> Color(0xFFC62828)
                        product.stock <= 3 -> Color(0xFFE65100)
                        else -> Color(0xFF2E7D32)
                    }
                    val stockBgColor = when {
                        product.stock == 0 -> Color(0xFFFFEBEE)
                        product.stock <= 3 -> Color(0xFFFFF3E0)
                        else -> Color(0xFFE8F5E9)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(stockBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Estoque: ${product.stock} un",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = stockColor
                        )
                    }

                    if (product.costPrice > 0.0) {
                        Text(
                            text = "Custo: ${formatCurrency(product.costPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(product.price),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (product.costPrice > 0.0) {
                    val markup = ((product.price - product.costPrice) / product.costPrice) * 100
                    Text(
                        text = "Lucro: +${String.format("%.0f", markup)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. FINANCEIRO TAB SCREEN
// ==========================================
fun getStartOfTodayMillis(): Long = PetViewModel.getStartOfDay(System.currentTimeMillis())
fun getEndOfTodayMillis(): Long = PetViewModel.getEndOfDay(System.currentTimeMillis())

fun getStartOfSevenDaysAgoMillis(): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = PetViewModel.getStartOfDay(System.currentTimeMillis())
    cal.add(Calendar.DAY_OF_YEAR, -6)
    return cal.timeInMillis
}

fun getStartOfThisMonthMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    return PetViewModel.getStartOfDay(cal.timeInMillis)
}

fun getEndOfThisMonthMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    return PetViewModel.getEndOfDay(cal.timeInMillis)
}

@Composable
fun BreakdownRow(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Text(formatCurrency(amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

fun formatShortDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

@Composable
fun FinanceiroTab(
    viewModel: PetViewModel,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (FinancialTransaction) -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf("Este Mês") } // "Hoje", "7 Dias", "Este Mês", "Todos", "Personalizado"
    var selectedPaymentMethod by remember { mutableStateOf("Todos") } // "Todos", "Pix", "Crédito", "Débito", "Dinheiro"
    var customStartDate by remember { mutableStateOf(getStartOfThisMonthMillis()) }
    var customEndDate by remember { mutableStateOf(getEndOfTodayMillis()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val activeRange = remember(selectedPeriod, customStartDate, customEndDate) {
        when (selectedPeriod) {
            "Hoje" -> Pair(getStartOfTodayMillis(), getEndOfTodayMillis())
            "7 Dias" -> Pair(getStartOfSevenDaysAgoMillis(), getEndOfTodayMillis())
            "Este Mês" -> Pair(getStartOfThisMonthMillis(), getEndOfThisMonthMillis())
            "Personalizado" -> Pair(PetViewModel.getStartOfDay(customStartDate), PetViewModel.getEndOfDay(customEndDate))
            else -> Pair(0L, Long.MAX_VALUE) // "Todos"
        }
    }
    val startMillis = activeRange.first
    val endMillis = activeRange.second

    val filteredTransactions = remember(transactions, startMillis, endMillis, selectedPaymentMethod) {
        transactions.filter { 
            val inDate = it.dateMillis in startMillis..endMillis
            val inMethod = if (selectedPaymentMethod == "Todos") true else it.paymentMethod == selectedPaymentMethod
            inDate && inMethod
        }
    }

    val filteredAppointments = remember(allAppointments, startMillis, endMillis, selectedPaymentMethod) {
        allAppointments.filter { 
            val inDate = it.paid && it.status != "Cancelado" && it.dateMillis in startMillis..endMillis
            val inMethod = if (selectedPaymentMethod == "Todos") true else it.paymentMethod == selectedPaymentMethod
            inDate && inMethod
        }
    }

    // Calculations based on filtered transactions
    val totalRevenue = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "RECEITA" }.sumOf { it.amount }
    }
    val totalExpenses = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "DESPESA" }.sumOf { it.amount }
    }
    val netProfit = totalRevenue - totalExpenses

    // Breakdown math
    val faturamentoServicos = remember(filteredAppointments) {
        filteredAppointments.sumOf { it.servicePrice }
    }
    val faturamentoBusca = remember(filteredAppointments) {
        filteredAppointments.sumOf { if (it.needsTransport) it.transportationFee else 0.0 }
    }
    val faturamentoProdutos = remember(filteredAppointments) {
        filteredAppointments.sumOf { it.productsPrice }
    }
    
    // Manual / other revenue = Total revenue - (Servicos + Busca + Produtos from appointments)
    val totalAppointmentRevenue = faturamentoServicos + faturamentoBusca + faturamentoProdutos
    val faturamentoOutros = remember(totalRevenue, totalAppointmentRevenue) {
        val diff = totalRevenue - totalAppointmentRevenue
        if (diff > 0.0) diff else 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.dimens.mediumPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.dimens.mediumPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Controle Financeiro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Gestão de caixa e faturamento",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAddTransactionClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.testTag("btn_add_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Transação")
            }
        }

        // Period Filters Header
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val periods = listOf("Hoje", "7 Dias", "Este Mês", "Todos", "Personalizado")
            items(periods) { p ->
                FilterChip(
                    selected = selectedPeriod == p,
                    onClick = { selectedPeriod = p },
                    label = { Text(p, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Custom Date Pickers
        if (selectedPeriod == "Personalizado") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("De: ${formatShortDate(customStartDate)}", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Até: ${formatShortDate(customEndDate)}", fontSize = 11.sp)
                }
            }
        }

        // Payment Method Filter Header
        Text(
            text = "Filtrar por Pagamento:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val methods = listOf("Todos", "Pix", "Crédito", "Débito", "Dinheiro")
            items(methods) { m ->
                FilterChip(
                    selected = selectedPaymentMethod == m,
                    onClick = { selectedPaymentMethod = m },
                    label = { Text(m, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = if (selectedPaymentMethod == m) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }
        }

        // Scrollable content area for reports
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dashboard summary box
            item {
                FinancialDashboardCard(
                    revenue = totalRevenue,
                    expenses = totalExpenses,
                    profit = netProfit
                )
            }

            // Detailed revenue breakdown
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Faturamento por Categoria",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        BreakdownRow(
                            label = "Serviços (Banho e Tosa)",
                            amount = faturamentoServicos,
                            icon = Icons.Default.Pets,
                            iconColor = Color(0xFF1565C0)
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 6.dp))
                        
                        BreakdownRow(
                            label = "Busca/Entrega (Táxi Dog)",
                            amount = faturamentoBusca,
                            icon = Icons.Default.DirectionsCar,
                            iconColor = Color(0xFFF57F17)
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 6.dp))
                        
                        BreakdownRow(
                            label = "Vendas de Produtos",
                            amount = faturamentoProdutos,
                            icon = Icons.Default.ShoppingBag,
                            iconColor = Color(0xFF2E7D32)
                        )
                        
                        if (faturamentoOutros > 0.0) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 6.dp))
                            BreakdownRow(
                                label = "Outras Receitas Manuais",
                                amount = faturamentoOutros,
                                icon = Icons.Default.Paid,
                                iconColor = Color(0xFF006064)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), thickness = 2.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Faturamento Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formatCurrency(totalRevenue),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Transações no Período (${filteredTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma transação neste período.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredTransactions) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onClick = { onEditTransactionClick(transaction) }
                    )
                }
            }
        }
    }

    if (showStartPicker) {
        CustomCalendarPickerDialog(
            initialSelectedDate = customStartDate,
            onDismissRequest = { showStartPicker = false },
            onDateSelected = {
                customStartDate = it
                showStartPicker = false
            }
        )
    }

    if (showEndPicker) {
        CustomCalendarPickerDialog(
            initialSelectedDate = customEndDate,
            onDismissRequest = { showEndPicker = false },
            onDateSelected = {
                customEndDate = it
                showEndPicker = false
            }
        )
    }
}

@Composable
fun FinancialDashboardCard(revenue: Double, expenses: Double, profit: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Saldo Líquido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(profit),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = if (profit >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Entries Box
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Entradas", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32))
                        Text(formatCurrency(revenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Outputs Box
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFEBEE))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFC62828))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Saídas", style = MaterialTheme.typography.labelMedium, color = Color(0xFFC62828))
                        Text(formatCurrency(expenses), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: FinancialTransaction, onClick: () -> Unit) {
    val isRevenue = transaction.type == "RECEITA"
    val colorAccent = if (isRevenue) Color(0xFF2E7D32) else Color(0xFFC62828)
    val bgColor = if (isRevenue) Color(0xFFE8F5E9).copy(alpha = 0.5f) else Color(0xFFFFEBEE).copy(alpha = 0.5f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRevenue) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = colorAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (transaction.discount > 0.0) {
                        val original = transaction.amount + transaction.discount
                        Text(
                            text = "Valor original: ${formatCurrency(original)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Desconto: - ${formatCurrency(transaction.discount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC62828)
                        )
                    }
                    Text(
                        text = formatDate(transaction.dateMillis) + if (transaction.paymentMethod.isNotEmpty()) " • ${transaction.paymentMethod}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${if (isRevenue) "+" else "-"} ${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colorAccent
            )
        }
    }
}

// ==========================================
// FORMS AND DIALOGS
// ==========================================

@Composable
fun ClientFormDialog(
    title: String,
    initialClient: Client? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, phoneSecondary: String, address: String, notes: String, pets: List<AdditionalPet>) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialClient?.name ?: "") }
    var phone by remember { mutableStateOf(initialClient?.phone ?: "") }
    var phoneSecondary by remember { mutableStateOf(initialClient?.phoneSecondary ?: "") }
    var notes by remember { mutableStateOf(initialClient?.notes ?: "") }
    var address by remember { mutableStateOf(initialClient?.address ?: "") }

    // Read full list of pets initially
    val initialPets = remember(initialClient) {
        initialClient?.let { client ->
            val list = mutableListOf<AdditionalPet>()
            if (client.petName.isNotBlank()) {
                list.add(
                    AdditionalPet(
                        name = client.petName,
                        breed = client.petBreed,
                        age = client.petAge,
                        bathPrice = client.petBathPrice,
                        groomingPrice = client.petGroomingPrice,
                        transportFee = client.petTransportFee,
                        size = client.petSize
                    )
                )
            }
            if (client.additionalPets.isNotBlank()) {
                client.additionalPets.split(";").forEach { petStr ->
                    if (petStr.isNotBlank()) {
                        AdditionalPet.fromSerializedString(petStr)?.let { list.add(it) }
                    }
                }
            }
            list
        } ?: emptyList()
    }

    val petsList = remember { mutableStateListOf<AdditionalPet>().apply { addAll(initialPets) } }

    // New pet form state
    var newPetName by remember { mutableStateOf("") }
    var newPetBreed by remember { mutableStateOf("") }
    var newPetAge by remember { mutableStateOf("") }
    var newPetBathPrice by remember { mutableStateOf("") }
    var newPetGroomingPrice by remember { mutableStateOf("") }
    var newPetTransportFee by remember { mutableStateOf("") }
    var newPetSize by remember { mutableStateOf("Pequeno") }
    var editingPetIndex by remember { mutableStateOf<Int?>(null) }

    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { /* Não fecha ao clicar fora ou voltar */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Owner details
                Text(
                    text = "Dados do Tutor / Dono",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Dono") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("client_owner_name")
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / WhatsApp Principal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("client_owner_phone")
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phoneSecondary,
                    onValueChange = { phoneSecondary = it },
                    label = { Text("Contato Secundário (Opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("client_owner_phone_secondary")
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Endereço Completo") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("client_owner_address")
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Pets section
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Cachorros Cadastrados (${petsList.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (petsList.isEmpty()) {
                    Text(
                        text = "Nenhum cachorro cadastrado ainda. Adicione pelo menos um abaixo!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        petsList.forEachIndexed { index, pet ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Pets,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pet.name,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Raça: ${pet.breed}" + (if (pet.age.isNotBlank()) " | Idade: ${pet.age}" else "") + " | Porte: ${pet.size}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val pricingParts = mutableListOf<String>()
                                        if (pet.bathPrice > 0.0) pricingParts.add("Banho: ${formatCurrency(pet.bathPrice)}")
                                        if (pet.groomingPrice > 0.0) pricingParts.add("Tosa: ${formatCurrency(pet.groomingPrice)}")
                                        if (pet.transportFee > 0.0) pricingParts.add("Busca: ${formatCurrency(pet.transportFee)}")
                                        if (pricingParts.isNotEmpty()) {
                                            Text(
                                                text = pricingParts.joinToString(" | "),
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            editingPetIndex = index
                                            newPetName = pet.name
                                            newPetBreed = pet.breed
                                            newPetAge = pet.age
                                            newPetBathPrice = if (pet.bathPrice > 0.0) pet.bathPrice.toString() else ""
                                            newPetGroomingPrice = if (pet.groomingPrice > 0.0) pet.groomingPrice.toString() else ""
                                            newPetTransportFee = if (pet.transportFee > 0.0) pet.transportFee.toString() else ""
                                            newPetSize = pet.size
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Editar pet",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (editingPetIndex == index) {
                                                editingPetIndex = null
                                                newPetName = ""
                                                newPetBreed = ""
                                                newPetAge = ""
                                                newPetBathPrice = ""
                                                newPetGroomingPrice = ""
                                                newPetTransportFee = ""
                                                newPetSize = "Pequeno"
                                            }
                                            petsList.removeAt(index)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Excluir pet",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add new pet sub-form
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (editingPetIndex != null) "Editar Cachorro / Pet" else "Novo Cachorro / Pet",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newPetName,
                            onValueChange = { newPetName = it },
                            label = { Text("Nome do Cachorro") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newPetBreed,
                                onValueChange = { newPetBreed = it },
                                label = { Text("Raça") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newPetAge,
                                onValueChange = { newPetAge = it },
                                label = { Text("Idade (ex: 2 anos)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Porte do Cachorro (Preenche valores sugeridos):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Pequeno", "Médio", "Grande").forEach { sizeOpt ->
                                val isSelected = newPetSize == sizeOpt
                                Surface(
                                    selected = isSelected,
                                    onClick = {
                                        newPetSize = sizeOpt
                                        when (sizeOpt) {
                                            "Pequeno" -> {
                                                newPetBathPrice = "40.00"
                                                newPetGroomingPrice = "50.00"
                                            }
                                            "Médio" -> {
                                                newPetBathPrice = "55.00"
                                                newPetGroomingPrice = "70.00"
                                            }
                                            "Grande" -> {
                                                newPetBathPrice = "75.00"
                                                newPetGroomingPrice = "90.00"
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = sizeOpt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newPetBathPrice,
                                onValueChange = { newPetBathPrice = it },
                                label = { Text("Valor Banho (R$)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newPetGroomingPrice,
                                onValueChange = { newPetGroomingPrice = it },
                                label = { Text("Valor Tosa (R$)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = newPetTransportFee,
                            onValueChange = { newPetTransportFee = it },
                            label = { Text("Taxa para Buscar / Táxi Dog (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (editingPetIndex != null) {
                                TextButton(
                                    onClick = {
                                        editingPetIndex = null
                                        newPetName = ""
                                        newPetBreed = ""
                                        newPetAge = ""
                                        newPetBathPrice = ""
                                        newPetGroomingPrice = ""
                                        newPetTransportFee = ""
                                        newPetSize = "Pequeno"
                                    }
                                ) {
                                    Text("Cancelar Edição", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Button(
                                onClick = {
                                    if (newPetName.isNotBlank() && newPetBreed.isNotBlank()) {
                                        val bath = newPetBathPrice.toDoubleOrNull() ?: 0.0
                                        val groom = newPetGroomingPrice.toDoubleOrNull() ?: 0.0
                                        val transport = newPetTransportFee.toDoubleOrNull() ?: 0.0
                                        val petObj = AdditionalPet(
                                            name = newPetName.trim(),
                                            breed = newPetBreed.trim(),
                                            age = newPetAge.trim(),
                                            bathPrice = bath,
                                            groomingPrice = groom,
                                            transportFee = transport,
                                            size = newPetSize
                                        )
                                        if (editingPetIndex != null) {
                                            petsList[editingPetIndex!!] = petObj
                                            editingPetIndex = null
                                        } else {
                                            petsList.add(petObj)
                                        }
                                        newPetName = ""
                                        newPetBreed = ""
                                        newPetAge = ""
                                        newPetBathPrice = ""
                                        newPetGroomingPrice = ""
                                        newPetTransportFee = ""
                                        newPetSize = "Pequeno"
                                    }
                                },
                                enabled = newPetName.isNotBlank() && newPetBreed.isNotBlank(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (editingPetIndex != null) Icons.Default.Edit else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (editingPetIndex != null) "Atualizar Pet" else "Incluir Pet",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações Gerais") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage.ifEmpty { "Preencha o Nome do tutor, Telefone e pelo menos um Pet!" },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                // Se o usuário preencheu o formulário de pet mas não clicou em "Incluir"
                                if (newPetName.isNotBlank()) {
                                    val bath = newPetBathPrice.toDoubleOrNull() ?: 0.0
                                    val groom = newPetGroomingPrice.toDoubleOrNull() ?: 0.0
                                    val transport = newPetTransportFee.toDoubleOrNull() ?: 0.0
                                    val petObj = AdditionalPet(
                                        name = newPetName.trim(),
                                        breed = newPetBreed.trim(),
                                        age = newPetAge.trim(),
                                        bathPrice = bath,
                                        groomingPrice = groom,
                                        transportFee = transport,
                                        size = newPetSize
                                    )
                                    if (editingPetIndex != null) {
                                        petsList[editingPetIndex!!] = petObj
                                    } else {
                                        petsList.add(petObj)
                                    }
                                }

                                if (name.isBlank() || phone.isBlank()) {
                                    errorMessage = "Preencha o Nome do tutor e o Telefone!"
                                    isError = true
                                } else if (petsList.isEmpty()) {
                                    errorMessage = "Adicione pelo menos um Cachorro/Pet ao cadastro!"
                                    isError = true
                                } else {
                                    onSave(name.trim(), phone.trim(), phoneSecondary.trim(), address.trim(), notes.trim(), petsList.toList())
                                }
                            },
                            modifier = Modifier.testTag("btn_save_client")
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceFormDialog(
    title: String,
    initialService: PetService? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialService?.name ?: "") }
    var priceStr by remember { mutableStateOf(initialService?.price?.toString() ?: "") }
    var description by remember { mutableStateOf(initialService?.description ?: "") }

    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { /* Não fecha ao clicar fora ou voltar */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Serviço") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("service_name")
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Preço cobrado (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("service_price")
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (opcional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Preencha o Nome e um Preço válido!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val price = priceStr.toDoubleOrNull()
                                if (name.isBlank() || price == null) {
                                    isError = true
                                } else {
                                    onSave(name, price, description)
                                }
                            },
                            modifier = Modifier.testTag("btn_save_service")
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    title: String,
    initialProduct: Product? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var priceStr by remember { mutableStateOf(initialProduct?.price?.toString() ?: "") }
    var costPriceStr by remember { mutableStateOf(initialProduct?.costPrice?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "") }

    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { /* Não fecha ao clicar fora ou voltar */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Produto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name")
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Preço Venda (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_sale_price")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = { Text("Custo (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text("Quantidade em Estoque") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_stock")
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Preencha o Nome, Preço de Venda e Estoque válidos!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val price = priceStr.toDoubleOrNull()
                                val costPrice = costPriceStr.toDoubleOrNull() ?: 0.0
                                val stock = stockStr.toIntOrNull()
                                if (name.isBlank() || price == null || stock == null) {
                                    isError = true
                                } else {
                                    onSave(name, price, costPrice, stock)
                                }
                            },
                            modifier = Modifier.testTag("btn_save_product")
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentFormDialog(
    viewModel: PetViewModel,
    preselectedClient: Client? = null,
    initialAppointment: Appointment? = null,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, Long, String, Double, Long, String, Boolean, Boolean, Double, String, Double, Double) -> Unit
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val services by viewModel.services.collectAsStateWithLifecycle()

    var selectedClient by remember(preselectedClient, initialAppointment) {
        mutableStateOf(
            preselectedClient ?: clients.find { it.id == initialAppointment?.clientId }
        )
    }
    var selectedPetName by remember(selectedClient, initialAppointment) {
        mutableStateOf(initialAppointment?.petName ?: selectedClient?.getFullPetList()?.firstOrNull()?.name ?: "")
    }
    val selectedPet = remember(selectedClient, selectedPetName) {
        selectedClient?.getFullPetList()?.find { it.name == selectedPetName }
    }
    val selectedServiceIds = remember(initialAppointment, services) {
        val list = mutableStateListOf<Long>()
        if (initialAppointment != null) {
            val names = initialAppointment.serviceName.split(", ").map { it.trim().lowercase() }
            services.filter { it.name.lowercase() in names || it.id == initialAppointment.serviceId }.forEach { list.add(it.id) }
        }
        list
    }
    var clientSearchQuery by remember { mutableStateOf("") }

    var appointmentDateMillis by remember(initialAppointment) { mutableStateOf(initialAppointment?.dateMillis ?: viewModel.selectedDateMillis.value) }
    
    val initialTimeParts = initialAppointment?.timeString?.split(":") ?: listOf("09", "00")
    var hour by remember(initialAppointment) { mutableStateOf(initialTimeParts.getOrNull(0) ?: "09") }
    var minute by remember(initialAppointment) { mutableStateOf(initialTimeParts.getOrNull(1) ?: "00") }
    
    var paid by remember(initialAppointment) { mutableStateOf(initialAppointment?.paid ?: false) }
    var needsTransport by remember(initialAppointment) { mutableStateOf(initialAppointment?.needsTransport ?: false) }
    var transportationFee by remember(initialAppointment) { mutableStateOf(initialAppointment?.transportationFee?.toString() ?: "15.00") }
    var paymentMethod by remember(initialAppointment) { mutableStateOf(initialAppointment?.paymentMethod?.takeIf { it.isNotBlank() } ?: "Pix") }
    
    var discountPercent by remember(initialAppointment) { mutableStateOf(initialAppointment?.discountPercentage?.toString() ?: "0") }
    var discountAmount by remember(initialAppointment) { mutableStateOf(initialAppointment?.discount?.toString() ?: "0") }
    
    // Flag to know which field was last touched to keep them in sync correctly
    var lastEditedByPercent by remember { mutableStateOf(true) }

    var groomingOption by remember(initialAppointment) { 
        val opt = if (initialAppointment?.serviceName?.contains("Tesoura", ignoreCase = true) == true) "Tesoura" else "Máquina"
        mutableStateOf(opt) 
    }

    val combinedPrice by remember(services, selectedPet) {
        derivedStateOf {
            services.filter { selectedServiceIds.contains(it.id) }.sumOf { service ->
                val lowerName = service.name.lowercase()
                val customPrice = when {
                    lowerName.contains("banho") && (selectedPet?.bathPrice ?: 0.0) > 0.0 -> selectedPet!!.bathPrice
                    lowerName.contains("tosa") && (selectedPet?.groomingPrice ?: 0.0) > 0.0 -> selectedPet!!.groomingPrice
                    else -> service.price
                }
                customPrice
            }
        }
    }

    // Auto-update discount values when price changes
    LaunchedEffect(combinedPrice) {
        if (lastEditedByPercent) {
            val p = discountPercent.toDoubleOrNull() ?: 0.0
            val calculated = (combinedPrice * p) / 100.0
            discountAmount = String.format(Locale.US, "%.2f", calculated)
        } else {
            val a = discountAmount.toDoubleOrNull() ?: 0.0
            val calculated = if (combinedPrice > 0) (a / combinedPrice) * 100.0 else 0.0
            discountPercent = String.format(Locale.US, "%.1f", calculated)
        }
    }

    LaunchedEffect(selectedPet, services, initialAppointment) {
        if (selectedPet != null && initialAppointment == null) {
            selectedServiceIds.clear()
            services.forEach { service ->
                val lowerName = service.name.lowercase()
                if (lowerName.contains("banho") && selectedPet.bathPrice > 0.0) {
                    selectedServiceIds.add(service.id)
                }
                if (lowerName.contains("tosa") && selectedPet.groomingPrice > 0.0) {
                    selectedServiceIds.add(service.id)
                }
            }

            if (selectedPet.transportFee > 0.0) {
                transportationFee = selectedPet.transportFee.toString()
                needsTransport = true
            } else {
                needsTransport = false
                transportationFee = "15.00"
            }
        }
    }

    var clientExpanded by remember { mutableStateOf(false) }
    var serviceExpanded by remember { mutableStateOf(false) }

    var isError by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { /* Não fecha ao clicar fora ou voltar */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(
                    text = "Agendar Banho / Tosa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (clients.isEmpty()) {
                    Text(
                        text = "Atenção: Cadastre pelo menos um Cliente primeiro!",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Fechar")
                    }
                    return@Column
                }

                if (services.isEmpty()) {
                    Text(
                        text = "Atenção: Cadastre pelo menos um Serviço primeiro!",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Fechar")
                    }
                    return@Column
                }

                // Client selector with search
                Text("Encontrar Cliente / Pet:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = clientSearchQuery,
                    onValueChange = { clientSearchQuery = it },
                    label = { Text("Pesquisar por pet, tutor ou endereço...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                    trailingIcon = {
                        if (clientSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { clientSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("appointment_client_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display selected client if any
                if (selectedClient != null) {
                    val clientPets = remember(selectedClient) { selectedClient!!.getFullPetList() }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Pets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tutor: ${selectedClient?.name}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Contato: ${selectedClient?.phone}" + if (!selectedClient?.phoneSecondary.isNullOrBlank()) " / ${selectedClient?.phoneSecondary}" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                if (!selectedClient?.address.isNullOrEmpty()) {
                                    Text(
                                        text = "Endereço: ${selectedClient?.address}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            IconButton(onClick = { selectedClient = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "Remover seleção", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (clientPets.isNotEmpty()) {
                        Text(
                            text = "Selecione o Cachorro para o Atendimento:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                        ) {
                            clientPets.forEach { pet ->
                                val isSelected = selectedPetName == pet.name
                                Surface(
                                    onClick = { selectedPetName = pet.name },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Pets,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = pet.name,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = pet.breed,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Suggestions / Results
                if (selectedClient == null) {
                    val filteredClients = remember(clients, clientSearchQuery) {
                        if (clientSearchQuery.isBlank()) {
                            clients.take(5) // show up to 5 default suggestions
                        } else {
                            clients.filter { client ->
                                client.name.contains(clientSearchQuery, ignoreCase = true) ||
                                client.address.contains(clientSearchQuery, ignoreCase = true) ||
                                client.phone.contains(clientSearchQuery, ignoreCase = true) ||
                                client.phoneSecondary.contains(clientSearchQuery, ignoreCase = true) ||
                                client.getFullPetList().any { pet ->
                                    pet.name.contains(clientSearchQuery, ignoreCase = true) ||
                                    pet.breed.contains(clientSearchQuery, ignoreCase = true)
                                }
                            }
                        }
                    }

                    Text(
                        text = if (clientSearchQuery.isEmpty()) "Clientes recentes / Sugestões:" else "Resultados da Busca (${filteredClients.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (filteredClients.isEmpty()) {
                        Text(
                            text = "Nenhum cliente ou pet encontrado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredClients.forEach { client ->
                                Surface(
                                    onClick = {
                                        selectedClient = client
                                        clientSearchQuery = "" // Clear search after selection
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Tutor: ${client.name}",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            val petsStr = client.getFullPetList().joinToString(", ") { "${it.name} (${it.breed})" }
                                            Text(
                                                text = "Pets: $petsStr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (client.address.isNotEmpty()) {
                                                Text(
                                                    text = "Endereço: ${client.address}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Service multi-selection checkbox list
                Text("Selecione os Serviços (vários se necessário):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    services.forEach { service ->
                        val isSelected = selectedServiceIds.contains(service.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isSelected) {
                                        selectedServiceIds.remove(service.id)
                                    } else {
                                        selectedServiceIds.add(service.id)
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked == true) {
                                        selectedServiceIds.add(service.id)
                                    } else {
                                        selectedServiceIds.remove(service.id)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = service.name,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (service.description.isNotBlank()) {
                                    Text(
                                        text = service.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            val lowerName = service.name.lowercase()
                            val customPrice = when {
                                lowerName.contains("banho") && (selectedPet?.bathPrice ?: 0.0) > 0.0 -> selectedPet!!.bathPrice
                                lowerName.contains("tosa") && (selectedPet?.groomingPrice ?: 0.0) > 0.0 -> selectedPet!!.groomingPrice
                                else -> null
                            }

                            if (customPrice != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatCurrency(customPrice),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Preço do Pet",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text(
                                    text = formatCurrency(service.price),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                val hasGroomingSelected = remember(services) {
                    derivedStateOf {
                        services.filter { selectedServiceIds.contains(it.id) }.any { it.name.lowercase().contains("tosa") }
                    }
                }

                if (hasGroomingSelected.value) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tipo de Tosa:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf("Máquina", "Tesoura")
                        options.forEach { option ->
                            val isSelected = groomingOption == option
                            OutlinedButton(
                                onClick = { groomingOption = option },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(option, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (selectedServiceIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total dos Serviços:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = formatCurrency(combinedPrice),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = discountPercent,
                                    onValueChange = { 
                                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                            lastEditedByPercent = true
                                            discountPercent = it
                                            val p = it.toDoubleOrNull() ?: 0.0
                                            val calculated = (combinedPrice * p) / 100.0
                                            discountAmount = String.format(Locale.US, "%.2f", calculated)
                                        }
                                    },
                                    label = { Text("Desc. %", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedTextField(
                                    value = discountAmount,
                                    onValueChange = { 
                                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                            lastEditedByPercent = false
                                            discountAmount = it
                                            val a = it.toDoubleOrNull() ?: 0.0
                                            val calculated = if (combinedPrice > 0) (a / combinedPrice) * 100.0 else 0.0
                                            discountPercent = String.format(Locale.US, "%.1f", calculated)
                                        }
                                    },
                                    label = { Text("Desc. R$", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            val finalTotal = combinedPrice - (discountAmount.toDoubleOrNull() ?: 0.0)
                            if (finalTotal != combinedPrice) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Valor c/ Desconto:", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = formatCurrency(finalTotal),
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF2E7D32),
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker field
                Text("Data do Atendimento:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formatDate(appointmentDateMillis),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerDialog = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Abrir calendário")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Time selector row
                Text("Horário (Hora e Minutos):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var hourExpanded by remember { mutableStateOf(false) }
                    var minExpanded by remember { mutableStateOf(false) }

                    // Hour drop
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(expanded = hourExpanded, onExpandedChange = { hourExpanded = it }) {
                            OutlinedTextField(
                                value = hour,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Hora") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hourExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = hourExpanded, onDismissRequest = { hourExpanded = false }) {
                                (7..19).forEach { h ->
                                    val formattedH = String.format("%02d", h)
                                    DropdownMenuItem(
                                        text = { Text(formattedH) },
                                        onClick = {
                                            hour = formattedH
                                            hourExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Minute drop
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(expanded = minExpanded, onExpandedChange = { minExpanded = it }) {
                            OutlinedTextField(
                                value = minute,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Minutos") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = minExpanded, onDismissRequest = { minExpanded = false }) {
                                listOf("00", "15", "30", "45").forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            minute = m
                                            minExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Paid checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { paid = !paid }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = paid,
                        onCheckedChange = { paid = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Serviço já foi pago?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Registra receita automática no fluxo financeiro", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (paid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Forma de Pagamento:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Pix", "Crédito", "Débito", "Dinheiro").forEach { method ->
                            val isSelected = paymentMethod == method
                            if (isSelected) {
                                Button(
                                    onClick = { paymentMethod = method },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(method, fontSize = 12.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { paymentMethod = method },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(method, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Needs Transportation Checkbox Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { needsTransport = !needsTransport }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = needsTransport,
                        onCheckedChange = { needsTransport = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Serviço de busca/entrega (Táxi Dog)?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Selecione se for buscar o pet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (needsTransport) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = transportationFee,
                        onValueChange = { transportationFee = it },
                        label = { Text("Taxa de Busca / Entrega (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("appointment_transport_fee")
                    )
                }

                 if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selecione um Cliente e pelo menos um Serviço!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedClient == null || selectedServiceIds.isEmpty()) {
                                isError = true
                            } else {
                                val c = selectedClient!!
                                val selectedServices = services.filter { selectedServiceIds.contains(it.id) }
                                val s = selectedServices.first()
                                val combinedName = selectedServices.joinToString(", ") { service ->
                                     if (service.name.lowercase().contains("tosa")) {
                                         "${service.name} ($groomingOption)"
                                     } else {
                                         service.name
                                     }
                                 }
                                val combinedTotalPrice = selectedServices.sumOf { service ->
                                    val lowerName = service.name.lowercase()
                                    val customPrice = when {
                                        lowerName.contains("banho") && (selectedPet?.bathPrice ?: 0.0) > 0.0 -> selectedPet!!.bathPrice
                                        lowerName.contains("tosa") && (selectedPet?.groomingPrice ?: 0.0) > 0.0 -> selectedPet!!.groomingPrice
                                        else -> service.price
                                    }
                                    customPrice
                                }
                                val time = "$hour:$minute"
                                val fee = transportationFee.toDoubleOrNull() ?: 0.0
                                val payMethod = if (paid) paymentMethod else ""
                                val finalPetName = if (selectedPetName.isNotBlank()) selectedPetName else c.petName
                                val dPercent = discountPercent.toDoubleOrNull() ?: 0.0
                                val dAmount = discountAmount.toDoubleOrNull() ?: 0.0
                                // Mantém o status original se for novo agendamento, não força "Finalizado" só porque está pago
                                onSave(c.id, c.name, finalPetName, s.id, combinedName, combinedTotalPrice, appointmentDateMillis, time, paid, needsTransport, fee, payMethod, dPercent, dAmount)
                            }
                        },
                        modifier = Modifier.testTag("btn_confirm_appointment")
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        CustomCalendarPickerDialog(
            initialSelectedDate = appointmentDateMillis,
            onDismissRequest = { showDatePickerDialog = false },
            onDateSelected = {
                appointmentDateMillis = it
                showDatePickerDialog = false
            }
        )
    }
}

@Composable
fun TransactionFormDialog(
    title: String,
    initialTransaction: FinancialTransaction? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var type by remember { mutableStateOf(initialTransaction?.type ?: "RECEITA") }
    var description by remember { mutableStateOf(initialTransaction?.description ?: "") }
    var amountStr by remember { mutableStateOf(initialTransaction?.amount?.toString() ?: "") }
    var transactionDateMillis by remember { mutableStateOf(initialTransaction?.dateMillis ?: System.currentTimeMillis()) }

    var isError by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { /* Não fecha ao clicar fora ou voltar */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Segmented buttons for Receipt / Expense
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isRev = type == "RECEITA"
                    Button(
                        onClick = { type = "RECEITA" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRev) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isRev) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("transaction_type_revenue")
                    ) {
                        Text("Entrada (+)")
                    }

                    Button(
                        onClick = { type = "DESPESA" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isRev) Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isRev) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("transaction_type_expense")
                    ) {
                        Text("Saída (-)")
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (ex: Compra de ração, Luz, Água)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("transaction_desc")
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Valor (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount")
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker field
                Text("Data da Transação:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formatDate(transactionDateMillis),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerDialog = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Abrir calendário")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Preencha a Descrição e um Valor válido!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        // Check if it is an automatic transaction. Automatic transactions shouldn't be edited/deleted manually
                        if (initialTransaction?.appointmentId == null) {
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            // Helper message explaining it is automated
                            Text("Gerado automaticamente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Manual save enabled or blocked if automated
                        val isAutomated = initialTransaction?.appointmentId != null
                        Button(
                            enabled = !isAutomated,
                            onClick = {
                                val amount = amountStr.toDoubleOrNull()
                                if (description.isBlank() || amount == null) {
                                    isError = true
                                } else {
                                    onSave(type, description, amount, transactionDateMillis)
                                }
                            },
                            modifier = Modifier.testTag("btn_save_transaction")
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        CustomCalendarPickerDialog(
            initialSelectedDate = transactionDateMillis,
            onDismissRequest = { showDatePickerDialog = false },
            onDateSelected = {
                transactionDateMillis = it
                showDatePickerDialog = false
            }
        )
    }
}

// ==========================================
// CUSTOM RESPONSIVE CALENDAR PICKER COMPOSABLE
// ==========================================
@Composable
fun CustomCalendarPickerDialog(
    initialSelectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = initialSelectedDate })
    }

    var displayYear by remember { mutableStateOf(selectedCalendar.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableStateOf(selectedCalendar.get(Calendar.MONTH)) }

    val months = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    // Calendar logic
    val daysInMonth = remember(displayMonth, displayYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, displayYear)
        cal.set(Calendar.MONTH, displayMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDay = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sunday = 0, Monday = 1, etc.)
        Pair(maxDays, firstDay)
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Month and Year Selection)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (displayMonth == 0) {
                                displayMonth = 11
                                displayYear -= 1
                            } else {
                                displayMonth -= 1
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Mês Anterior")
                    }

                    Text(
                        text = "${months[displayMonth]} $displayYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = {
                            if (displayMonth == 11) {
                                displayMonth = 0
                                displayYear += 1
                            } else {
                                displayMonth += 1
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Próximo Mês")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Days of week header
                val daysOfWeek = listOf("D", "S", "T", "Q", "Q", "S", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days Grid
                val (maxDays, startPadding) = daysInMonth
                val totalSlots = maxDays + startPadding
                val rows = (totalSlots + 6) / 7

                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0..6) {
                            val slotIndex = r * 7 + c
                            val dayNumber = slotIndex - startPadding + 1

                            if (dayNumber in 1..maxDays) {
                                val isSelected = Calendar.getInstance().apply {
                                    timeInMillis = selectedCalendar.timeInMillis
                                }.run {
                                    get(Calendar.YEAR) == displayYear &&
                                    get(Calendar.MONTH) == displayMonth &&
                                    get(Calendar.DAY_OF_MONTH) == dayNumber
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            val newCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, displayYear)
                                                set(Calendar.MONTH, displayMonth)
                                                set(Calendar.DAY_OF_MONTH, dayNumber)
                                            }
                                            selectedCalendar = newCal
                                            onDateSelected(newCal.timeInMillis)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodDialog(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("Pix") }
    var paid by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = { /* Não fecha ao clicar fora ou voltar */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(
                    text = "Finalizar Atendimento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Pet: ${appointment.petName} (Dono: ${appointment.clientName})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Select Paid or Pending
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { paid = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = paid,
                        onClick = { paid = true }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Marcar como Pago agora", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { paid = false }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !paid,
                        onClick = { paid = false }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deixar como Pendente (Não Pago)", fontWeight = FontWeight.Bold)
                }

                if (paid) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Selecione a Forma de Pagamento:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val methods = listOf(
                        "Pix" to Icons.Default.Paid,
                        "Crédito" to Icons.Default.Paid,
                        "Débito" to Icons.Default.Paid,
                        "Dinheiro" to Icons.Default.Paid
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        methods.forEach { (method, icon) ->
                            val isSelected = selectedMethod == method
                            Surface(
                                onClick = { selectedMethod = method },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = method,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(if (paid) selectedMethod else "Pendente", paid)
                        }
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

// ==========================================
// STRING & CURRENCY FORMATTERS IN PORTUGUESE
// ==========================================
fun formatCurrency(value: Double): String {
    return String.format(Locale("pt", "BR"), "R$ %.2f", value)
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    return sdf.format(Date(millis))
}

fun formatDateLong(millis: Long): String {
    val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
    return sdf.format(Date(millis)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
}
