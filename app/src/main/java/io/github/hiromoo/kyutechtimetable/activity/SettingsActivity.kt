package io.github.hiromoo.kyutechtimetable.activity

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import io.github.hiromoo.kyutechtimetable.R
import io.github.hiromoo.kyutechtimetable.Utility.Companion.getTimeString
import io.github.hiromoo.kyutechtimetable.Utility.Companion.showDatePickerDialog
import io.github.hiromoo.kyutechtimetable.Utility.Companion.showTimeLengthPickerDialog
import io.github.hiromoo.kyutechtimetable.Utility.Companion.showTimePickerDialog
import io.github.hiromoo.kyutechtimetable.Utility.Companion.showTimeZonePickerDialog
import io.github.hiromoo.kyutechtimetable.Utility.TimeTranslator
import io.github.hiromoo.kyutechtimetable.Utility.TimeTranslator.TimeElementType
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.QuarterFlag
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.QuarterFlag.FINISH
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.QuarterFlag.START
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.classTimeMinute
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.dateFormatterWithoutYear
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getClassTimeString
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getFinishDate
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getStartDate
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.getStartTime
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.isNotificationEnabled
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.notificationTimeMinute
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.save
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.setClassTimeMinuteWithUpdate
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.setDateOfQuarterWithUpdate
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.setStartTimeOfClassWithUpdate
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.setSubjectTimeZoneWithUpdate
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.setUseCustomTermsWithUpdate
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.subjectZoneId
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.timeFormatter
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.useCustomTerms
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, HeaderFragment())
                .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            when (supportFragmentManager.fragments[0]) {
                is HeaderFragment -> setTitle(R.string.title_activity_settings)
                is TermsFragment -> setTitle(R.string.terms_header)
            }
            /*
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.title_activity_settings)
            }
            */
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (!supportFragmentManager.popBackStackImmediate()) {
            finish()
        }
        return true
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment
        ).apply {
            arguments = args
            //setTargetFragment(caller, 0)
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment)
            .addToBackStack(null)
            .commit()
        title = pref.title
        return true
    }

    class HeaderFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey)
        }
    }

    @Keep
    class TermsFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        private val customTermsPreference: SwitchPreferenceCompat by lazy {
            findPreference("custom_terms")!!
        }
        private val timeZonePreference: Preference by lazy {
            findPreference("subject_time_zone")!!
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.terms_preferences, rootKey)

            customTermsPreference.isChecked = useCustomTerms

            with(timeZonePreference) {
                updateAllSummaries()
                setOnPreferenceClickListener {
                    val zoneIds = ZoneId.getAvailableZoneIds().toList()
                    showTimeZonePickerDialog(
                        requireActivity(),
                        subjectZoneId,
                        zoneIds
                    ) { _, _, values ->
                        setSubjectTimeZoneWithUpdate(
                            context,
                            ZoneId.of(zoneIds[values[0]])
                        )
                        updateAllSummaries()
                    }
                    true
                }
            }
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                "custom_terms" -> {
                    setUseCustomTermsWithUpdate(
                        requireContext(),
                        sharedPreferences?.getBoolean(key, false)!!
                    )
                    save(requireContext())
                    updateAllSummaries()
                }
            }
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        private fun updateAllSummaries() {
            timeZonePreference.summary =
                subjectZoneId.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }
    }

    @Keep
    class NotificationsFragment : PreferenceFragmentCompat() {

        private val enabledPreference: SwitchPreferenceCompat by lazy {
            findPreference("default_notification_enabled")!!
        }

        private val timePreference: Preference by lazy {
            findPreference("default_notification_time")!!
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.notifications_preferences, rootKey)
            preferenceManager.preferenceDataStore = DataStore(requireContext())

            enabledPreference.isChecked = isNotificationEnabled

            with(timePreference) {
                updateAllSummaries()
                setOnPreferenceClickListener {
                    showTimeLengthPickerDialog(
                        requireActivity(),
                        notificationTimeMinute
                    ) { _, _, values ->
                        notificationTimeMinute = TimeTranslator(
                            values[0],
                            TimeElementType.values()[values[1]]
                        ).minutesAll
                        save(context)
                        updateAllSummaries()
                    }
                    true
                }
            }
        }

        private fun updateAllSummaries() {
            timePreference.summary =
                getTimeString(requireContext(), notificationTimeMinute, R.string.time_before)
        }
    }

    @Keep
    class QuarterFragment : PreferenceFragmentCompat() {

        private val quartersPreferences: QuartersPreferences by lazy {
            QuartersPreferences(this)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.quarter_preferences, rootKey)

            val year = LocalDate.now().year
            val getDate = { quarter: Int, quarterFlag: QuarterFlag ->
                when (quarterFlag) {
                    START -> getStartDate(year, quarter)
                    FINISH -> getFinishDate(year, quarter)
                }
            }
            val block: (Int, QuarterFlag) -> (Preference.() -> Unit) =
                { quarter, quarterFlag ->
                    {
                        setOnPreferenceClickListener {
                            showDatePickerDialog(
                                requireContext(),
                                getDate(quarter, quarterFlag)
                            ) { _, _, month, dayOfMonth ->
                                setDateOfQuarterWithUpdate(
                                    context,
                                    quarter,
                                    quarterFlag,
                                    month + 1,
                                    dayOfMonth
                                )
                                save(requireContext())
                                updateAllSummaries()
                            }
                            true
                        }
                    }
                }
            with(quartersPreferences) {
                for (i in 0 until size) {
                    val quarter = i + 1
                    with(getQuarter(quarter)) {
                        with(start, block(quarter, START))
                        with(finish, block(quarter, FINISH))
                    }
                }
            }
            updateAllSummaries()
        }

        private fun updateAllSummaries() {
            val year = LocalDate.now().year
            val block: (LocalDate) -> (Preference.() -> Unit) = {
                {
                    summary = dateFormatterWithoutYear.format(it)
                }
            }
            with(quartersPreferences) {
                for (i in 0 until size) {
                    val quarter = i + 1
                    with(getQuarter(quarter)) {
                        with(start, block(getStartDate(year, quarter)))
                        with(finish, block(getFinishDate(year, quarter)))
                    }
                }
            }
        }

        private class QuartersPreferences(quarterFragment: QuarterFragment) {

            val size: Int get() = quarters.size

            private val quarters: List<QuarterPreferences>

            init {
                var quarters: Array<QuarterPreferences> = arrayOf()
                for (quarter in 1..4) {
                    quarters += QuarterPreferences(quarterFragment, quarter)
                }
                this.quarters = quarters.toList()
            }

            fun getQuarter(quarter: Int): QuarterPreferences {
                return quarters[quarter - 1]
            }

            class QuarterPreferences(quarterFragment: QuarterFragment, quarter: Int) {

                val start: Preference
                val finish: Preference

                init {
                    with(quarterFragment) {
                        start = findPreference("start_quarter_$quarter")!!
                        finish = findPreference("finish_quarter_$quarter")!!
                    }
                }
            }
        }
    }

    @Keep
    class PeriodFragment : PreferenceFragmentCompat() {

        private val periodsPreference: PeriodsPreference by lazy {
            PeriodsPreference(this)
        }
        private val classTimePreference: Preference by lazy {
            findPreference("class_time")!!
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.period_preferences, rootKey)

            with(periodsPreference) {
                for (i in 0 until size) {
                    val period = i + 1
                    getPeriod(period).setOnPreferenceClickListener {
                        showTimePickerDialog(
                            requireContext(),
                            getStartTime(period),
                            true
                        ) { _, hourOfDay, minute ->
                            setStartTimeOfClassWithUpdate(
                                requireContext(),
                                period,
                                hourOfDay,
                                minute
                            )
                            save(requireContext())
                            updateAllSummaries()
                        }
                        true
                    }
                }
            }
            classTimePreference.setOnPreferenceClickListener {
                showTimeLengthPickerDialog(
                    requireActivity(),
                    classTimeMinute
                ) { _, _, values ->
                    setClassTimeMinuteWithUpdate(
                        requireContext(),
                        TimeTranslator(
                            values[0],
                            TimeElementType.values()[values[1]]
                        ).minutesAll
                    )
                    save(requireContext())
                    updateAllSummaries()
                }
                true
            }
            updateAllSummaries()
        }

        private fun updateAllSummaries() {
            with(periodsPreference) {
                for (i in 0 until size) {
                    val period = i + 1
                    getPeriod(period).summary = timeFormatter.format(getStartTime(period))
                }
            }
            classTimePreference.summary = getClassTimeString(requireContext())
        }

        private class PeriodsPreference(periodFragment: PeriodFragment) {

            val size: Int get() = periods.size

            private val periods: List<Preference>

            init {
                var periods: Array<Preference> = arrayOf()
                for (period in 1..5) {
                    periods += periodFragment.findPreference<Preference>("start_period_$period")!!
                }
                this.periods = periods.toList()
            }

            fun getPeriod(period: Int): Preference {
                return periods[period - 1]
            }
        }
    }

    private class DataStore(val context: Context) : PreferenceDataStore() {

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                "default_notification_enabled" -> {
                    isNotificationEnabled = value
                    save(context)
                }
            }
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            when (key) {
                "default_notification_enabled" -> {
                    return isNotificationEnabled
                }
            }
            return defValue
        }
    }
}