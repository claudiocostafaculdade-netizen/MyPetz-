package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Long): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)
}

@Dao
interface PetServiceDao {
    @Query("SELECT * FROM services ORDER BY name ASC")
    fun getAllServices(): Flow<List<PetService>>

    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun getServiceById(id: Long): PetService?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: PetService): Long

    @Update
    suspend fun updateService(service: PetService)

    @Delete
    suspend fun deleteService(service: PetService)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY dateMillis DESC, timeString ASC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: Long): Appointment?

    @Query("SELECT * FROM appointments WHERE dateMillis >= :startOfDay AND dateMillis <= :endOfDay ORDER BY timeString ASC")
    fun getAppointmentsForDay(startOfDay: Long, endOfDay: Long): Flow<List<Appointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)
}

@Dao
interface FinancialTransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<FinancialTransaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): FinancialTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinancialTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: FinancialTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: FinancialTransaction)

    @Query("DELETE FROM transactions WHERE appointmentId = :appointmentId")
    suspend fun deleteByAppointmentId(appointmentId: Long)

    @Query("SELECT * FROM transactions WHERE appointmentId = :appointmentId LIMIT 1")
    suspend fun getTransactionByAppointmentId(appointmentId: Long): FinancialTransaction?
}
