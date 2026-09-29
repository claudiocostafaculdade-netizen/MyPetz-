package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName
import java.util.UUID

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey var id: Long = 0L,
    var name: String = "",
    var phone: String = "",
    var petName: String = "",
    var petBreed: String = "",
    var petAge: String = "",
    var notes: String = "",
    var address: String = "",
    var phoneSecondary: String = "",
    var additionalPets: String = "",
    var petBathPrice: Double = 0.0,
    var petGroomingPrice: Double = 0.0,
    var petTransportFee: Double = 0.0,
    var petSize: String = "Pequeno",
    var lastUpdated: Long = 0L
)

data class AdditionalPet(
    var name: String = "",
    var breed: String = "",
    var age: String = "",
    var bathPrice: Double = 0.0,
    var groomingPrice: Double = 0.0,
    var transportFee: Double = 0.0,
    var size: String = "Pequeno"
) {
    fun toSerializedString(): String {
        val escName = name.replace("|", "").replace(";", "").trim()
        val escBreed = breed.replace("|", "").replace(";", "").trim()
        val escAge = age.replace("|", "").replace(";", "").trim()
        val escSize = size.replace("|", "").replace(";", "").trim()
        return "$escName|$escBreed|$escAge|$bathPrice|$groomingPrice|$transportFee|$escSize"
    }

    companion object {
        fun fromSerializedString(str: String): AdditionalPet? {
            val parts = str.split("|")
            if (parts.size >= 2) {
                return AdditionalPet(
                    name = parts[0],
                    breed = parts[1],
                    age = parts.getOrNull(2) ?: "",
                    bathPrice = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0,
                    groomingPrice = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0,
                    transportFee = parts.getOrNull(5)?.toDoubleOrNull() ?: 0.0,
                    size = parts.getOrNull(6) ?: "Pequeno"
                )
            }
            return null
        }
    }
}

fun Client.getFullPetList(): List<AdditionalPet> {
    val list = mutableListOf<AdditionalPet>()
    if (this.petName.isNotBlank()) {
        list.add(AdditionalPet(this.petName, this.petBreed, this.petAge, this.petBathPrice, this.petGroomingPrice, this.petTransportFee, this.petSize))
    }
    if (this.additionalPets.isNotBlank()) {
        this.additionalPets.split(";").forEach { petStr ->
            if (petStr.isNotBlank()) {
                AdditionalPet.fromSerializedString(petStr)?.let { list.add(it) }
            }
        }
    }
    return list
}

@Entity(tableName = "services")
data class PetService(
    @PrimaryKey var id: Long = 0L,
    var name: String = "",
    var price: Double = 0.0,
    var description: String = "",
    var lastUpdated: Long = 0L
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey var id: Long = 0L,
    var name: String = "",
    var price: Double = 0.0,
    var costPrice: Double = 0.0,
    var stock: Int = 0,
    var lastUpdated: Long = 0L
)

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey var id: Long = 0L,
    var clientId: Long = 0L,
    var clientName: String = "",
    var petName: String = "",
    var serviceId: Long = 0L,
    var serviceName: String = "",
    var servicePrice: Double = 0.0,
    var dateMillis: Long = 0L,
    var timeString: String = "",
    var status: String = "Agendado",
    var paid: Boolean = false,
    var needsTransport: Boolean = false,
    var transportationFee: Double = 0.0,
    var paymentMethod: String = "",
    var productsPrice: Double = 0.0,
    var productsList: String = "",
    var discount: Double = 0.0,
    var discountPercentage: Double = 0.0,
    var increase: Double = 0.0,
    var behaviorRating: Int = -1,
    var behaviorObservation: String = "",
    var professionalNotes: String = "",
    var lastUpdated: Long = 0L
)

@Entity(tableName = "transactions")
data class FinancialTransaction(
    @PrimaryKey var id: Long = 0L,
    var type: String = "",
    var description: String = "",
    var amount: Double = 0.0,
    var dateMillis: Long = 0L,
    var appointmentId: Long? = null,
    var paymentMethod: String = "",
    var discount: Double = 0.0,
    var increase: Double = 0.0,
    var lastUpdated: Long = 0L
)

object IdGenerator {
    fun generate(): Long {
        return System.currentTimeMillis() * 1000 + (0..999).random()
    }
}
