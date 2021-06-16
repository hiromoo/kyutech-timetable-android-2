package io.github.hiromoo.kyutechtimetable.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

class ScheduleViewModel : ViewModel() {

    private val _date = MutableLiveData<LocalDate>().apply {
        value = LocalDate.now()
    }
    val date: LiveData<LocalDate> = _date
    fun setDate(date: LocalDate) {
        _date.value = date
    }
}