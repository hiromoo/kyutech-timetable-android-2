package io.github.hiromoo.kyutechtimetable.model

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager.GET_META_DATA
import android.net.Uri
import android.text.format.DateFormat
import io.github.hiromoo.kyutechtimetable.R
import io.github.hiromoo.kyutechtimetable.Utility.TimeTranslator
import io.github.hiromoo.kyutechtimetable.Utility.TimeTranslator.TimeElementType
import io.github.hiromoo.kyutechtimetable.Utility.Companion.alterDocument
import io.github.hiromoo.kyutechtimetable.Utility.Companion.getTimeString
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.QuarterFlag.FINISH
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.QuarterFlag.START
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.classTimeMinute
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.finishDateOfQuarter
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.isNotificationEnabled
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.nextNotificationId
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.notificationIds
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.notificationTimeMinute
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.schedules
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.startDateOfQuarter
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.startTimeOfClass
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.subjectZoneId
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.subjects
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.useCustomTerms
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Property.*
import net.fortuna.ical4j.model.TimeZone
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.component.VTimeZone
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.model.property.Action.AUDIO
import net.fortuna.ical4j.model.property.Action.DISPLAY
import net.fortuna.ical4j.util.UidGenerator
import org.jsoup.Jsoup
import java.io.File
import java.text.Normalizer
import java.time.*
import java.time.DayOfWeek.MONDAY
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.*


