package com.iicytower.wanderlist.data.local

import com.iicytower.wanderlist.domain.model.OpeningHours
import com.iicytower.wanderlist.domain.model.TimeSlot
import java.time.LocalDateTime

/**
 * Uproszczony parser wyrażeń OSM `opening_hours` (MVP): `24/7`, reguły rozdzielane `;`
 * w postaci `Mo-Fr 09:00-17:00`, `Mo,We,Fr 10:00-18:00`, `Sa 10:00-14:00,15:00-18:00`, `Su off`.
 * Format spoza podzbioru → zachowujemy tylko surowy string (slots = null).
 */
object OsmOpeningHoursParser {

    private val DAYS = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    private val TIME_RANGE = Regex("""(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})""")

    fun parse(raw: String, now: LocalDateTime = LocalDateTime.now()): OpeningHours {
        val slots = parseSlots(raw.trim())
        return OpeningHours(
            raw = raw,
            slots = slots,
            isOpenNow = slots?.let { isOpenAt(it, now) }
        )
    }

    fun isOpenAt(slots: List<TimeSlot>, now: LocalDateTime): Boolean {
        val day = now.dayOfWeek.value
        val minutes = now.hour * 60 + now.minute
        return slots.any { slot ->
            slot.dayOfWeek == day && when {
                slot.closeMinutes > slot.openMinutes -> minutes >= slot.openMinutes && minutes < slot.closeMinutes
                else -> minutes >= slot.openMinutes || minutes < slot.closeMinutes // przez północ
            }
        }
    }

    private fun parseSlots(raw: String): List<TimeSlot>? {
        if (raw.isEmpty()) return null
        if (raw == "24/7") return (1..7).map { TimeSlot(it, 0, 24 * 60) }

        val slots = mutableListOf<TimeSlot>()
        for (rulePart in raw.split(";")) {
            val rule = rulePart.trim()
            if (rule.isEmpty()) continue

            val firstSpace = rule.indexOf(' ')
            val daySpec: String
            val timeSpec: String
            if (firstSpace < 0) {
                if (rule.endsWith("off", ignoreCase = true)) continue
                return null
            } else {
                daySpec = rule.substring(0, firstSpace)
                timeSpec = rule.substring(firstSpace + 1).trim()
            }

            val days = parseDays(daySpec) ?: return null
            if (timeSpec.equals("off", ignoreCase = true) || timeSpec.equals("closed", ignoreCase = true)) continue

            val ranges = timeSpec.split(",").map { it.trim() }
            for (range in ranges) {
                val m = TIME_RANGE.matchEntire(range) ?: return null
                val (oh, om, ch, cm) = m.destructured
                val open = oh.toInt() * 60 + om.toInt()
                val close = ch.toInt() * 60 + cm.toInt()
                if (oh.toInt() > 24 || ch.toInt() > 24) return null
                days.forEach { slots += TimeSlot(it, open, close) }
            }
        }
        return slots.takeIf { it.isNotEmpty() }
    }

    private fun parseDays(spec: String): List<Int>? {
        val days = mutableListOf<Int>()
        for (part in spec.split(",")) {
            val p = part.trim()
            if (p == "PH" || p == "SH") continue // święta poza zakresem MVP
            if (p.contains("-")) {
                val (from, to) = p.split("-").takeIf { it.size == 2 } ?: return null
                val fromIdx = DAYS.indexOf(from.trim())
                val toIdx = DAYS.indexOf(to.trim())
                if (fromIdx < 0 || toIdx < 0) return null
                if (fromIdx <= toIdx) {
                    (fromIdx..toIdx).forEach { days += it + 1 }
                } else {
                    // zakres przez niedzielę, np. Sa-Mo
                    (fromIdx..6).forEach { days += it + 1 }
                    (0..toIdx).forEach { days += it + 1 }
                }
            } else {
                val idx = DAYS.indexOf(p)
                if (idx < 0) return null
                days += idx + 1
            }
        }
        return days.takeIf { it.isNotEmpty() }
    }
}
