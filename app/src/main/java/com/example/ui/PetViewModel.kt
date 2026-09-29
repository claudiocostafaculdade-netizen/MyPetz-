package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class PetViewModel(private val repository: PetRepository) : ViewModel() {

    private val authManager = AuthManager(repository.context)
    private val firebaseAuth: FirebaseAuth? by lazy {
        try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    }
    private val firestore: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }

    private val _currentUser = MutableStateFlow<UserAccount?>(authManager.getCurrentUser())
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _showOnboardingTour = MutableStateFlow(false)
    val showOnboardingTour: StateFlow<Boolean> = _showOnboardingTour.asStateFlow()

    private val _selectedDateMillis = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    val clients: StateFlow<List<Client>> = repository.allClients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val services: StateFlow<List<PetService>> = repository.allServices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val products: StateFlow<List<Product>> = repository.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAppointments: StateFlow<List<Appointment>> = repository.allAppointments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val appointmentsForDay: StateFlow<List<Appointment>> = _selectedDateMillis
        .flatMapLatest { date -> repository.getAppointmentsForDay(getStartOfDay(date), getEndOfDay(date)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<FinancialTransaction>> = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val user = authManager.getCurrentUser()
        if (user != null) {
            setupUserEnv(user)
        }
    }

    private fun setupUserEnv(user: UserAccount) {
        repository.switchDatabase(user.uid)
        _currentUser.value = user
        _showOnboardingTour.value = authManager.shouldShowTour(user.uid)
        
        viewModelScope.launch {
            // First sync to get existing data
            try { repository.syncFromCloud() } catch (e: Exception) { Log.e("PetViewModel", "Sync failed", e) }

            // Small delay to allow listeners to potentially populate data if sync was fast
            kotlinx.coroutines.delay(1000)

            val currentServices = repository.allServices.first()
            if (currentServices.isEmpty()) {
                repository.insertService(PetService(name = "Banho Simples", price = 45.0, description = "Banho completo e perfume"))
                repository.insertService(PetService(name = "Tosa Higiênica", price = 70.0, description = "Banho e tosa de higiene"))
            }
            
            if (authManager.isNewRegister(user.uid)) {
                authManager.clearNewRegister(user.uid)
            }
        }
    }

    private suspend fun syncUserProfile(uid: String, name: String, email: String, photoUrl: String, provider: String) {
        try {
            val db = firestore ?: return
            val userRef = db.collection("users").document(uid)
            val doc = userRef.get().await()
            
            if (doc.exists()) {
                userRef.update("lastAccess", System.currentTimeMillis()).await()
            } else {
                val userMap = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "photoUrl" to photoUrl,
                    "provider" to provider,
                    "createdAt" to System.currentTimeMillis(),
                    "lastAccess" to System.currentTimeMillis()
                )
                userRef.set(userMap).await()
            }
        } catch (e: Exception) {
            Log.e("PetViewModel", "Failed to sync user profile", e)
        }
    }

    fun loginWithEmail(email: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val fAuth = firebaseAuth ?: throw Exception("Firebase não disponível")
                val result = fAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                
                if (firebaseUser != null) {
                    syncUserProfile(firebaseUser.uid, firebaseUser.displayName ?: "", firebaseUser.email ?: "", firebaseUser.photoUrl?.toString() ?: "", "Email")
                    val user = authManager.getCurrentUser()
                    if (user != null) setupUserEnv(user)
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(e.localizedMessage ?: "Erro ao entrar.")
            }
        }
    }

    fun registerWithEmail(name: String, email: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val fAuth = firebaseAuth ?: throw Exception("Firebase não disponível")
                val result = fAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val profileUpdates = com.google.firebase.auth.userProfileChangeRequest { displayName = name }
                    firebaseUser.updateProfile(profileUpdates).await()
                    
                    syncUserProfile(firebaseUser.uid, name, email, "", "Email")
                    authManager.markAsNewRegister(firebaseUser.uid)
                    
                    val user = authManager.getCurrentUser()
                    if (user != null) setupUserEnv(user)
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(e.localizedMessage ?: "Erro ao cadastrar.")
            }
        }
    }

    fun resetPassword(email: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                firebaseAuth?.sendPasswordResetEmail(email)?.await()
                onResult(null)
            } catch (e: Exception) {
                onResult(e.localizedMessage ?: "Erro ao enviar e-mail.")
            }
        }
    }

    fun onGoogleSignInSuccess(idToken: String) {
        if (idToken == "demo_token") {
            val demoUser = UserAccount(uid = "demo_user", name = "Usuário Teste", email = "teste@mypetz.com")
            setupUserEnv(demoUser)
            return
        }

        viewModelScope.launch {
            try {
                val fAuth = firebaseAuth ?: throw Exception("Firebase não disponível")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = fAuth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    syncUserProfile(
                        uid = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "",
                        email = firebaseUser.email ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                        provider = "Google"
                    )
                    
                    if (authResult.additionalUserInfo?.isNewUser == true) {
                        authManager.markAsNewRegister(firebaseUser.uid)
                    }
                    val user = authManager.getCurrentUser()
                    if (user != null) setupUserEnv(user)
                }
            } catch (e: Exception) {
                Log.e("PetViewModel", "Google Sign-in error", e)
            }
        }
    }

    fun logout() {
        authManager.signOut()
        _currentUser.value = null
        _showOnboardingTour.value = false
        repository.useDefaultDatabase()
    }

    fun completeOnboardingTour() {
        val user = _currentUser.value
        if (user != null) {
            authManager.completeTour(user.uid)
            _showOnboardingTour.value = false
        }
    }

    // CRUD e Helpers...
    fun setSelectedDate(millis: Long) { _selectedDateMillis.value = getStartOfDay(millis) }
    fun addClient(name: String, phone: String, petName: String, petBreed: String, petAge: String, notes: String, address: String, phoneSecondary: String = "", additionalPets: String = "", petBathPrice: Double = 0.0, petGroomingPrice: Double = 0.0, petTransportFee: Double = 0.0, petSize: String = "Pequeno") {
        viewModelScope.launch { repository.insertClient(Client(name = name, phone = phone, petName = petName, petBreed = petBreed, petAge = petAge, notes = notes, address = address, phoneSecondary = phoneSecondary, additionalPets = additionalPets, petBathPrice = petBathPrice, petGroomingPrice = petGroomingPrice, petTransportFee = petTransportFee, petSize = petSize)) }
    }
    fun updateClient(client: Client) { viewModelScope.launch { repository.updateClient(client) } }
    fun deleteClient(client: Client) { viewModelScope.launch { repository.deleteClient(client) } }
    fun addService(name: String, price: Double, description: String) { viewModelScope.launch { repository.insertService(PetService(name = name, price = price, description = description)) } }
    fun updateService(service: PetService) { viewModelScope.launch { repository.updateService(service) } }
    fun deleteService(service: PetService) { viewModelScope.launch { repository.deleteService(service) } }
    fun addProduct(name: String, price: Double, costPrice: Double, stock: Int) { viewModelScope.launch { repository.insertProduct(Product(name = name, price = price, costPrice = costPrice, stock = stock)) } }
    fun updateProduct(product: Product) { viewModelScope.launch { repository.updateProduct(product) } }
    fun deleteProduct(product: Product) { viewModelScope.launch { repository.deleteProduct(product) } }
    fun addAppointment(clientId: Long, clientName: String, petName: String, serviceId: Long, serviceName: String, servicePrice: Double, dateMillis: Long, timeString: String, paid: Boolean, needsTransport: Boolean = false, transportationFee: Double = 0.0, paymentMethod: String = "", discount: Double = 0.0, discountPercentage: Double = 0.0) {
        viewModelScope.launch { repository.insertAppointment(Appointment(clientId = clientId, clientName = clientName, petName = petName, serviceId = serviceId, serviceName = serviceName, servicePrice = servicePrice, dateMillis = getStartOfDay(dateMillis), timeString = timeString, status = "Agendado", paid = paid, needsTransport = needsTransport, transportationFee = transportationFee, paymentMethod = paymentMethod, discount = discount, discountPercentage = discountPercentage)) }
    }

    fun updateAppointment(appointment: Appointment) { viewModelScope.launch { repository.updateAppointment(appointment) } }
    fun deleteAppointment(appointment: Appointment) { viewModelScope.launch { repository.deleteAppointment(appointment) } }
    fun payAppointment(appointment: Appointment, paymentMethod: String) { viewModelScope.launch { repository.payAppointment(appointment, paymentMethod) } }
    fun cancelAppointment(appointment: Appointment) { viewModelScope.launch { repository.cancelAppointment(appointment) } }
    fun addTransaction(type: String, description: String, amount: Double, dateMillis: Long) { viewModelScope.launch { repository.insertTransaction(FinancialTransaction(type = type, description = description, amount = amount, dateMillis = dateMillis)) } }
    fun deleteTransaction(transaction: FinancialTransaction) { viewModelScope.launch { repository.deleteTransaction(transaction) } }
    fun updateTransaction(transaction: FinancialTransaction) { viewModelScope.launch { repository.updateTransaction(transaction) } }

    companion object {
        fun getStartOfDay(millis: Long): Long { val cal = Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }; return cal.timeInMillis }
        fun getEndOfDay(millis: Long): Long { val cal = Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }; return cal.timeInMillis }
    }
}

class PetViewModelFactory(private val repository: PetRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PetViewModel::class.java)) { @Suppress("UNCHECKED_CAST") return PetViewModel(repository) as T }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
