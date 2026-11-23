package com.example.lab18.task1

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    const val PATTERN = "dd.MM.yyyy"
    const val ARCHIVE_YEARS = 4

    fun formatter() = SimpleDateFormat(PATTERN, Locale.US).apply { isLenient = false }

    fun today(): String = formatter().format(Date())

    /** null — дата коректна; інакше текст помилки (31.02.2026, майбутнє, старше 4 років) */
    fun validate(date: String): String? {
        val parsed = try {
            formatter().parse(date)
        } catch (e: ParseException) {
            null
        } ?: return "Некоректна дата «$date». Формат: дд.ММ.рррр"

        val minDate = Calendar.getInstance().apply { add(Calendar.YEAR, -ARCHIVE_YEARS) }.time
        return when {
            parsed.after(Date()) -> "Не можна обрати майбутню дату"
            parsed.before(minDate) -> "Архів доступний лише за останні $ARCHIVE_YEARS роки"
            else -> null
        }
    }
}
