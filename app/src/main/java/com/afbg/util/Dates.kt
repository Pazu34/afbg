package com.afbg.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Toutes les dates « jour » de l'appli (parties, prêts) sont stockées comme
 * minuit UTC du jour local choisi : pas de décalage d'affichage selon le fuseau.
 */
object Dates {
    private const val DAY_MS = 86_400_000L
    private val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneOffset.UTC)

    fun today(): Long = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun format(dayMillis: Long): String = formatter.format(Instant.ofEpochMilli(dayMillis))

    fun daysSince(dayMillis: Long): Long = ((today() - dayMillis) / DAY_MS).coerceAtLeast(0)
}
