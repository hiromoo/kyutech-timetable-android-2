package io.github.hiromoo.kyutechtimetable.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Syllabus(
    @SerialName("科目名")
    val name: String,
    @SerialName("科目コード")
    val code: String,
    @SerialName("担当教員")
    val teachers: String,
    @SerialName("開講年度")
    val year: String,
    @SerialName("対象学年")
    val grade: String,
    @SerialName("開講学期")
    val term: String,
    @SerialName("クラス")
    val `class`: String,
    @SerialName("曜日・時限")
    val dayOfWeeksAndPeriods: String,
    @SerialName("講義室")
    val rooms: String,
    @SerialName("更新日")
    val lastUpdated: String,
    @SerialName("単位")
    val credits: Map<String, List<String>>
) {
    val nameInJapanese: String
        get() {
            return name.replace("\\(.*?\\)".toRegex(), "").trim()
        }
}
