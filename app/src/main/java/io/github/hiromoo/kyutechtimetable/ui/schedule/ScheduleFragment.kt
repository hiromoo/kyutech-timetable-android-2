package io.github.hiromoo.kyutechtimetable.ui.schedule

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.hiromoo.kyutechtimetable.R
import io.github.hiromoo.kyutechtimetable.StartActivityLifecycleObserver
import io.github.hiromoo.kyutechtimetable.databinding.FragmentScheduleBinding
import io.github.hiromoo.kyutechtimetable.model.Data
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.exportSchedules
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.importSchedules
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.removeAllSchedules
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.schedules
import io.github.hiromoo.kyutechtimetable.model.Schedule
import io.github.hiromoo.kyutechtimetable.ui.ad.BottomAdView
import io.github.hiromoo.kyutechtimetable.ui.schedule.detail.ScheduleDetailFragment
import io.github.hiromoo.kyutechtimetable.ui.schedule.detail.ScheduleEditFragment
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScheduleFragment : Fragment() {

    private lateinit var createFileActivityObserver: StartActivityLifecycleObserver
    private lateinit var openFileActivityObserver: StartActivityLifecycleObserver

    private lateinit var viewModel: ScheduleViewModel
    private var _binding: FragmentScheduleBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var bottomAdView: BottomAdView

    private val filteredSchedules: List<Schedule>
        get() {
            return schedules.filter {
                it.startDateTime.toLocalDate() == viewModel.date.value
            }.sortedBy {
                it.startDateTime
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createFileActivityObserver =
            StartActivityLifecycleObserver(requireActivity().activityResultRegistry, "create_file") {
                if (it?.resultCode == RESULT_OK) {
                    // The result data contains a URI for the document or directory that
                    // the user selected.
                    it.data?.data?.also { uri ->
                        // Perform operations on the document using its URI.
                        exportSchedules(requireContext(), uri)
                    }
                }
            }
        openFileActivityObserver =
            StartActivityLifecycleObserver(requireActivity().activityResultRegistry, "open_file") {
                if (it?.resultCode == RESULT_OK) {
                    it.data?.data?.also { uri ->
                        importSchedules(requireContext(), uri)
                    }
                }
            }
        with(lifecycle) {
            addObserver(createFileActivityObserver)
            addObserver(openFileActivityObserver)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[ScheduleViewModel::class.java]
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            val navController = Navigation.findNavController(view)
            val appBarConfiguration = AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_schedule
            ).build()
            NavigationUI.setupWithNavController(toolbarSchedule, navController, appBarConfiguration)

            toolbarSchedule.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_add -> {
                        val scheduleEditFragment =
                            ScheduleEditFragment.newInstance(viewModel.date.value!!)
                        addFragment(scheduleEditFragment, "schedule_edit")
                    }
                    R.id.action_export -> {
                        createFile(null)
                    }
                    R.id.action_import -> {
                        openFile(null)
                    }
                    R.id.action_delete_all_schedules -> {
                        AlertDialog.Builder(activity)
                            .setTitle(R.string.delete_all_schedules)
                            .setIcon(R.drawable.outline_warning_24)
                            .setMessage(R.string.delete_all_schedules_message)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                removeAllSchedules(true, requireContext()) { true }
                                updateScheduleList()
                                Data.save(requireContext())
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }
                return@setOnMenuItemClickListener false
            }
            calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                viewModel.setDate(LocalDate.of(year, month + 1, dayOfMonth))
            }
            scheduleListView.layoutManager = LinearLayoutManager(context)
            bottomAdView = BottomAdView(requireActivity(), bottomAdViewContainer!!).apply {
                loadBanner()
            }

            viewModel.date.observe(viewLifecycleOwner) {
                calendarView.date = it.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
                updateScheduleList()
            }
        }
    }

    fun updateScheduleList() {
        filteredSchedules.let { listSchedules ->
            binding.scheduleListView.adapter =
                ScheduleListAdapter(listSchedules, requireContext()).apply {
                    setOnItemClickListener { _, position ->
                        val scheduleDetailFragment =
                            ScheduleDetailFragment.newInstance(listSchedules[position])
                        addFragment(scheduleDetailFragment, "schedule_detail")
                    }
                }
        }
    }

    private fun addFragment(fragment: Fragment, tag: String? = null) {
        childFragmentManager.beginTransaction()
            .setTransition(TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.fragment_container, fragment, tag)
            .addToBackStack(null)
            .commit()
    }

    private fun createFile(pickerInitialUri: Uri?) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/calendar"
            putExtra(Intent.EXTRA_TITLE, "schedules.ics")

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        createFileActivityObserver.launch(intent)
    }

    private fun openFile(pickerInitialUri: Uri?) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/calendar"

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }

        openFileActivityObserver.launch(intent)
    }

    private class ScheduleListAdapter(
        private val schedules: List<Schedule>,
        private val context: Context
    ) : RecyclerView.Adapter<ScheduleListAdapter.ViewHolder>() {

        private var listener: OnItemClickListener? = null

        fun interface OnItemClickListener {
            fun onClick(v: View, position: Int)
        }

        fun setOnItemClickListener(listener: OnItemClickListener) {
            this.listener = listener
        }

        private class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val time: TextView = view.findViewById(R.id.schedule_time)
            val name: TextView = view.findViewById(R.id.schedule_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_schedule, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val timeFormatter = DateTimeFormatter.ofPattern("H:mm")
            val schedule = schedules[position]
            with(holder) {
                time.text = context.resources.getString(
                    R.string.term_with_string,
                    timeFormatter.format(schedule.startDateTime),
                    timeFormatter.format(schedule.finishDateTime)
                )
                name.text = schedule.name
                itemView.setOnClickListener {
                    listener?.onClick(it, position)
                }
            }
        }

        override fun getItemCount() = schedules.size
    }
}