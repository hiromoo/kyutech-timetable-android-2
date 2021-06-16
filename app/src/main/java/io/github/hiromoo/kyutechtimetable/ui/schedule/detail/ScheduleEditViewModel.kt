package io.github.hiromoo.kyutechtimetable.ui.schedule.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.hiromoo.kyutechtimetable.model.Data
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.notificationTimeMinute
import io.github.hiromoo.kyutechtimetable.model.Schedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ScheduleEditViewModel : ViewModel() {
    private val now = LocalDateTime.now()

    private val _title = MutableLiveData<String>().apply {
        value = ""
    }
    val title: LiveData<String> = _title
    fun setTitle(title: String) {
        _title.value = title
    }

    private val _startDate = MutableLiveData<LocalDate>().apply {
        value = now.toLocalDate()
    }
    val startDate: LiveData<LocalDate> = _startDate
    fun setStartDate(startDate: LocalDate) {
        _startDate.value = startDate
    }

    private val _startTime = MutableLiveData<LocalTime>().apply {
        value = LocalTime.of(12, 0)
    }
    val startTime: LiveData<LocalTime> = _startTime
    fun setStartTime(startTime: LocalTime) {
        _startTime.value = startTime
    }

    private val _finishDate = MutableLiveData<LocalDate>().apply {
        value = now.toLocalDate()
    }
    val finishDate: LiveData<LocalDate> = _finishDate
    fun setFinishDate(finishDate: LocalDate) {
        _finishDate.value = finishDate
    }

    private val _finishTime = MutableLiveData<LocalTime>().apply {
        value = LocalTime.of(13, 0)
    }
    val finishTime: LiveData<LocalTime> = _finishTime
    fun setFinishTime(finishTime: LocalTime) {
        _finishTime.value = finishTime
    }

    private val _zoneId = MutableLiveData<ZoneId>().apply {
        value = ZoneId.systemDefault()
    }
    val zoneId: LiveData<ZoneId> = _zoneId
    fun setZoneId(zoneId: ZoneId) {
        _zoneId.value = zoneId
    }

    private val _isNotificationEnabled = MutableLiveData<Boolean>().apply {
        value = Data.isNotificationEnabled
    }
    val isNotificationEnabled: LiveData<Boolean> = _isNotificationEnabled
    fun setNotificationEnabled(isNotificationEnabled: Boolean) {
        _isNotificationEnabled.value = isNotificationEnabled
    }

    private val _notificationMinute = MutableLiveData<Int>().apply {
        value = notificationTimeMinute
    }
    val notificationMinute: LiveData<Int> = _notificationMinute
    fun setNotificationMinute(notificationMinute: Int) {
        _notificationMinute.value = notificationMinute
    }

    private val _location = MutableLiveData<String>().apply {
        value = ""
    }
    val location: LiveData<String> = _location
    fun setLocation(location: String) {
        _location.value = location
    }

    private val _description = MutableLiveData<String>().apply {
        value = ""
    }
    val description: LiveData<String> = _description
    fun setDescription(description: String) {
        _description.value = description
    }

    fun setSchedule(schedule: Schedule) {
        with(schedule) {
            setTitle(name)
            setStartDate(startDateTime.toLocalDate())
            setStartTime(startDateTime.toLocalTime())
            setFinishDate(finishDateTime.toLocalDate())
            setFinishTime(finishDateTime.toLocalTime())
            setNotificationEnabled(isNotificationEnabled)
            setNotificationMinute(notificationMinute)
            setLocation(location)
            setDescription(description)
        }
    }
}