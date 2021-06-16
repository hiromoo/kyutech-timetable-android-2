package io.github.hiromoo.kyutechtimetable.ui.schedule.detail

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import io.github.hiromoo.kyutechtimetable.Utility.Companion.getTimeString
import io.github.hiromoo.kyutechtimetable.Utility.Companion.showDatePickerDialog
import io.github.hiromoo.kyutechtimetable.Utility.Companion.showTimeLengthPickerDialog
import io.github.hiromoo.kyutechtimetable.Utility.Companion.showTimePickerDialog
import io.github.hiromoo.kyutechtimetable.Utility.Companion.showTimeZonePickerDialog
import io.github.hiromoo.kyutechtimetable.Utility.TimeTranslator
import io.github.hiromoo.kyutechtimetable.Utility.TimeTranslator.TimeElementType
import io.github.hiromoo.kyutechtimetable.R
import io.github.hiromoo.kyutechtimetable.databinding.FragmentScheduleEditBinding
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.dateFormatter
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.save
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.timeFormatter
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.updateSchedule
import io.github.hiromoo.kyutechtimetable.model.Schedule
import io.github.hiromoo.kyutechtimetable.ui.schedule.ScheduleFragment
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_SCHEDULE = "schedule"
private const val ARG_DATE_TIME = "date_time"
private const val ARG_DATE = "date"

/**
 * Use the [ScheduleEditFragment.newInstance] factory methods to
 * create an instance of this fragment.
 */
class ScheduleEditFragment : Fragment() {