@Serializable(DataSerializer::class)
class Data {
    companion object {

        enum class QuarterFlag {
            START,
            FINISH
        }

        val japanZoneId = ZoneId.of("Asia/Tokyo")!!
        val dateTimeFormatter: DateTimeFormatter
            get() = DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(
                    Locale.getDefault(), "yyyyEEEMMMd H:mm"
                )
            )
        val dateFormatter: DateTimeFormatter
            get() = DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(
                    Locale.getDefault(), "yyyyEEEMMMd"
                )
            )
        val dateFormatterWithoutYear: DateTimeFormatter
            get() = DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(
                    Locale.getDefault(), "MMMd"
                )
            )
        val timeFormatter: DateTimeFormatter
            get() = DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(
                    Locale.getDefault(), "H:mm"
                )
            )

        private val defaultStartTimeOfClass = listOf(
            60 * 8 + 50,
            60 * 10 + 30,
            60 * 13 + 0,
            60 * 14 + 40,
            60 * 16 + 20
        )
        private val defaultStartDateOfQuarter = listOf(
            listOf(4, 11),
            listOf(6, 13),
            listOf(10, 3),
            listOf(12, 9)
        )
        private val defaultFinishDateOfQuarter = listOf(
            listOf(6, 10),
            listOf(8, 12),
            listOf(12, 8),
            listOf(2, 17)
        )
        private const val defaultClassTimeMinute = 90

        lateinit var syllabuses: List<Syllabus>
        var subjects = mutableListOf<Subject>()
        var schedules = mutableListOf<Schedule>()
        var useCustomTerms = false
        var nextNotificationId = 0
        var notificationIds = mutableMapOf<String, Int>()
        var startTimeOfClass = defaultStartTimeOfClass.toTypedArray()
            get() = if (useCustomTerms) field else defaultStartTimeOfClass.toTypedArray()
        var startDateOfQuarter = defaultStartDateOfQuarter.map { it.toTypedArray() }
            get() = if (useCustomTerms) field else defaultStartDateOfQuarter.map { it.toTypedArray() }
        var finishDateOfQuarter = defaultFinishDateOfQuarter.map { it.toTypedArray() }
            get() = if (useCustomTerms) field else defaultFinishDateOfQuarter.map { it.toTypedArray() }
        var subjectZoneId = ZoneId.systemDefault()!!
            get() = if (useCustomTerms) field else japanZoneId
        var classTimeMinute = defaultClassTimeMinute
            get() = if (useCustomTerms) field else defaultClassTimeMinute
        var isNotificationEnabled = true
        var notificationTimeMinute = 30

        private const val dataFileName = "data.json"
        private const val syllabusesFileName = "syllabuses.json"

        fun load(context: Context) {
            val dataFile = File(context.filesDir, dataFileName)
            if (dataFile.exists()) {
                val fis = context.openFileInput(dataFile.name)
                Json.decodeFromString<Data>(fis.bufferedReader().use { it.readText() })
            }
            val syllabusesFile = context.assets.open(syllabusesFileName)
            if (syllabusesFile.available() > 0) {
                syllabuses = Json.decodeFromString<Map<String, Map<String, Syllabus>>>(
                    syllabusesFile.bufferedReader().use { it.readText() }
                )
                    .values.map { it.values.toList() }.reduce { acc, list -> acc + list }
            }
        }

        fun save(context: Context) {
            val json = Json.encodeToString(Data())
            val fos = context.openFileOutput(dataFileName, MODE_PRIVATE)
            fos.use { it.write(json.toByteArray()) }
        }

        fun getNotificationId(): Int {
            val firstNotificationId = nextNotificationId++
            if (!notificationIds.values.contains(firstNotificationId)) {
                return firstNotificationId
            }
            while (nextNotificationId != firstNotificationId) {
                if (nextNotificationId < 0) nextNotificationId = 0
                if (!notificationIds.values.contains(nextNotificationId)) {
                    return nextNotificationId
                }
                nextNotificationId++
            }
            throw IllegalStateException("notificationIds is too big.")
        }

        fun getStartDate(year: Int, quarter: Int): LocalDate {
            val month = startDateOfQuarter[quarter - 1][0]
            val dayOfMonth = startDateOfQuarter[quarter - 1][1]
            val tempStartDate = LocalDate.of(year, month, dayOfMonth)
            val startDateOfYear = getStartDateOfYear(year)
            return if (tempStartDate < startDateOfYear) {
                tempStartDate.plusYears(1)
            } else {
                tempStartDate
            }
        }

        fun getFinishDate(year: Int, quarter: Int): LocalDate {
            val month = finishDateOfQuarter[quarter - 1][0]
            val dayOfMonth = finishDateOfQuarter[quarter - 1][1]
            val tempFinishDate = LocalDate.of(year, month, dayOfMonth)
            val startDateOfYear = getStartDateOfYear(year)
            return if (tempFinishDate < startDateOfYear) {
                tempFinishDate.plusYears(1)
            } else {
                tempFinishDate
            }
        }

        fun getStartTime(period: Int): LocalTime {
            val hour = startTimeOfClass[period - 1] / 60
            val minute = startTimeOfClass[period - 1] % 60
            return LocalTime.of(hour, minute)
        }

        fun getFinishTime(period: Int): LocalTime {
            return getStartTime(period).plusMinutes(classTimeMinute.toLong())
        }

        fun getClassTimeString(context: Context): String {
            if (Locale.getDefault().language == Locale.JAPANESE.language) {
                val time = TimeTranslator(classTimeMinute)
                with(time) {
                    if (optimizedElementType == TimeElementType.HOURS) {
                        return "${optimizedElement}時間"
                    }
                }
            }
            return getTimeString(context, classTimeMinute, R.string.class_time)
        }

        fun getCurrentAcademicYear(): Int {
            val current = LocalDate.now()
            if (current > getFinishDate(current.year - 1, 4)) {
                return current.year
            }
            return current.year - 1
        }

        fun getCurrentQuarter(): Int {
            val current = LocalDate.now()
            val year = getCurrentAcademicYear()
            return when {
                current <= getFinishDate(year, 1) -> 1
                current <= getFinishDate(year, 2) -> 2
                current <= getFinishDate(year, 3) -> 3
                current <= getFinishDate(year, 4) -> 4
                else -> 1
            }
        }

        fun setSubjectsFromHTML(html: String, context: Context) {
            val document = Jsoup.parse(html)
            val year = document
                .select("input[name=\"jikanwariSchoolYear\"]")
                .attr("value")
                .toInt()
            val semester = document
                .select("select[name=\"jikanwariSemesterCode\"]").first()
                .select("option[selected=\"selected\"]").first().text()
            val table = document
                .select("div#container table.tableBdr")
                .first().child(0)
            var period = 1
            for (tr in table.children()) {
                if (tr.className().contains("medium")) continue
                var dayOfWeek = MONDAY.value
                for (td in tr.children()) {
                    if (td.className().contains("medium")) continue
                    val cells = td.select(".timeTableSubject2")
                    for (cell in cells) {
                        val text = cell.text()
                        if (text != "") {
                            val subjectName = cell.select("a").first().text().trim()
                            val onclick = cell.select("a").first().attr("onclick")
                            val subjectParameters = onclick
                                .substring(onclick.indexOf("?") + 1 until onclick.lastIndexOf("'"))
                                .split("&")
                                .associateBy(
                                    { it.substring(0 until it.indexOf("=")) },
                                    { it.substring(it.indexOf("=") + 1) }
                                )
                            val syllabus = syllabuses.firstOrNull {
                                it.code == subjectParameters["kamokuCode"] && it.`class` == subjectParameters["classCode"]
                            }
                            val location = syllabus?.rooms ?: ""
                            val normalizedText = Normalizer.normalize(text, Normalizer.Form.NFKC)
                            val quarterText = Regex("""(第\dクォーター)""").find(normalizedText)?.value
                            val quarters = quarterText?.let {
                                listOf(Regex("""\d""").find(it)!!.value.toInt())
                            } ?: when (semester) {
                                "前期" -> listOf(1, 2)
                                "後期" -> listOf(3, 4)
                                else -> listOf()
                            }
                            for (quarter in quarters) {
                                updateSubject(
                                    subjectName,
                                    subjectParameters["kamokuCode"]!!,
                                    subjectParameters["classCode"]!!,
                                    year,
                                    quarter,
                                    dayOfWeek,
                                    period,
                                    isNotificationEnabled,
                                    notificationTimeMinute,
                                    location,
                                    "",
                                    true,
                                    context
                                )
                            }
                        }
                    }
                    dayOfWeek++
                }
                period++
            }
        }

        fun removeSchedule(
            schedule: Schedule,
            removeRelationToSubject: Boolean,
            context: Context
        ): Boolean {
            schedule.removeNotification(context)
            if (removeRelationToSubject) {
                removeScheduleIdFromSubject(schedule.id)
            }
            return schedules.remove(schedule)
        }

        fun removeAllSchedules(
            removeRelationToSubject: Boolean,
            context: Context,
            predicate: (Schedule) -> Boolean
        ): Boolean {
            var result = false
            with(schedules.iterator()) {
                while (hasNext()) {
                    val schedule = next()
                    if (predicate(schedule)) {
                        schedule.removeNotification(context)
                        if (removeRelationToSubject) {
                            removeScheduleIdFromSubject(schedule.id)
                        }
                        remove()
                        result = true
                    }
                }
            }
            return result
        }

        fun removeSubject(subject: Subject, context: Context): Boolean {
            subject.removeAllSchedules(context)
            return subjects.remove(subject)
        }

        fun removeAllSubjects(context: Context, predicate: (Subject) -> Boolean): Boolean {
            var result = false
            with(subjects.iterator()) {
                while (hasNext()) {
                    val subject = next()
                    if (predicate(subject)) {
                        subject.removeAllSchedules(context)
                        remove()
                        result = true
                    }
                }
            }
            return result
        }

        fun updateSchedule(
            id: String,
            name: String,
            startDateTime: LocalDateTime,
            finishDateTime: LocalDateTime,
            zoneId: ZoneId,
            isNotificationEnabled: Boolean,
            notificationMinute: Int,
            location: String,
            description: String,
            add: Boolean,
            context: Context? = null// If context exists, notification is set automatically.
        ): Schedule? {
            val index = schedules.indexOfFirst { it.id == id }
            if (index < 0) {
                return if (add) {
                    val schedule = Schedule(
                        id,
                        name,
                        startDateTime,
                        finishDateTime,
                        zoneId,
                        isNotificationEnabled,
                        notificationMinute,
                        location,
                        description,
                        context
                    )
                    schedules.add(schedule)
                    schedule
                } else null
            }
            with(schedules[index]) {
                if (name != this.name) {
                    removeScheduleIdFromSubject(id)
                }
                this.name = name
                this.startDateTime = startDateTime
                this.finishDateTime = finishDateTime
                this.zoneId = zoneId
                this.isNotificationEnabled = isNotificationEnabled
                this.notificationMinute = notificationMinute
                this.location = location
                this.description = description
                if (context != null) {
                    removeNotification(context)
                    setNotification(context)
                }
                return this.copy()
            }
        }

        fun updateSubject(
            name: String,
            code: String?,
            `class`: String?,
            year: Int,
            quarter: Int,
            dayOfWeek: Int,
            period: Int,
            isNotificationEnabled: Boolean,
            notificationMinute: Int,
            location: String,
            description: String,
            add: Boolean,
            context: Context? = null// If context exists, notification is set automatically.){}
        ): Subject? {
            val index = subjects.indexOfFirst {
                it.year == year
                        && it.quarter == quarter
                        && it.dayOfWeek == dayOfWeek
                        && it.period == period
            }
            if (index < 0) {
                return if (add) {
                    Subject(
                        name,
                        code,
                        `class`,
                        year,
                        quarter,
                        dayOfWeek,
                        period,
                        isNotificationEnabled,
                        notificationMinute,
                        location,
                        description,
                        context
                    ).apply {
                        subjects.add(this)
                    }
                } else return null
            }
            with(subjects[index]) {
                this.name = name
                this.code = code
                this.`class` = `class`
                this.isNotificationEnabled = isNotificationEnabled
                this.notificationMinute = notificationMinute
                this.location = location
                this.description = description
                if (context != null) {
                    removeAllSchedules(context)
                    setSchedules(context)
                }
                return this.copy()
            }
        }

        fun updateAllSubjects(context: Context) {
            for (subject in subjects) {
                with(subject) {
                    if (
                        updateSubject(
                            name,
                            code,
                            `class`,
                            year,
                            quarter,
                            dayOfWeek,
                            period,
                            isNotificationEnabled,
                            notificationMinute,
                            location,
                            description,
                            false,
                            context
                        ) == null
                    ) {
                        throw IllegalStateException("Data.subjects is corrupted")
                    }
                }
            }
        }

        fun setUseCustomTermsWithUpdate(context: Context, value: Boolean) {
            useCustomTerms = value
            updateAllSubjects(context)
        }

        fun setDateOfQuarterWithUpdate(
            context: Context,
            quarter: Int,
            flag: QuarterFlag,
            month: Int,
            dayOfMonth: Int
        ) {
            val block = { dateOfQuarter: Array<Int> ->
                with(dateOfQuarter) {
                    this[0] = month
                    this[1] = dayOfMonth
                }
                updateAllSubjects(context)
            }
            when (flag) {
                START -> block(startDateOfQuarter[quarter - 1])
                FINISH -> block(finishDateOfQuarter[quarter - 1])
            }
        }

        fun setStartTimeOfClassWithUpdate(
            context: Context,
            period: Int,
            hourOfDay: Int,
            minute: Int
        ) {
            startTimeOfClass[period - 1] = minute + hourOfDay * 60
            updateAllSubjects(context)
        }

        fun setClassTimeMinuteWithUpdate(
            context: Context,
            minute: Int
        ) {
            classTimeMinute = minute
            updateAllSubjects(context)
        }

        fun setSubjectTimeZoneWithUpdate(
            context: Context,
            zoneId: ZoneId
        ) {
            subjectZoneId = zoneId
            updateAllSubjects(context)
        }

        fun exportSchedules(context: Context, uri: Uri) {

            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, GET_META_DATA)
            val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
            val appVersion = packageInfo.versionName

            // Create a calendar
            val calendar = Calendar()
            with(calendar.properties) {
                add(ProdId("-//Hiroki Muta//$appName $appVersion//JP"))
                add(Version.VERSION_2_0)
                add(CalScale.GREGORIAN)
            }

            for (schedule in schedules) {
                with(schedule) {

                    // Create the event
                    val eventName = name
                    val start = DateTime(startDateTime.atZone(zoneId).toEpochSecond() * 1000)
                    val end = DateTime(finishDateTime.atZone(zoneId).toEpochSecond() * 1000)
                    val event = VEvent(start, end, eventName)

                    with(event) {
                        with(properties) {
                            // Create a TimeZone
                            val registry = TimeZoneRegistryFactory.getInstance().createRegistry()
                            val timezone: TimeZone = registry.getTimeZone(zoneId.id)
                            val tz: VTimeZone = timezone.vTimeZone

                            // add timezone info..
                            add(tz.timeZoneId)

                            // generate unique identifier..
                            val ug = UidGenerator { Uid(id) }
                            val uid = ug.generateUid()
                            add(uid)
                        }

                        // add an alarm
                        val alarm = if (isNotificationEnabled) {
                            VAlarm(Duration.ofMinutes(notificationMinute.toLong())).apply {
                                with(properties) {
                                    add(AUDIO)
                                    add(DISPLAY)
                                }
                            }
                        } else null
                        alarms.add(alarm)
                    }

                    // add the location
                    event.properties.add(Location(location))

                    // add the description
                    event.properties.add(Description(description))

                    // Add the event
                    calendar.components.add(event)
                }
            }

            alterDocument(context, uri, calendar.toString())
        }

        fun importSchedules(context: Context, uri: Uri) {

            val calendar = CalendarBuilder().build(context.contentResolver.openInputStream(uri))

            for (component in calendar.components) {

                val uid: Uid
                var summary: Summary? = null
                var dtStart: DtStart? = null
                var dtEnd: DtEnd? = null
                var tzId: TzId? = null
                var vAlarm: VAlarm? = null
                var location: Location? = null
                var description: Description? = null
                val result = with(component) {
                    uid = getProperty(UID) ?: Uid(UUID.randomUUID().toString())
                    summary = getProperty(SUMMARY) ?: return@with false
                    dtStart = getProperty(DTSTART) ?: return@with false
                    dtEnd = getProperty(DTEND)
                    tzId = getProperty(TZID) ?: TzId(ZoneId.systemDefault().id)
                    vAlarm = (component as? VEvent)?.alarms?.get(0)
                    location = getProperty(LOCATION) ?: Location("")
                    description = getProperty(DESCRIPTION) ?: Description("")
                    true
                }
                if (!result) continue

                val notificationMinute = vAlarm?.trigger?.run {
                    Duration.from(duration).toMinutes().toInt()
                }

                updateSchedule(
                    uid.value,
                    summary!!.value,
                    LocalDateTime.ofInstant(dtStart!!.date.toInstant(), ZoneId.of(tzId!!.value)),
                    LocalDateTime.ofInstant(dtEnd!!.date.toInstant(), ZoneId.of(tzId!!.value)),
                    ZoneId.of(tzId!!.value),
                    notificationMinute != null,
                    notificationMinute ?: notificationTimeMinute,
                    location!!.value,
                    description!!.value,
                    true,
                    context
                )

                save(context)
            }
        }

        private fun getStartDateOfYear(year: Int): LocalDate {
            val month = startDateOfQuarter[0][0]
            val dayOfMonth = startDateOfQuarter[0][1]
            return LocalDate.of(year, month, dayOfMonth)
        }

        private fun removeScheduleIdFromSubject(id: String) {
            with(subjects.iterator()) {
                while (hasNext()) {
                    next().scheduleIds.remove(id)
                }
            }
        }
    }
}

