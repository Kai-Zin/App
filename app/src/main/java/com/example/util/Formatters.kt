package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Formatters {
    private val viLocale = Locale("vi", "VN")
    private val symbols = DecimalFormatSymbols(viLocale).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val intFormat = DecimalFormat("#,##0", symbols)
    private val decFormat = DecimalFormat("#,##0.##", symbols)

    private fun isoFmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    private fun viDateFmt() = SimpleDateFormat("dd/MM/yyyy", viLocale)

    fun money(amount: Long): String = "${intFormat.format(amount)}\u00a0₫"

    fun moneyDouble(amount: Double): String = money(amount.roundToLong())

    fun qty(q: Double): String = decFormat.format(q)

    fun formatMoneyInput(rawDigits: String): String {
        val digits = rawDigits.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val value = digits.toLongOrNull() ?: return digits
        return intFormat.format(value)
    }

    fun parseMoney(text: String): Long {
        val cleaned = text.filter { it.isDigit() || it == '-' }
        return cleaned.toLongOrNull() ?: 0L
    }

    fun parseQty(text: String): Double {
        val normalized = text.trim().replace(',', '.')
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
    }

    fun chartCompact(value: Double): String {
        val n = value.roundToLong()
        val a = abs(n)
        return when {
            a >= 1_000_000 -> "${decFormat.format(n / 1_000_000.0)}tr"
            a >= 1_000 -> "${decFormat.format(n / 1_000.0)}k"
            else -> n.toString()
        }
    }

    fun todayIso(): String = isoFmt().format(Calendar.getInstance().time)

    fun currentMonthPrefix(): String = todayIso().take(7)

    fun todayVietnameseBanner(): String {
        val cal = Calendar.getInstance()
        val dow = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Thứ Hai"
            Calendar.TUESDAY -> "Thứ Ba"
            Calendar.WEDNESDAY -> "Thứ Tư"
            Calendar.THURSDAY -> "Thứ Năm"
            Calendar.FRIDAY -> "Thứ Sáu"
            Calendar.SATURDAY -> "Thứ Bảy"
            else -> "Chủ Nhật"
        }
        return "$dow, ${cal.get(Calendar.DAY_OF_MONTH)} tháng ${cal.get(Calendar.MONTH) + 1}, ${cal.get(Calendar.YEAR)}"
    }

    fun dateVi(isoDate: String): String {
        if (isoDate.isBlank()) return "—"
        return runCatching {
            val d = isoFmt().parse(isoDate.take(10)) ?: return isoDate
            viDateFmt().format(d)
        }.getOrDefault(isoDate)
    }

    fun weekdayShort(isoDate: String): String {
        return runCatching {
            val d = isoFmt().parse(isoDate.take(10)) ?: return ""
            val cal = Calendar.getInstance().apply { time = d }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "CN"
                Calendar.MONDAY -> "T2"
                Calendar.TUESDAY -> "T3"
                Calendar.WEDNESDAY -> "T4"
                Calendar.THURSDAY -> "T5"
                Calendar.FRIDAY -> "T6"
                Calendar.SATURDAY -> "T7"
                else -> ""
            }
        }.getOrDefault("")
    }

    fun daysSince(isoDate: String?): Int? {
        if (isoDate.isNullOrBlank()) return null
        return runCatching {
            val d = isoFmt().parse(isoDate.take(10)) ?: return null
            val targetCal = Calendar.getInstance().apply {
                time = d
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val nowCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            ((nowCal.timeInMillis - targetCal.timeInMillis) / 86_400_000L).toInt()
        }.getOrNull()
    }

    fun addDaysIso(isoDate: String, days: Int): String {
        val cal = parseCalendar(isoDate)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return isoFmt().format(cal.time)
    }

    fun addMonthsIso(isoDate: String, months: Int): String {
        val cal = parseCalendar(isoDate)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, months)
        return isoFmt().format(cal.time)
    }

    fun weekStartIso(isoDate: String): String {
        val cal = parseCalendar(isoDate)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val diffFromMonday = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -diffFromMonday)
        return isoFmt().format(cal.time)
    }

    fun monthRangeIso(isoDate: String): Pair<String, String> {
        val cal = parseCalendar(isoDate)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val from = isoFmt().format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val to = isoFmt().format(cal.time)
        return from to to
    }

    fun parseCalendar(isoDate: String): Calendar {
        val cal = Calendar.getInstance()
        runCatching {
            isoFmt().parse(isoDate.take(10))?.let { cal.time = it }
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun millisToIso(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return isoFmt().format(cal.time)
    }

    fun isoToMillis(isoDate: String): Long = parseCalendar(isoDate).timeInMillis

    fun newId(prefix: String): String {
        val ts = System.currentTimeMillis().toString(36).uppercase(Locale.US)
        val rnd = (10..99).random()
        return "$prefix-$ts$rnd"
    }
}