    companion object {
        /**
         * @return A new instance of fragment ScheduleDetailFragment.
         */
        @JvmStatic
        fun newInstance() = ScheduleEditFragment()

        /*
         * Use these factory methods to create a new instance of
         * this fragment using the provided parameters.
         */
        /**
         * @param schedule Schedule to set for this fragment.
         * @return A new instance of fragment ScheduleDetailFragment set by [schedule].
         */
        @JvmStatic
        fun newInstance(schedule: Schedule) = ScheduleEditFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_SCHEDULE, schedule)
            }
        }

        /**
         * @param dateTime Date and time to set for this fragment.
         * @return A new instance of fragment ScheduleDetailFragment set by [dateTime].
         */
        @JvmStatic
        fun newInstance(dateTime: LocalDateTime) = ScheduleEditFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_DATE_TIME, dateTime)
            }
        }

        /**
         * @param date Date to set for this fragment.
         * @return A new instance of fragment ScheduleDetailFragment set by [date].
         */
        @JvmStatic
        fun newInstance(date: LocalDate) = ScheduleEditFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }
        }
    }

    private lateinit var viewModel: ScheduleEditViewModel
    private var _binding: FragmentScheduleEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var schedule: Schedule? = null
    private var dateTime: LocalDateTime? = null
    private var date: LocalDate? = null

    private val isValidData: Boolean
        get() = with(viewModel) {
            title.value != "" && LocalDateTime.of(
                startDate.value,
                startTime.value
            ) <= LocalDateTime.of(
                finishDate.value,
                finishTime.value
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            schedule = getSerializable(ARG_SCHEDULE) as Schedule?
            dateTime = getSerializable(ARG_DATE_TIME) as LocalDateTime?
            date = getSerializable(ARG_DATE) as LocalDate?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ScheduleEditViewModel::class.java)
        _binding = FragmentScheduleEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            buttonClose.setOnClickListener {
                close()
            }
            buttonDone.setOnClickListener {
                val scheduleToUpdate = with(viewModel) {
                    updateSchedule(
                        schedule?.id ?: UUID.randomUUID().toString(),
                        title.value!!,
                        LocalDateTime.of(startDate.value, startTime.value),
                        LocalDateTime.of(finishDate.value, finishTime.value),
                        zoneId.value!!,
                        isNotificationEnabled.value!!,
                        notificationMinute.value!!,
                        location.value!!,
                        description.value!!,
                        true,
                        context
                    )!!
                }
                save(requireContext())
                updateParentFragments(scheduleToUpdate)
                close()
            }
            editTextTitle.addTextChangedListener {
                viewModel.setTitle(it.toString())
            }
            textDateStart.setOnClickListener {
                with(viewModel.startDate.value!!) {
                    showDatePickerDialog(
                        requireContext(), year, month.value, dayOfMonth
                    ) { _, year, month, dayOfMonth ->
                        viewModel.setStartDate(LocalDate.of(year, month + 1, dayOfMonth))
                    }
                }
            }
            textStartTime.setOnClickListener {
                with(viewModel.startTime.value!!) {
                    showTimePickerDialog(
                        requireContext(), hour, minute, true
                    ) { _, hourOfDay, minute ->
                        viewModel.setStartTime(LocalTime.of(hourOfDay, minute))
                    }
                }
            }
            textDateFinish.setOnClickListener {
                with(viewModel.finishDate.value!!) {
                    showDatePickerDialog(
                        requireContext(), year, month.value, dayOfMonth
                    ) { _, year, month, dayOfMonth ->
                        viewModel.setFinishDate(LocalDate.of(year, month + 1, dayOfMonth))
                    }
                }
            }
            textFinishTime.setOnClickListener {
                with(viewModel.finishTime.value!!) {
                    showTimePickerDialog(
                        requireContext(), hour, minute, true
                    ) { _, hourOfDay, minute ->
                        viewModel.setFinishTime(LocalTime.of(hourOfDay, minute))
                    }
                }
            }
            textTimeZone.setOnClickListener {
                val zoneIds = ZoneId.getAvailableZoneIds().toList()
                showTimeZonePickerDialog(
                    requireActivity(),
                    viewModel.zoneId.value!!,
                    zoneIds
                ) { _, _, values ->
                    viewModel.setZoneId(ZoneId.of(zoneIds[values[0]]))
                }
            }
            switchNotification.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setNotificationEnabled(isChecked)
            }
            textNotification.setOnClickListener {
                with(viewModel) {
                    showTimeLengthPickerDialog(
                        requireActivity(),
                        notificationMinute.value!!
                    ) { _, _, values ->
                        setNotificationMinute(
                            TimeTranslator(
                                values[0],
                                TimeElementType.values()[values[1]]
                            ).minutesAll
                        )
                    }
                }
            }
            editTextLocation.addTextChangedListener {
                viewModel.setLocation(it.toString())
            }
            editTextDescription.addTextChangedListener {
                viewModel.setDescription(it.toString())
            }
            with(viewModel) {
                schedule?.let {
                    setSchedule(it)
                }
                dateTime?.run {
                    setStartDate(toLocalDate())
                    setStartTime(toLocalTime())
                    setFinishDate(toLocalDate())
                    setFinishTime(toLocalTime())
                }
                date?.let {
                    setStartDate(it)
                    setFinishDate(it)
                }
                title.observe(viewLifecycleOwner) {
                    with(editTextTitle) {
                        if (it != text.toString()) {
                            setText(it)
                        }
                    }
                    checkValidData()
                }
                startDate.observe(viewLifecycleOwner) {
                    textDateStart.text = dateFormatter.format(it)
                    checkValidData()
                }
                startTime.observe(viewLifecycleOwner) {
                    textStartTime.text = timeFormatter.format(it)
                    checkValidData()
                }
                finishDate.observe(viewLifecycleOwner) {
                    textDateFinish.text = dateFormatter.format(it)
                    checkValidData()
                }
                finishTime.observe(viewLifecycleOwner) {
                    textFinishTime.text = timeFormatter.format(it)
                    checkValidData()
                }
                zoneId.observe(viewLifecycleOwner) {
                    textTimeZone.text = it.getDisplayName(TextStyle.FULL, Locale.getDefault())
                }
                isNotificationEnabled.observe(viewLifecycleOwner) {
                    with(scheduleNotificationContainer) {
                        removeView(scheduleNotification)
                        removeView(scheduleNotificationOff)
                        if (it) {
                            addView(scheduleNotification)
                        } else {
                            addView(scheduleNotificationOff)
                        }
                    }
                    with(switchNotification) {
                        if (it != isChecked) {
                            isChecked = it
                        }
                    }
                }
                notificationMinute.observe(viewLifecycleOwner) {
                    textNotification.text =
                        getTimeString(requireContext(), it, R.string.time_before)
                }
                location.observe(viewLifecycleOwner) {
                    with(editTextLocation) {
                        if (it != text.toString()) {
                            setText(it)
                        }
                    }
                }
                description.observe(viewLifecycleOwner) {
                    with(editTextDescription) {
                        if (it != text.toString()) {
                            setText(it)
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        val inputMethodManager =
            context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        inputMethodManager?.run {
            view?.run {
                hideSoftInputFromWindow(windowToken, HIDE_NOT_ALWAYS)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun close() {
        parentFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
            .remove(this@ScheduleEditFragment)
            .commit()
    }

    private fun checkValidData() {
        binding.buttonDone.isEnabled = isValidData
    }

    private fun updateParentFragments(schedule: Schedule) {
        parentFragment?.let { parentFragment1 ->
            when (parentFragment1) {
                is ScheduleDetailFragment -> {
                    val scheduleDetailFragment: ScheduleDetailFragment = parentFragment1
                    with(scheduleDetailFragment) {
                        setSchedule(schedule)
                        parentFragment?.let { parentFragment2 ->
                            if (parentFragment2 is ScheduleFragment) {
                                val scheduleFragment: ScheduleFragment = parentFragment2
                                scheduleFragment.updateScheduleList()
                            }
                        }
                    }
                }
                is ScheduleFragment -> {
                    val scheduleFragment: ScheduleFragment = parentFragment1
                    scheduleFragment.updateScheduleList()
                }
                else -> {
                }
            }
        }
    }
}