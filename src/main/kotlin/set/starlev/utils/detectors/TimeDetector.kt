package set.starlev.utils.detectors

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TimeDetector {
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy")

    /**
     * Возвращает текущее время в формате HH:mm:ss
     */
    fun getTime(): String = LocalDateTime.now().format(TIME_FORMATTER)

    /**
     * Возвращает текущую дату в формате dd.MM.yyyy
     */
    fun getDate(): String = LocalDateTime.now().format(DATE_FORMATTER)

    /**
     * Возвращает текущий год в формате yyyy
     */
    fun getYear(): String = LocalDateTime.now().format(YEAR_FORMATTER)

    /**
     * Возвращает полную строку (время + дата)
     */
    fun getFullDateTime(): String = "${getTime()} (${getDate()})"
}
