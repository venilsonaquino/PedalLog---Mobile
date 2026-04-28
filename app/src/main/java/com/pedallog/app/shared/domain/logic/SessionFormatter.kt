package com.pedallog.app.shared.domain.logic

import java.util.Locale

/**
 * Formata dados de uma sessão de pedal para exibição na UI.
 *
 * SRP: formatação é responsabilidade de apresentação, não do model de dados.
 * Funções puras — sem efeitos colaterais, sem estado.
 */
object SessionFormatter {

    private const val ZERO_DURATION = "00:00:00"
    private const val MILLIS_PER_SECOND = 1000L
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_PER_HOUR = 3_600_000L

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return ZERO_DURATION
        val hours = durationMs / MILLIS_PER_HOUR
        val minutes = (durationMs % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE
        val seconds = (durationMs % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatDistance(distanceKm: Double): String {
        return String.format(Locale.US, "%.2f km", distanceKm)
    }

    fun formatDate(startTime: Long): String {
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", Locale("pt", "BR"))
        return dateFormat.format(java.util.Date(startTime))
    }

    fun getSessionPeriodName(startTime: Long): String {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = startTime }
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Pedal Matinal"
            in 12..17 -> "Pedal Vespertino"
            else -> "Pedal Noturno"
        }
    }
}
