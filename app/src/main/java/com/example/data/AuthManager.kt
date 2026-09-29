package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

data class UserAccount(
    val uid: String,
    val name: String,
    val email: String,
    val profilePicUrl: String = ""
)

class AuthManager(context: Context) {
    // Gracefully handle missing Firebase configuration
    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("AuthManager", "Firebase Auth not initialized. Check google-services.json", e)
            null
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun getCurrentUser(): UserAccount? {
        val firebaseUser = auth?.currentUser
        return firebaseUser?.let {
            UserAccount(
                uid = it.uid,
                name = it.displayName ?: "",
                email = it.email ?: "",
                profilePicUrl = it.photoUrl?.toString() ?: ""
            )
        }
    }

    fun signOut() {
        auth?.signOut()
    }

    fun shouldShowTour(uid: String): Boolean {
        return prefs.getBoolean("user_show_tour_$uid", true)
    }

    fun completeTour(uid: String) {
        prefs.edit().putBoolean("user_show_tour_$uid", false).apply()
    }

    fun isNewRegister(uid: String): Boolean {
        return prefs.getBoolean("user_is_new_register_$uid", false)
    }

    fun clearNewRegister(uid: String) {
        prefs.edit().putBoolean("user_is_new_register_$uid", false).apply()
    }
    
    fun markAsNewRegister(uid: String) {
        prefs.edit().putBoolean("user_is_new_register_$uid", true).apply()
    }
    
    fun isFirebaseAvailable(): Boolean = auth != null
}
