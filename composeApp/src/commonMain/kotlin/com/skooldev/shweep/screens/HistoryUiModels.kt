package com.skooldev.shweep.screens

import com.skooldev.shweep.data.Session
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal data class HistorySessionUiModel(
    val id: String,
    val dateText: String,
    val startTimeText: String,
    val durationText: String,
    val durationMinutes: Int?,
    val sheepCount: Int,
    val displayedSheepIcons: Int,
    val hasOverflowingFlock: Boolean
)

internal data class HistorySummaryUiModel(
    val totalNights: Int,
    val averageDurationText: String,
    val averageDurationMinutes: Int?,
    val averageSheepCount: Int
)

@OptIn(ExperimentalTime::class)
internal fun List<Session>.toHistoryUiModels(
    timeZone: TimeZone
): List<HistorySessionUiModel> = this
    .sortedByDescending { it.startTime }
    .map { session ->
        val startInstant = Instant.fromEpochMilliseconds(session.startTime)
        val localDateTime = startInstant.toLocalDateTime(timeZone)

        val dayName = localDateTime.dayOfWeek.name.lowercase()
            .replaceFirstChar { it.uppercase() }
        val month = localDateTime.monthNumber
        val day = localDateTime.dayOfMonth
        val year = localDateTime.year
        val dateText = "$dayName, $month/$day/$year"

        val startTimeText = "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"

        val durationMillis = session.endTime - session.startTime
        val durationText = formatSleepDuration(durationMillis)
        val durationMinutes = if (durationMillis >= 0L) {
            (durationMillis / 60_000.0).roundToInt().coerceAtLeast(1)
        } else null

        val icons = flockIconCount(session.sheepCount)

        HistorySessionUiModel(
            id = session.id,
            dateText = dateText,
            startTimeText = startTimeText,
            durationText = durationText,
            durationMinutes = durationMinutes,
            sheepCount = session.sheepCount,
            displayedSheepIcons = icons,
            hasOverflowingFlock = session.sheepCount > 50
        )
    }

internal fun List<Session>.toHistorySummaryUiModel(): HistorySummaryUiModel {
    val validDurations = mapNotNull { session ->
        val duration = session.endTime - session.startTime
        duration.takeIf { it >= 0L }
    }

    val averageDurationMillis = if (validDurations.isNotEmpty()) {
        validDurations.average()
    } else null

    val averageDurationText = averageDurationMillis?.let {
        formatSleepDuration(it.toLong())
    } ?: "—"

    val averageDurationMinutes = averageDurationMillis?.let {
        (it / 60_000.0).roundToInt().coerceAtLeast(1)
    }

    val averageSheepCount = if (isNotEmpty()) {
        map { it.sheepCount }.average().roundToInt()
    } else 0

    return HistorySummaryUiModel(
        totalNights = size,
        averageDurationText = averageDurationText,
        averageDurationMinutes = averageDurationMinutes,
        averageSheepCount = averageSheepCount
    )
}

internal fun formatSleepDuration(durationMillis: Long): String {
    if (durationMillis < 0) return "—"

    val totalSeconds = durationMillis / 1000
    if (totalSeconds < 60) return "< 1 min"

    val totalMinutes = (durationMillis / 60_000.0).roundToInt()

    if (totalMinutes < 60) {
        return "$totalMinutes min"
    }

    val hours = totalMinutes / 60
    val remainingMinutes = totalMinutes % 60
    return if (remainingMinutes == 0) {
        "$hours h"
    } else {
        "$hours h $remainingMinutes min"
    }
}

internal fun flockIconCount(sheepCount: Int): Int = when {
    sheepCount <= 0 -> 0
    sheepCount <= 10 -> 1
    sheepCount <= 20 -> 2
    sheepCount <= 30 -> 3
    sheepCount <= 40 -> 4
    else -> 5
}
