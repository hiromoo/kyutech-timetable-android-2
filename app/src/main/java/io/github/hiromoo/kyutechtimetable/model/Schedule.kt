package io.github.hiromoo.kyutechtimetable.model

import android.content.Context
import io.github.hiromoo.kyutechtimetable.Notifier
import io.github.hiromoo.kyutechtimetable.R
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.ZoneId

@Serializable(ScheduleSerializer::class)
data class Schedule(
    var id: String,
    var name: String,
    var startDateTime: LocalDateTime,
    var finishDateTime: LocalDateTime,
    var zoneId: ZoneId,
    var isNotificationEnabled: Boolean,
    var notificationMinute: Int,
    var location: String,
    var description: String
) : java.io.Serializable {
    constructor(
        id: String,
        name: String,
        startDateTime: LocalDateTime,
        finishDateTime: LocalDateTime,
        zoneId: ZoneId,
        isNotificationEnabled: Boolean,
        notificationMinute: Int,
        location: String,
        description: String,
        context: Context?// If context exists, notification is set automatically.
    ) : this(
        id,
        name,
        startDateTime,
        finishDateTime,
        zoneId,
        isNotificationEnabled,
        notificationMinute,
        location,
        description
    ) {
        if (context != null) {
            setNotification(context)
        }
    }

    fun setNotification(context: Context) {
        if (isNotificationEnabled) {
            with(Notifier(context)) {
                ready()
                set(
                    id,
                    R.drawable.outline_event_24,
                    name,
                    location,
                    startDateTime.atZone(zoneId),
                    startDateTime.minusMinutes(notificationMinute.toLong())
                )
            }
        }
    }

    fun removeNotification(context: Context) {
        with(Notifier(context)) {
            cancel(id)
        }
    }
}

private object ScheduleSerializer : KSerializer<Schedule> {
    override val descriptor: SerialDescriptor = ScheduleSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Schedule) {
        val surrogate = ScheduleSurrogate(
            value.id,
            value.name,
            value.startDateTime.toString(),
            value.finishDateTime.toString(),
            value.zoneId.id,
            value.isNotificationEnabled,
            value.notificationMinute,
            value.location,
            value.description
        )
        encoder.encodeSerializableValue(ScheduleSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Schedule {
        val surrogate = decoder.decodeSerializableValue(ScheduleSurrogate.serializer())
        return Schedule(
            surrogate.id,
            surrogate.name,
            LocalDateTime.parse(surrogate.startDateTime),
            LocalDateTime.parse(surrogate.finishDateTime),
            ZoneId.of(surrogate.zoneId),
            surrogate.isNotificationEnabled,
            surrogate.notificationMinute,
            surrogate.location,
            surrogate.description
        )
    }

    @Serializable
    @SerialName("Schedule")
    private class ScheduleSurrogate(
        val id: String,
        val name: String,
        val startDateTime: String,
        val finishDateTime: String,
        val zoneId: String,
        val isNotificationEnabled: Boolean,
        val notificationMinute: Int,
        val location: String,
        val description: String
    )
}