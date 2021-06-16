package io.github.hiromoo.kyutechtimetable.model

import android.content.Context
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.classTimeMinute
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getFinishDate
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getFinishTime
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getStartDate
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getStartTime
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.schedules
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.subjectZoneId
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.*

@Serializable
data class Subject(
    var name: String,
    var code: String?,
    var `class`: String?,
    var year: Int,
    var quarter: Int,
    var dayOfWeek: Int,
    var period: Int,
    var isNotificationEnabled: Boolean,
    var notificationMinute: Int,
    var location: String,
    var description: String
) : java.io.Serializable {

    var scheduleIds = mutableListOf<String>()

    constructor(
        name: String,
        code: String?,
        `class`: String?,
        year: Int,
        quarter: Int,
        dayOfWeek: Int,
        period: Int,
        isNotificationEnabled: Boolean,
        notificationTimeMinute: Int,
        location: String,
        description: String,
        context: Context?// If context exists, schedules and its notifications are set automatically.
    ) : this(
        name,
        code,
        `class`,
        year,
        quarter,
        dayOfWeek,
        period,
        isNotificationEnabled,
        notificationTimeMinute,
        location,
        description
    ) {
        setSchedules(context)
    }

    fun setSchedules(context: Context? = null) {// If context exists, notification is set automatically.
        var current = getStartDate(year, quarter)
            .minusDays(1)
            .with(TemporalAdjusters.next(DayOfWeek.of(dayOfWeek)))
        val finishDate = getFinishDate(year, quarter)
        while (current <= finishDate) {
            val id = UUID.randomUUID().toString()
            schedules.add(
                Schedule(
                    id,
                    name,
                    LocalDateTime.of(current, getStartTime(period)),
                    LocalDateTime.of(
                        current.plusDays(classTimeMinute.toLong() / (60 * 24)),
                        getFinishTime(period)
                    ),
                    subjectZoneId,
                    isNotificationEnabled,
                    notificationMinute,
                    location,
                    description,
                    context
                )
            )
            scheduleIds.add(id)
            current = current.with(TemporalAdjusters.next(DayOfWeek.of(dayOfWeek)))
        }
    }

    fun removeAllSchedules(context: Context): Boolean {
        var result = false
        with(schedules.iterator()) {
            while (hasNext()) {
                val schedule = next()
                val id = schedule.id
                if (scheduleIds.contains(id)) {
                    schedule.removeNotification(context)
                    scheduleIds.remove(id)
                    remove()
                    result = true
                }
            }
        }
        return result
    }

    private fun setNotification(context: Context) {
        val schedules = schedules.filter { scheduleIds.contains(it.id) }
        for (schedule in schedules) {
            schedule.setNotification(context)
        }
    }
}
