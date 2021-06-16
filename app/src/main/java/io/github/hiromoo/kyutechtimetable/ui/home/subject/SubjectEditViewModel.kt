package io.github.hiromoo.kyutechtimetable.ui.home.subject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.hiromoo.kyutechtimetable.model.Data
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.isNotificationEnabled
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.notificationTimeMinute
import io.github.hiromoo.kyutechtimetable.model.Subject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class SubjectEditViewModel : ViewModel() {

    private val _title = MutableLiveData<String>().apply {
        value = ""
    }
    val title: LiveData<String> = _title
    fun setTitle(title: String) {
        _title.value = title
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

    fun setSubject(subject: Subject) {
        with(subject) {
            setTitle(name)
            setNotificationEnabled(isNotificationEnabled)
            setNotificationMinute(notificationMinute)
            setLocation(location)
            setDescription(description)
        }
    }
}