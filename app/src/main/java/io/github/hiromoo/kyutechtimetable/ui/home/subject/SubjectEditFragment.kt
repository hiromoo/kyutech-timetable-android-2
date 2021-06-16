package io.github.hiromoo.kyutechtimetable.ui.home.subject

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import io.github.hiromoo.kyutechtimetable.Utility.Companion.getTimeString
import io.github.hiromoo.kyutechtimetable.Utility.Companion.showTimeLengthPickerDialog
import io.github.hiromoo.kyutechtimetable.R
import io.github.hiromoo.kyutechtimetable.Utility.TimeTranslator
import io.github.hiromoo.kyutechtimetable.databinding.FragmentSubjectEditBinding
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.save
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.updateSubject
import io.github.hiromoo.kyutechtimetable.model.Subject
import io.github.hiromoo.kyutechtimetable.ui.home.HomeFragment
import java.util.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_SUBJECT = "subject"
private const val ARG_YEAR = "year"
private const val ARG_QUARTER = "quarter"
private const val ARG_DAY_OF_WEEK = "day_of_week"
private const val ARG_PERIOD = "period"

/**
 * Use the [SubjectEditFragment.newInstance] factory methods to
 * create an instance of this fragment.
 */
class SubjectEditFragment : Fragment() {

    companion object {
        /*
         * Use these factory methods to create a new instance of
         * this fragment using the provided parameters.
         */
        /**
         * @param subject Subject to set for this fragment.
         * @return A new instance of fragment SubjectEditFragment set by [subject].
         */
        @JvmStatic
        fun newInstance(subject: Subject) = SubjectEditFragment().apply {
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
            SubjectEditFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_YEAR, year)
                    putInt(ARG_QUARTER, quarter)
                    putInt(ARG_DAY_OF_WEEK, dayOfWeek)
                    putInt(ARG_PERIOD, period)
                }
            }
    }

    private lateinit var viewModel: SubjectEditViewModel
    private var _binding: FragmentSubjectEditBinding? = null

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
        viewModel = ViewModelProvider(this).get(SubjectEditViewModel::class.java)
        _binding = FragmentSubjectEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            buttonClose.setOnClickListener {
                close()
            }
            buttonDone.setOnClickListener {
                val subjectToUpdate = with(viewModel) {
                    updateSubject(
                        title.value!!,
                        subject?.code,
                        subject?.`class`,
                        subject?.year ?: this@SubjectEditFragment.year!!,
                        subject?.quarter ?: this@SubjectEditFragment.quarter!!,
                        subject?.dayOfWeek ?: this@SubjectEditFragment.dayOfWeek!!,
                        subject?.period ?: this@SubjectEditFragment.period!!,
                        isNotificationEnabled.value!!,
                        notificationMinute.value!!,
                        location.value!!,
                        description.value!!,
                        true,
                        context
                    )!!
                }
                save(requireContext())
                updateParentFragments(subjectToUpdate)
                close()
            }
            editTextTitle.addTextChangedListener {
                viewModel.setTitle(it.toString())
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
                                TimeTranslator.TimeElementType.values()[values[1]]
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
                subject?.let {
                    setSubject(it)
                }
                title.observe(viewLifecycleOwner) {
                    with(editTextTitle) {
                        if (it != text.toString()) {
                            setText(it)
                        }
                    }
                }
                isNotificationEnabled.observe(viewLifecycleOwner) {
                    with(subjectNotificationContainer) {
                        removeView(subjectNotification)
                        removeView(subjectNotificationOff)
                        if (it) {
                            addView(subjectNotification)
                        } else {
                            addView(subjectNotificationOff)
                        }
                    }
                    with(switchNotification) {
                        if (it != isChecked) {
                            isChecked = it
                        }
                    }
                }
                notificationMinute.observe(viewLifecycleOwner) {
                    textNotification.text = getTimeString(requireContext(), it, R.string.time_before)
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
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        inputMethodManager?.run {
            view?.run {
                hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
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
            .remove(this@SubjectEditFragment)
            .commit()
    }

    private fun updateParentFragments(subject: Subject) {
        parentFragment?.let { parentFragment1 ->
            when (parentFragment1) {
                is SubjectDetailFragment -> {
                    with(parentFragment1) {
                        setSubject(subject)
                        parentFragment?.let { parentFragment2 ->
                            if (parentFragment2 is HomeFragment) {
                                parentFragment2.updateTimetable()
                            }
                        }
                    }
                }
                is HomeFragment -> {
                    parentFragment1.updateTimetable()
                }
                else -> {
                }
            }
        }
    }
}