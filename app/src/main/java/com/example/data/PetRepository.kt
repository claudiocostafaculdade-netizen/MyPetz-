package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.notifications.NotificationHelper
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PetRepository(val context: Context) {
    
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _databaseFlow = MutableStateFlow<AppDatabase?>(null)
    private val cloudListeners = mutableListOf<ListenerRegistration>()
    
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("PetRepository", "Firebase Firestore not initialized", e)
            null
        }
    }
    
    private var currentUid: String? = null

    init {
        try {
            // Initialize with default database
            _databaseFlow.value = AppDatabase.getDatabase(context)
        } catch (e: Exception) {
            Log.e("PetRepository", "Failed to init default DB", e)
        }
    }

    fun switchDatabase(uid: String) {
        if (uid.isBlank()) return
        if (currentUid == uid) return
        currentUid = uid
        
        cleanupListeners()
        
        val dbName = "pet_agenda_db_$uid"
        try {
            val newDb = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                dbName
            )
            .fallbackToDestructiveMigration()
            .build()
            
            // Validate the DB before setting it
            newDb.query("SELECT 1", null).close()
            
            _databaseFlow.value = newDb
            Log.d("PetRepository", "Switched to database: $dbName")
            
            listenToCloudUpdates(uid)
        } catch (e: Exception) {
            Log.e("PetRepository", "Error switching database for UID $uid", e)
            // Fallback to default DB to keep app alive
            _databaseFlow.value = AppDatabase.getDatabase(context)
        }
    }

    private fun cleanupListeners() {
        cloudListeners.forEach { it.remove() }
        cloudListeners.clear()
    }

    private fun listenToCloudUpdates(uid: String) {
        val db = firestore ?: return
        val userRef = db.collection("users").document(uid)

        val cols = listOf("clients", "services", "appointments", "transactions", "products")
        cols.forEach { col ->
            val registration = userRef.collection(col).addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                snapshots?.documentChanges?.forEach { change ->
                    val doc = change.document
                    repositoryScope.launch {
                        try {
                            val activeDb = _databaseFlow.value ?: return@launch
                            when (col) {
                                "clients" -> doc.toObject(Client::class.java)?.let { syncClient(activeDb, it, change.type) }
                                "services" -> doc.toObject(PetService::class.java)?.let { syncService(activeDb, it, change.type) }
                                "appointments" -> doc.toObject(Appointment::class.java)?.let { syncAppointment(activeDb, it, change.type) }
                                "transactions" -> doc.toObject(FinancialTransaction::class.java)?.let { syncTransaction(activeDb, it, change.type) }
                                "products" -> doc.toObject(Product::class.java)?.let { syncProduct(activeDb, it, change.type) }
                            }
                        } catch (ex: Exception) {
                            Log.e("PetRepository", "Sync error $col", ex)
                        }
                    }
                }
            }
            cloudListeners.add(registration)
        }
    }

    private suspend fun syncClient(db: AppDatabase, cloud: Client, type: DocumentChange.Type) {
        if (type == DocumentChange.Type.REMOVED) db.clientDao().deleteClient(cloud)
        else {
            val local = db.clientDao().getClientById(cloud.id)
            if (local == null || cloud.lastUpdated > local.lastUpdated) db.clientDao().insertClient(cloud)
        }
    }

    private suspend fun syncService(db: AppDatabase, cloud: PetService, type: DocumentChange.Type) {
        if (type == DocumentChange.Type.REMOVED) db.petServiceDao().deleteService(cloud)
        else {
            val local = db.petServiceDao().getServiceById(cloud.id)
            if (local == null || cloud.lastUpdated > local.lastUpdated) db.petServiceDao().insertService(cloud)
        }
    }

    private suspend fun syncAppointment(db: AppDatabase, cloud: Appointment, type: DocumentChange.Type) {
        if (type == DocumentChange.Type.REMOVED) {
            db.appointmentDao().deleteAppointment(cloud)
            NotificationHelper.cancelNotification(context, cloud.id)
        } else {
            val local = db.appointmentDao().getAppointmentById(cloud.id)
            if (local == null || cloud.lastUpdated > local.lastUpdated) {
                db.appointmentDao().insertAppointment(cloud)
                NotificationHelper.scheduleNotification(context, cloud)
            }
        }
    }

    private suspend fun syncTransaction(db: AppDatabase, cloud: FinancialTransaction, type: DocumentChange.Type) {
        if (type == DocumentChange.Type.REMOVED) db.financialTransactionDao().deleteTransaction(cloud)
        else {
            val local = db.financialTransactionDao().getTransactionById(cloud.id)
            if (local == null || cloud.lastUpdated > local.lastUpdated) db.financialTransactionDao().insertTransaction(cloud)
        }
    }

    private suspend fun syncProduct(db: AppDatabase, cloud: Product, type: DocumentChange.Type) {
        if (type == DocumentChange.Type.REMOVED) db.productDao().deleteProduct(cloud)
        else {
            val local = db.productDao().getProductById(cloud.id)
            if (local == null || cloud.lastUpdated > local.lastUpdated) db.productDao().insertProduct(cloud)
        }
    }

    suspend fun syncFromCloud() {
        val uid = currentUid ?: return
        val db = firestore ?: return
        val userRef = db.collection("users").document(uid)
        val activeDb = _databaseFlow.value ?: return

        listOf("clients", "services", "appointments", "transactions", "products").forEach { col ->
            try {
                val docs = userRef.collection(col).get().await()
                for (doc in docs) {
                    try {
                        when (col) {
                            "clients" -> doc.toObject(Client::class.java)?.let { syncClient(activeDb, it, DocumentChange.Type.ADDED) }
                            "services" -> doc.toObject(PetService::class.java)?.let { syncService(activeDb, it, DocumentChange.Type.ADDED) }
                            "appointments" -> doc.toObject(Appointment::class.java)?.let { syncAppointment(activeDb, it, DocumentChange.Type.ADDED) }
                            "transactions" -> doc.toObject(FinancialTransaction::class.java)?.let { syncTransaction(activeDb, it, DocumentChange.Type.ADDED) }
                            "products" -> doc.toObject(Product::class.java)?.let { syncProduct(activeDb, it, DocumentChange.Type.ADDED) }
                        }
                    } catch (e: Exception) { Log.e("PetRepository", "Sync $col item error", e) }
                }
            } catch (e: Exception) { Log.e("PetRepository", "Sync $col collection error", e) }
        }
    }

    fun useDefaultDatabase() {
        currentUid = null
        cleanupListeners()
        _databaseFlow.value = AppDatabase.getDatabase(context)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allClients: Flow<List<Client>> = _databaseFlow.flatMapLatest { it?.clientDao()?.getAllClients() ?: flowOf(emptyList()) }
    @OptIn(ExperimentalCoroutinesApi::class)
    val allServices: Flow<List<PetService>> = _databaseFlow.flatMapLatest { it?.petServiceDao()?.getAllServices() ?: flowOf(emptyList()) }
    @OptIn(ExperimentalCoroutinesApi::class)
    val allProducts: Flow<List<Product>> = _databaseFlow.flatMapLatest { it?.productDao()?.getAllProducts() ?: flowOf(emptyList()) }
    @OptIn(ExperimentalCoroutinesApi::class)
    val allAppointments: Flow<List<Appointment>> = _databaseFlow.flatMapLatest { it?.appointmentDao()?.getAllAppointments() ?: flowOf(emptyList()) }
    @OptIn(ExperimentalCoroutinesApi::class)
    val allTransactions: Flow<List<FinancialTransaction>> = _databaseFlow.flatMapLatest { it?.financialTransactionDao()?.getAllTransactions() ?: flowOf(emptyList()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAppointmentsForDay(start: Long, end: Long): Flow<List<Appointment>> = _databaseFlow.flatMapLatest { it?.appointmentDao()?.getAppointmentsForDay(start, end) ?: flowOf(emptyList()) }

    suspend fun insertClient(client: Client): Long {
        val db = _databaseFlow.value ?: return 0L
        if (client.id == 0L) client.id = IdGenerator.generate()
        client.lastUpdated = System.currentTimeMillis()
        db.clientDao().insertClient(client)
        saveToCloud("clients", client.id.toString(), client)
        return client.id
    }
    suspend fun updateClient(client: Client) {
        val db = _databaseFlow.value ?: return
        client.lastUpdated = System.currentTimeMillis()
        db.clientDao().updateClient(client)
        saveToCloud("clients", client.id.toString(), client)
    }
    suspend fun deleteClient(client: Client) {
        _databaseFlow.value?.clientDao()?.deleteClient(client)
        removeFromCloud("clients", client.id.toString())
    }

    suspend fun insertService(service: PetService): Long {
        val db = _databaseFlow.value ?: return 0L
        if (service.id == 0L) service.id = IdGenerator.generate()
        service.lastUpdated = System.currentTimeMillis()
        db.petServiceDao().insertService(service)
        saveToCloud("services", service.id.toString(), service)
        return service.id
    }
    suspend fun updateService(service: PetService) {
        val db = _databaseFlow.value ?: return
        service.lastUpdated = System.currentTimeMillis()
        db.petServiceDao().updateService(service)
        saveToCloud("services", service.id.toString(), service)
    }
    suspend fun deleteService(service: PetService) {
        _databaseFlow.value?.petServiceDao()?.deleteService(service)
        removeFromCloud("services", service.id.toString())
    }

    suspend fun insertProduct(product: Product): Long {
        val db = _databaseFlow.value ?: return 0L
        if (product.id == 0L) product.id = IdGenerator.generate()
        product.lastUpdated = System.currentTimeMillis()
        db.productDao().insertProduct(product)
        saveToCloud("products", product.id.toString(), product)
        return product.id
    }
    suspend fun updateProduct(product: Product) {
        val db = _databaseFlow.value ?: return
        product.lastUpdated = System.currentTimeMillis()
        db.productDao().updateProduct(product)
        saveToCloud("products", product.id.toString(), product)
    }
    suspend fun deleteProduct(product: Product) {
        _databaseFlow.value?.productDao()?.deleteProduct(product)
        removeFromCloud("products", product.id.toString())
    }

    suspend fun insertAppointment(appointment: Appointment): Long {
        val db = _databaseFlow.value ?: return 0L
        if (appointment.id == 0L) appointment.id = IdGenerator.generate()
        appointment.lastUpdated = System.currentTimeMillis()
        db.appointmentDao().insertAppointment(appointment)
        saveToCloud("appointments", appointment.id.toString(), appointment)
        handleAppointmentTransaction(appointment)
        NotificationHelper.scheduleNotification(context, appointment)
        return appointment.id
    }

    suspend fun updateAppointment(appointment: Appointment) {
        val db = _databaseFlow.value ?: return
        appointment.lastUpdated = System.currentTimeMillis()
        db.appointmentDao().updateAppointment(appointment)
        saveToCloud("appointments", appointment.id.toString(), appointment)
        handleAppointmentTransaction(appointment)
        NotificationHelper.scheduleNotification(context, appointment)
    }

    suspend fun deleteAppointment(appointment: Appointment) {
        val db = _databaseFlow.value ?: return
        val transaction = db.financialTransactionDao().getTransactionByAppointmentId(appointment.id)
        
        db.appointmentDao().deleteAppointment(appointment)
        db.financialTransactionDao().deleteByAppointmentId(appointment.id)
        
        removeFromCloud("appointments", appointment.id.toString())
        transaction?.let { removeFromCloud("transactions", it.id.toString()) }

        NotificationHelper.cancelNotification(context, appointment.id)
    }

    private suspend fun handleAppointmentTransaction(appointment: Appointment) {
        val db = _databaseFlow.value ?: return
        val existing = db.financialTransactionDao().getTransactionByAppointmentId(appointment.id)
        
        if (appointment.paid && appointment.status != "Cancelado") {
            val total = appointment.servicePrice + appointment.productsPrice + 
                        (if (appointment.needsTransport) appointment.transportationFee else 0.0) - 
                        appointment.discount
            
            val trans = FinancialTransaction(
                id = existing?.id ?: IdGenerator.generate(),
                type = "RECEITA",
                description = "Atendimento: ${appointment.petName} (${appointment.clientName})",
                amount = total,
                dateMillis = appointment.dateMillis,
                appointmentId = appointment.id,
                paymentMethod = appointment.paymentMethod,
                lastUpdated = System.currentTimeMillis()
            )
            db.financialTransactionDao().insertTransaction(trans)
            saveToCloud("transactions", trans.id.toString(), trans)
        } else {
            existing?.let { 
                db.financialTransactionDao().deleteTransaction(it)
                removeFromCloud("transactions", it.id.toString())
            }
        }
    }

    suspend fun payAppointment(appointment: Appointment, method: String) {
        appointment.paid = true
        appointment.paymentMethod = method
        appointment.status = "Finalizado"
        updateAppointment(appointment)
    }

    suspend fun cancelAppointment(appointment: Appointment) {
        appointment.status = "Cancelado"
        appointment.paid = false
        updateAppointment(appointment)
    }

    suspend fun insertTransaction(transaction: FinancialTransaction): Long {
        val db = _databaseFlow.value ?: return 0L
        if (transaction.id == 0L) transaction.id = IdGenerator.generate()
        transaction.lastUpdated = System.currentTimeMillis()
        db.financialTransactionDao().insertTransaction(transaction)
        saveToCloud("transactions", transaction.id.toString(), transaction)
        return transaction.id
    }
    suspend fun updateTransaction(transaction: FinancialTransaction) {
        val db = _databaseFlow.value ?: return
        transaction.lastUpdated = System.currentTimeMillis()
        db.financialTransactionDao().updateTransaction(transaction)
        saveToCloud("transactions", transaction.id.toString(), transaction)
    }
    suspend fun deleteTransaction(transaction: FinancialTransaction) {
        _databaseFlow.value?.financialTransactionDao()?.deleteTransaction(transaction)
        removeFromCloud("transactions", transaction.id.toString())
    }

    private suspend fun saveToCloud(collection: String, docId: String, data: Any) {
        val uid = currentUid ?: return
        try { firestore?.collection("users")?.document(uid)?.collection(collection)?.document(docId)?.set(data, SetOptions.merge())?.await() } catch (e: Exception) {}
    }

    private suspend fun removeFromCloud(collection: String, docId: String) {
        val uid = currentUid ?: return
        try { firestore?.collection("users")?.document(uid)?.collection(collection)?.document(docId)?.delete()?.await() } catch (e: Exception) {}
    }
}