private object DataSerializer : KSerializer<Data> {
    override val descriptor: SerialDescriptor = DataSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Data) {
        val surrogate = DataSurrogate(
            subjects,
            schedules,
            useCustomTerms,
            nextNotificationId,
            notificationIds,
            startTimeOfClass.toList(),
            startDateOfQuarter.map { it.toList() },
            finishDateOfQuarter.map { it.toList() },
            subjectZoneId.id,
            classTimeMinute,
            isNotificationEnabled,
            notificationTimeMinute
        )
        encoder.encodeSerializableValue(DataSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Data {
        val surrogate = decoder.decodeSerializableValue(DataSurrogate.serializer())
        subjects = surrogate.subjects.toMutableList()
        schedules = surrogate.schedules.toMutableList()
        useCustomTerms = surrogate.useCustomTerms
        nextNotificationId = surrogate.nextNotificationId
        notificationIds = surrogate.notificationIds.toMutableMap()
        startTimeOfClass = surrogate.startTimeOfClass.toTypedArray()
        startDateOfQuarter = surrogate.startDateOfQuarter.map { it.toTypedArray() }
        finishDateOfQuarter = surrogate.finishDateOfQuarter.map { it.toTypedArray() }
        subjectZoneId = ZoneId.of(surrogate.subjectZoneId)
        isNotificationEnabled = surrogate.isNotificationEnabled
        classTimeMinute = surrogate.classTimeMinute
        notificationTimeMinute = surrogate.notificationTimeMinute
        return Data()
    }

    @Serializable
    @SerialName("Data")
    private class DataSurrogate(
        val subjects: List<Subject>,
        val schedules: List<Schedule>,
        val useCustomTerms: Boolean,
        val nextNotificationId: Int,
        val notificationIds: Map<String, Int>,
        val startTimeOfClass: List<Int>,
        val startDateOfQuarter: List<List<Int>>,
        val finishDateOfQuarter: List<List<Int>>,
        val subjectZoneId: String,
        val classTimeMinute: Int,
        val isNotificationEnabled: Boolean,
        val notificationTimeMinute: Int
    )
}