package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appointmentId = intent.getLongExtra("appointmentId", -1L)
        val petName = intent.getStringExtra("petName") ?: "Pet"
        val clientName = intent.getStringExtra("clientName") ?: "Cliente"
        val serviceName = intent.getStringExtra("serviceName") ?: "Serviço"
        val timeString = intent.getStringExtra("timeString") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "pet_appointments"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lembretes de Agendamentos",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("appointmentId", appointmentId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            appointmentId.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🐶 MyPetz - Hora do Atendimento")
            .setContentText("Atender $petName (Cliente: $clientName)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Está na hora de atender $petName.\n" +
                "Cliente: $clientName\n" +
                "Serviço: $serviceName\n" +
                "Horário: $timeString"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(appointmentId.toInt(), notification)
    }
}
