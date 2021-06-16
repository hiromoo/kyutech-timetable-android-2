package io.github.hiromoo.kyutechtimetable

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

class Utility {

    companion object {

        fun showDatePickerDialog(
            context: Context, year: Int, month: Int, dayOfMonth: Int,
            listener: DatePickerDialog.OnDateSetListener
        ) {
            with(DatePickerDialog(context)) {
                updateDate(year, month - 1, dayOfMonth)
                setOnDateSetListener(listener)
                show()
            }
        }

        fun showDatePickerDialog(
            context: Context, date: LocalDate,
            listener: DatePickerDialog.OnDateSetListener
        ) {
            with(date) {
                showDatePickerDialog(context, year, month.value, dayOfMonth, listener)
            }
        }

        fun showTimePickerDialog(
            context: Context, hourOfDay: Int, minute: Int, is24HourView: Boolean,
            listener: TimePickerDialog.OnTimeSetListener
        ) {
            TimePickerDialog(
                context,
                listener,
                hourOfDay, minute, is24HourView
            ).show()
        }

        fun showTimePickerDialog(
            context: Context, time: LocalTime, is24HourView: Boolean,
            listener: TimePickerDialog.OnTimeSetListener
        ) {
            with(time) {
                showTimePickerDialog(context, hour, minute, is24HourView, listener)
            }
        }

        fun showTimeLengthPickerDialog(
            activity: FragmentActivity,
            minute: Int,
            listener: PickersDialog.OnClickPositiveButtonListener
        ) {
            with(
                with(TimeTranslator(minute)) {
                    with(activity) {
                        PickersDialog(
                            this,
                            listOf(0, 0),
                            listOf(999, 3),
                            listOf(
                                optimizedElement,
                                optimizedElementType.ordinal
                            ),
                            listOf(
                                null,
                                listOf(
                                    getString(R.string.minutes),
                                    getString(R.string.hours),
                                    getString(R.string.days),
                                    getString(R.string.weeks)
                                )
                            ),
                            2
                        )
                    }
                }
            ) {
                setOnClickPositiveButtonListener(listener)
                show(activity.supportFragmentManager, null)
            }
        }

        fun showTimeZonePickerDialog(
            activity: FragmentActivity,
            zoneId: ZoneId,
            zoneIds: List<String>,
            listener: PickersDialog.OnClickPositiveButtonListener
        ) {
            PickersDialog(
                activity,
                0,
                zoneIds.size - 1,
                zoneIds.indexOf(zoneId.id),
                zoneIds.map {
                    ZoneId.of(it).getDisplayName(TextStyle.FULL, Locale.getDefault())
                }
            ).apply {
                setOnClickPositiveButtonListener(listener)
                show(activity.supportFragmentManager, null)
            }
        }

        fun getTimeString(context: Context, minute: Int, @StringRes resId: Int): String {
            with(context) {
                with(TimeTranslator(minute)) {
                    return when (optimizedElementType) {
                        TimeTranslator.TimeElementType.WEEKS ->
                            getString(resId, optimizedElement, getString(R.string.weeks))
                        TimeTranslator.TimeElementType.DAYS ->
                            getString(resId, optimizedElement, getString(R.string.days))
                        TimeTranslator.TimeElementType.HOURS ->
                            getString(resId, optimizedElement, getString(R.string.hours))
                        else ->
                            getString(resId, optimizedElement, getString(R.string.minutes))
                    }
                }
            }
        }

        fun alterDocument(context: Context, uri: Uri, data: String) {
            try {
                context.contentResolver.openFileDescriptor(uri, "w")?.use { descriptor ->
                    FileOutputStream(descriptor.fileDescriptor).use { fos ->
                        with(fos) {
                            channel.truncate(0)
                            write(data.toByteArray())
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        /*
        @Throws(IOException::class)
        private fun readTextFromUri(context: Context, uri: Uri): String {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
            return stringBuilder.toString()
        }
        */
    }

    class TimeTranslator(
        var minutesAll: Int = 0
    ) {

        enum class TimeElementType {
            MINUTES,
            HOURS,
            DAYS,
            WEEKS
        }

        constructor(value: Int, type: TimeElementType) : this(
            when (type) {
                TimeElementType.WEEKS -> value * 7 * 24 * 60
                TimeElementType.DAYS -> value * 24 * 60
                TimeElementType.HOURS -> value * 60
                TimeElementType.MINUTES -> value
            }
        )

        val minutes: Int get() = minutesAll % 60
        val hours: Int get() = (minutesAll - weeks * 7 * 24 * 60 - days * 24 * 60) / 60
        val days: Int get() = (minutesAll - weeks * 7 * 24 * 60) / (60 * 24)
        val weeks: Int get() = minutesAll / (60 * 24 * 7)

        val optimizedElementType: TimeElementType
            get() =
                if (weeks > 0 && days == 0 && hours == 0 && minutes == 0) TimeElementType.WEEKS
                else if (days > 0 && hours == 0 && minutes == 0) TimeElementType.DAYS
                else if (hours > 0 && minutes == 0) TimeElementType.HOURS
                else TimeElementType.MINUTES

        val optimizedElement: Int
            get() = when (optimizedElementType) {
                TimeElementType.WEEKS -> weeks
                TimeElementType.DAYS -> days + weeks * 7
                TimeElementType.HOURS -> hours + days * 24 + weeks * 7 * 24
                TimeElementType.MINUTES -> minutesAll
            }
    }
}