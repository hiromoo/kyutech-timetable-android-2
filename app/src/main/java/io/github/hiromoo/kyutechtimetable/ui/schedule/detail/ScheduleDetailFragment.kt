package io.github.hiromoo.kyutechtimetable.ui.schedule.detail

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
import androidx.lifecycle.ViewModelProvider
import io.github.hiromoo.kyutechtimetable.Utility.Companion.getTimeString
import io.github.hiromoo.kyutechtimetable.R
import io.github.hiromoo.kyutechtimetable.databinding.FragmentScheduleDetailBinding
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.dateTimeFormatter
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.removeAllSchedules
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.removeSchedule
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.save
import io.github.hiromoo.kyutechtimetable.model.Schedule
import io.github.hiromoo.kyutechtimetable.ui.schedule.ScheduleFragment
import java.time.format.TextStyle
import java.util.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_SCHEDULE = "schedule"

/**
 * Use the [ScheduleDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ScheduleDetailFragment : Fragment() {

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param schedule Schedule to set for this fragment.
         * @return A new instance of fragment ScheduleDetailFragment.
         */
        @JvmStatic
        fun newInstance(schedule: Schedule) = ScheduleDetailFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_SCHEDULE, schedule)
            }
        }
    }

    private lateinit var viewModel: ScheduleDetailViewModel
    private var _binding: FragmentScheduleDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var schedule: Schedule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            schedule = it.getSerializable(ARG_SCHEDULE) as Schedule
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ScheduleDetailViewModel::class.java)
        _binding = FragmentScheduleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            buttonClose.setOnClickListener {
                close()
            }
            buttonEdit.setOnClickListener {
                addFragment(ScheduleEditFragment.newInstance(schedule), "schedule_edit")
            }
            with(buttonDelete) {
                setOnClickListener {
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.delete_schedule)
                        .setIcon(R.drawable.outline_warning_24)
                        .setMessage(R.string.delete_schedule_message)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            removeSchedule(schedule, true, requireContext())
                            parentFragment?.let {
                                if (it is ScheduleFragment) {
                                    it.updateScheduleList()
                                }
                            }
                            save(context)
                            close()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            }
            with(viewModel) {
                setSchedule(schedule)
                title.observe(viewLifecycleOwner) {
                    textTitle.text = it
                }
                startDateTime.observe(viewLifecycleOwner) {
                    textStartDateTime.text = dateTimeFormatter.format(it)
                }
                finishDateTime.observe(viewLifecycleOwner) {
                    textFinishDateTime.text = dateTimeFormatter.format(it)
                }
                zoneId.observe(viewLifecycleOwner) {
                    textTimeZone.text = it.getDisplayName(TextStyle.FULL, Locale.getDefault())
                }
                isNotificationEnabled.observe(viewLifecycleOwner) {
                    with(scheduleContainer) {
                        removeView(scheduleNotification)
                        removeView(scheduleNotificationOff)
                        if (it) {
                            addView(scheduleNotification, 4)
                        } else {
                            addView(scheduleNotificationOff, 4)
                        }
                    }
                }
                notificationMinute.observe(viewLifecycleOwner) {
                    textNotification.text = getTimeString(requireContext(), it, R.string.time_before)
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

    fun setSchedule(schedule: Schedule) {
        this.schedule = schedule
        viewModel.setSchedule(schedule)
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