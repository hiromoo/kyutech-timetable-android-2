package io.github.hiromoo.kyutechtimetable

import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.content.ComponentName
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.media.RingtoneManager.TYPE_NOTIFICATION
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getNotificationId
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.japanZoneId
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.notificationIds
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.schedules
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class Notifier(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "schedule"
    }

    private val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager

    fun updateAll() {
        clear()
        ready()
        for (id in notificationIds.keys) {
            val schedule = schedules.firstOrNull { it.id == id }
            schedule?.run {
                set(
                    id,
                    R.drawable.outline_event_24,
                    name,
                    location,
                    startDateTime.atZone(zoneId),
                    startDateTime
                        .minusMinutes(notificationMinute.toLong())
                )
            }
        }
    }

    fun set(
        id: String,
        icon: Int,
        title: String,
        text: String,
        startTime: ZonedDateTime,
        notificationTime: LocalDateTime
    ) {
        if (startTime < ZonedDateTime.now()) return
        if (!notificationIds.containsKey(id)) {
            notificationIds[id] = getNotificationId()
        }
        val alarmIntent = getAlarmIntent(id, icon, title, text, startTime)!!
        alarmManager.setExactAndAllowWhileIdle(
            RTC_WAKEUP,
            notificationTime.toEpochSecond(
                japanZoneId.rules.getOffset(Instant.now())
            ) * 1000,
            alarmIntent
        )
    }

    fun notify(id: String, icon: Int, title: String, text: String, time: Long) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setWhen(time)
            .setShowWhen(true)
            .build()
        with(NotificationManagerCompat.from(context)) {
            notify(notificationIds[id]!!, notification)
        }
    }

    fun ready() {
        val receiver = ComponentName(context, AlarmReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        createNotificationChannel()
    }

    fun cancel(id: String) {
        getAlarmIntent(id)?.let {
            alarmManager.cancel(it)
        }
        notificationIds[id]?.let {
            with(NotificationManagerCompat.from(context)) {
                cancel(it)
            }
        }
        notificationIds.remove(id)
    }

    private fun clear() {
        val receiver = ComponentName(context, AlarmReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        with(NotificationManagerCompat.from(context)) {
            cancelAll()
        }
    }

    private fun getAlarmIntent(
        id: String,
        icon: Int,
        title: String,
        text: String,
        time: ZonedDateTime
    ): PendingIntent? {
        return notificationIds[id]?.let {
            PendingIntent.getBroadcast(
                context,
                it,
                Intent(context, AlarmReceiver::class.java)
                    .putExtra("id", id)
                    .putExtra("icon", icon)
                    .putExtra("title", title)
                    .putExtra("text", text)
                    .putExtra("time", time.toEpochSecond() * 1000),
                FLAG_CANCEL_CURRENT
            )
        }
    }

    private fun getAlarmIntent(id: String): PendingIntent? {
        return notificationIds[id]?.let {
            PendingIntent.getBroadcast(
                context,
                it,
                Intent(context, AlarmReceiver::class.java),
                FLAG_CANCEL_CURRENT
            )
        }
    }

    private fun createNotificationChannel() {
        val name = context.getString(R.string.channel_name_schedule)
        val descriptionText = context.getString(R.string.channel_description_schedule)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setSound(RingtoneManager.getDefaultUri(TYPE_NOTIFICATION), null)
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}