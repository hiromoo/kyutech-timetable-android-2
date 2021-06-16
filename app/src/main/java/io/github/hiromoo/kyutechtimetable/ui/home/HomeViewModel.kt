package io.github.hiromoo.kyutechtimetable.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getCurrentAcademicYear
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getCurrentQuarter
import io.github.hiromoo.kyutechtimetable.model.Subject

class HomeViewModel : ViewModel() {

    private val _year = MutableLiveData<Int>().apply {
        value = getCurrentAcademicYear()
    }
    var year: LiveData<Int> = _year
    fun setYear(year: Int) {
        _year.value = year
    }

    private val _quarter = MutableLiveData<Int>().apply {
        value = getCurrentQuarter()
    }
    val quarter: LiveData<Int> = _quarter
    fun setQuarter(quarter: Int) {
        _quarter.value = quarter
    }

    private val _timetable = MutableLiveData<List<Subject>>().apply {
        value = listOf()
    }
    val timetable: LiveData<List<Subject>> = _timetable
    fun setTimetable(timetable: List<Subject>) {
        _timetable.value = timetable
    }
}