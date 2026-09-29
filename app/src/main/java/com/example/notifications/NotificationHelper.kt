package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.Appointment
import java.util.*

object NotificationHelper {

    fun scheduleNotification(context: Context, appointment: Appointment) {
        if (appointment.status == "Cancelado" || appointment.status == "Finalizado") {
            cancelNotification(context, appointment.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("appointmentId", appointment.id)
            putExtra("petName", appointment.petName)
            putExtra("clientName", appointment.clientName)
            putExtra("serviceName", appointment.serviceName)
            putExtra("timeString", appointment.timeString)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointment.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = calculateTriggerTime(appointment.dateMillis, appointment.timeString)
        
        // Only schedule if time is in the future
        if (triggerTime > System.currentTimeMillis()) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                Log.d("NotificationHelper", "Scheduled notification for appointment ${appointment.id} at $triggerTime")
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error scheduling notification", e)
            }
        } else {
            Log.d("NotificationHelper", "Not scheduling: Trigger time is in the past ($triggerTime)")
        }
    }

    fun cancelNotification(context: Context, appointmentId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d("NotificationHelper", "Cancelled notification for appointment $appointmentId")
        }
    }

    private fun calculateTriggerTime(dateMillis: Long, timeString: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateMillis
        
        val timeParts = timeString.split(":")
        if (timeParts.size == 2) {
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            calendar.set(Calendar.MINUTE, timeParts[1].toInt())
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        
        return calendar.timeInMillis
    }
}
