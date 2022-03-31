package io.github.hiromoo.kyutechtimetable.ui.home

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.github.hiromoo.kyutechtimetable.PickersDialog
import io.github.hiromoo.kyutechtimetable.R
import io.github.hiromoo.kyutechtimetable.StartActivityLifecycleObserver
import io.github.hiromoo.kyutechtimetable.activity.SettingsActivity
import io.github.hiromoo.kyutechtimetable.activity.WebActivity
import io.github.hiromoo.kyutechtimetable.databinding.FragmentHomeBinding
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.removeAllSubjects
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.save
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.subjects
import io.github.hiromoo.kyutechtimetable.model.Subject
import io.github.hiromoo.kyutechtimetable.ui.ad.BottomAdView
import io.github.hiromoo.kyutechtimetable.ui.home.subject.SubjectDetailFragment

class HomeFragment : Fragment() {

    companion object {
        private const val COLUMNS = 5
        private const val ROWS = 5
    }

    private lateinit var webActivityObserver: StartActivityLifecycleObserver

    private lateinit var viewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var bottomAdView: BottomAdView

    private val filteredSubjects: List<Subject>
        get() {
            with(viewModel) {
                return subjects.filter {
                    it.year == year.value && it.quarter == quarter.value
                }.sortedBy {
                    it.period * COLUMNS + it.dayOfWeek
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webActivityObserver =
            StartActivityLifecycleObserver(requireActivity().activityResultRegistry, "web") {
                if (it?.resultCode == RESULT_OK) {
                    updateTimetable()
                }
            }
        lifecycle.addObserver(webActivityObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            val navController = Navigation.findNavController(view)
            val appBarConfiguration = AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_schedule
            ).build()
            NavigationUI.setupWithNavController(toolbarHome, navController, appBarConfiguration)

            toolbarHome.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_update_timetable -> {
                        webActivityObserver.launch(
                            requireContext(),
                            WebActivity::class.java
                        )
                        return@setOnMenuItemClickListener true
                    }
                    R.id.item_settings -> {
                        startActivity(Intent(context, SettingsActivity::class.java))
                        return@setOnMenuItemClickListener true
                    }
                    R.id.item_licenses -> {
                        startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_delete_all_subjects -> {
                        AlertDialog.Builder(activity)
                            .setTitle(R.string.delete_all_subjects)
                            .setIcon(R.drawable.outline_warning_24)
                            .setMessage(R.string.delete_all_subjects_message)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                removeAllSubjects(requireContext()) { true }
                                updateTimetable()
                                save(requireContext())
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }
                return@setOnMenuItemClickListener false
            }
            buttonYear.setOnClickListener {
                val year = viewModel.year.value!!
                with(subjects) {
                    with(PickersDialog(
                        requireContext(),
                        minOfOrNull { it.year } ?: year,
                        maxOfOrNull { it.year } ?: year,
                        year
                    )) {
                        setOnClickPositiveButtonListener { _, _, values ->
                            viewModel.setYear(values[0])
                        }
                        show(this@HomeFragment.requireActivity().supportFragmentManager, null)
                    }
                }
            }
            buttonQuarter.setOnClickListener {
                with(PickersDialog(
                    requireContext(),
                    0,
                    3,
                    viewModel.quarter.value!! - 1,
                    List(4) { "${it + 1}Q" }
                )) {
                    setOnClickPositiveButtonListener { _, _, values ->
                        viewModel.setQuarter(values[0] + 1)
                    }
                    show(this@HomeFragment.requireActivity().supportFragmentManager, null)
                }
            }
            timetable.layoutManager = GridLayoutManager(context, COLUMNS)
            bottomAdView = BottomAdView(requireActivity(), bottomAdViewContainer).apply {
                loadBanner()
            }
            with(viewModel) {
                year.observe(viewLifecycleOwner) {
                    buttonYear.text = it.toString()
                    setTimetable(filteredSubjects)
                }
                quarter.observe(viewLifecycleOwner) {
                    buttonQuarter.text = resources.getString(R.string.quarter, it)
                    setTimetable(filteredSubjects)
                }
                timetable.observe(viewLifecycleOwner) { subjectList ->
                    binding.timetable.adapter = TimetableAdapter(subjectList).apply {
                        setOnItemClickListener { _, position ->
                            val row = position / COLUMNS
                            val column = position % COLUMNS
                            val subject = subjectList.firstOrNull {
                                it.dayOfWeek == column + 1
                                        && it.period == row + 1
                            }
                            val subjectDetailFragment: SubjectDetailFragment by lazy {
                                subject?.let {
                                    SubjectDetailFragment.newInstance(it)
                                } ?: run {
                                    SubjectDetailFragment.newInstance(
                                        year.value!!,
                                        quarter.value!!,
                                        column + 1,
                                        row + 1
                                    )
                                }
                            }
                            addFragment(subjectDetailFragment, "subject_edit")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun updateTimetable() {
        viewModel.setTimetable(filteredSubjects)
    }

    private fun addFragment(fragment: Fragment, tag: String? = null) {
        childFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.fragment_container, fragment, tag)
            .addToBackStack(null)
            .commit()
    }

    private class TimetableAdapter(private val subjects: List<Subject>) :
        RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {

        private var listener: OnItemClickListener? = null

        fun interface OnItemClickListener {
            fun onClick(v: View, position: Int)
        }

        fun setOnItemClickListener(listener: OnItemClickListener) {
            this.listener = listener
        }

        private class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.name_subject)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_subject, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val row = position / COLUMNS
            val column = position % COLUMNS
            with(holder) {
                name.text = subjects.firstOrNull {
                    it.period == row + 1 && it.dayOfWeek == column + 1
                }?.name ?: ""
                itemView.setOnClickListener {
                    listener?.onClick(it, position)
                }
            }
        }

        override fun getItemCount() = COLUMNS * ROWS
    }
}