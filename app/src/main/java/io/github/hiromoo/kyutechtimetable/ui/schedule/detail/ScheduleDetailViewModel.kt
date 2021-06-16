package io.github.hiromoo.kyutechtimetable.ui.schedule.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.hiromoo.kyutechtimetable.model.Schedule
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleDetailViewModel : ViewModel() {
    private val now = LocalDateTime.now()

    private val _title = MutableLiveData<String>().apply {
        value = ""
    }
    val title: LiveData<String> = _title
    fun setTitle(title: String) {
        _title.value = title
    }

    private val _startDateTime = MutableLiveData<LocalDateTime>().apply {
        value = now
    }
    val startDateTime: LiveData<LocalDateTime> = _startDateTime
    fun setStartDateTime(startDateTime: LocalDateTime) {
        _startDateTime.value = startDateTime
    }

    private val _finishDateTime = MutableLiveData<LocalDateTime>().apply {
        value = now.plusHours(1)
    }
    val finishDateTime: LiveData<LocalDateTime> = _finishDateTime
    fun setFinishDateTime(finishDateTime: LocalDateTime) {
        _finishDateTime.value = finishDateTime
    }

    private val _zoneId = MutableLiveData<ZoneId>().apply {
        value = ZoneId.systemDefault()
    }
    val zoneId: LiveData<ZoneId> = _zoneId
    fun setZoneId(zoneId: ZoneId) {
        _zoneId.value = zoneId
    }

    private val _isNotificationEnabled = MutableLiveData<Boolean>().apply {
        value = true
    }
    val isNotificationEnabled: LiveData<Boolean> = _isNotificationEnabled
    fun setNotificationEnabled(isNotificationEnabled: Boolean) {
        _isNotificationEnabled.value = isNotificationEnabled
    }

    private val _notificationMinute = MutableLiveData<Int>().apply {
        value = 30
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
            setStartDateTime(startDateTime)
            setFinishDateTime(finishDateTime)
            setZoneId(zoneId)
            setNotificationEnabled(isNotificationEnabled)
            setNotificationMinute(notificationMinute)
            setLocation(location)
            setDescription(description)
        }
    }
}