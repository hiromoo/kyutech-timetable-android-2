package io.github.hiromoo.kyutechtimetable.ui.home.subject

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
import androidx.lifecycle.ViewModelProvider
import io.github.hiromoo.kyutechtimetable.Utility.Companion.getTimeString
import io.github.hiromoo.kyutechtimetable.R
import io.github.hiromoo.kyutechtimetable.databinding.FragmentSubjectDetailBinding
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.dateFormatter
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getFinishDate
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getFinishTime
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getStartDate
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getStartTime
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.removeAllSubjects
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.save
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.subjectZoneId
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.timeFormatter
import io.github.hiromoo.kyutechtimetable.model.Subject
import io.github.hiromoo.kyutechtimetable.ui.home.HomeFragment
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_SUBJECT = "subject"
private const val ARG_YEAR = "year"
private const val ARG_QUARTER = "quarter"
private const val ARG_DAY_OF_WEEK = "day_of_week"
private const val ARG_PERIOD = "period"

/**
 * Use the [SubjectDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SubjectDetailFragment : Fragment() {

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param subject Subject to set for this fragment.
         * @return A new instance of fragment SubjectDetailFragment.
         */
        @JvmStatic
        fun newInstance(subject: Subject) = SubjectDetailFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_SUBJECT, subject)
            }
        }

        /**
         * @param year Year to set for this fragment.
         * @param quarter Quarter to set for this fragment.
         * @param dayOfWeek Day of week to set for this fragment.
         * @param period Period to set for this fragment.
         * @return A new instance of fragment SubjectEditFragment set by [dayOfWeek] and [period].
         */
        @JvmStatic
        fun newInstance(year: Int, quarter: Int, dayOfWeek: Int, period: Int) =
            SubjectDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_YEAR, year)
                    putInt(ARG_QUARTER, quarter)
                    putInt(ARG_DAY_OF_WEEK, dayOfWeek)
                    putInt(ARG_PERIOD, period)
                }
            }
    }

    private lateinit var viewModel: SubjectDetailViewModel
    private var _binding: FragmentSubjectDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var subject: Subject? = null
    private var year: Int? = null
    private var quarter: Int? = null
    private var dayOfWeek: Int? = null
    private var period: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            subject = getSerializable(ARG_SUBJECT) as Subject?
            year = getInt(ARG_YEAR)
            quarter = getInt(ARG_QUARTER)
            dayOfWeek = getInt(ARG_DAY_OF_WEEK)
            period = getInt(ARG_PERIOD)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(SubjectDetailViewModel::class.java)
        _binding = FragmentSubjectDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            buttonClose.setOnClickListener {
                close()
            }
            buttonEdit.setOnClickListener {
                val subjectEditFragment: SubjectEditFragment by lazy {
                    subject?.let {
                        SubjectEditFragment.newInstance(it)
                    } ?: run {
                        SubjectEditFragment.newInstance(year!!, quarter!!, dayOfWeek!!, period!!)
                    }
                }
                addFragment(subjectEditFragment, "subject_edit")
            }
            with(buttonDelete) {
                visibility = if (subject != null) VISIBLE else INVISIBLE
                subject?.run {
                    setOnClickListener {
                        AlertDialog.Builder(activity)
                            .setTitle(R.string.delete_subject)
                            .setIcon(R.drawable.outline_warning_24)
                            .setMessage(R.string.delete_subject_message)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                removeAllSubjects(requireContext()) {
                                    it.year == year
                                            && it.quarter == quarter
                                            && it.dayOfWeek == dayOfWeek
                                            && it.period == period
                                }
                                parentFragment?.run {
                                    if (this is HomeFragment) {
                                        updateTimetable()
                                    }
                                }
                                save(requireContext())
                                close()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }
            }
            with(viewModel) {
                subject?.let {
                    setSubject(it)
                } ?: run {
                    val dayOfWeekText = DayOfWeek.of(dayOfWeek!!)
                        .getDisplayName(TextStyle.FULL, Locale.getDefault())
                    setTitle(getString(R.string.day_of_week_and_period, dayOfWeekText, period!!))
                    setStartDate(getStartDate(year!!, quarter!!))
                    setFinishDate(getFinishDate(year!!, quarter!!))
                    setStartTime(getStartTime(period!!))
                    setFinishTime(getFinishTime(period!!))
                    setZoneId(subjectZoneId)
                    setNotificationEnabled(false)
                    setLocation(getString(R.string.location))
                    setDescription(getString(R.string.description))
                }
                title.observe(viewLifecycleOwner) {
                    textTitle.text = it
                }
                startDate.observe(viewLifecycleOwner) {
                    textDateStart.text = dateFormatter.format(it)
                }
                finishDate.observe(viewLifecycleOwner) {
                    textDateFinish.text = dateFormatter.format(it)
                }
                startTime.observe(viewLifecycleOwner) {
                    textTimeStart.text = timeFormatter.format(it)
                }
                finishTime.observe(viewLifecycleOwner) {
                    textTimeFinish.text = timeFormatter.format(it)
                }
                zoneId.observe(viewLifecycleOwner) {
                    textTimeZone.text = it.getDisplayName(TextStyle.FULL, Locale.getDefault())
                }
                isNotificationEnabled.observe(viewLifecycleOwner) {
                    with(subjectContainer) {
                        removeView(subjectNotification)
                        removeView(subjectNotificationOff)
                        if (it) {
                            addView(subjectNotification, 5)
                        } else {
                            addView(subjectNotificationOff, 5)
                        }
                    }
                }
                notificationMinute.observe(viewLifecycleOwner) {
                    textNotification.text =
                        getTimeString(requireContext(), it, R.string.time_before)
                }
                location.observe(viewLifecycleOwner) {
                    textLocation.text = it
                }
                description.observe(viewLifecycleOwner) {
                    textDescription.text = it
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setSubject(subject: Subject) {
        this.subject = subject
        viewModel.setSubject(subject)
    }

    private fun close() {
        parentFragmentManager.beginTransaction()
            .setTransition(TRANSIT_FRAGMENT_CLOSE)
            .remove(this)
            .commit()
    }

    private fun addFragment(fragment: Fragment, tag: String? = null) {
        childFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
    }
}