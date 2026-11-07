package com.alfleyla.zeituna.utils

import kotlinx.datetime.*

object DateTimeUtils {
    /**
     * Safely resolves a TimeZone with specific fallbacks for Wasm/JS environments.
     */
    fun safeTimeZone(tz: String?, preferMadrid: Boolean = false): TimeZone {
        // 1. Try provided timezone string
        if (!tz.isNullOrBlank()) {
            try {
                val resolved = TimeZone.of(tz)
                if (resolved.id != "UTC" || tz.equals("UTC", ignoreCase = true)) return resolved
            } catch (t: Throwable) {}
        }

        // 2. If we are looking for the teacher's zone (Madrid)
        if (preferMadrid) {
            try {
                val resolved = TimeZone.of("Europe/Madrid")
                if (resolved.id != "UTC") return resolved
            } catch (t: Throwable) {}

            val month = Clock.System.now().toLocalDateTime(TimeZone.UTC).month
            return if (month in Month.APRIL..Month.OCTOBER) {
                TimeZone.of("UTC+02:00")
            } else {
                TimeZone.of("UTC+01:00")
            }
        }

        // 3. Try platform specific ID first
        val platformId = platformGetTimeZoneId()
        if (platformId != "SYSTEM" && platformId != "UTC") {
            try { 
                val resolved = TimeZone.of(platformId)
                if (resolved.id != "UTC") return resolved
            } catch (t: Throwable) {}
        }

        // 4. Try System Default
        try {
            return TimeZone.currentSystemDefault()
        } catch (t: Throwable) {}

        return TimeZone.UTC
    }

    /**
     * Returns a descriptive ID for logging, using platform lookup if the TimeZone ID is generic.
     */
    fun getDisplayId(tz: TimeZone): String {
        val id = tz.id
        if (id == "SYSTEM" || id == "UTC" || id.startsWith("FixedOffset")) {
            val pId = platformGetTimeZoneId()
            if (pId != "SYSTEM" && pId != "UTC") return pId
        }
        return id
    }

    fun parseLocalTimeSafe(timeStr: String): LocalTime? {
        return try {
            val trimmed = timeStr.trim().take(5)
            val parts = trimmed.split(":")
            if (parts.size >= 2) {
                LocalTime(parts[0].toInt(), parts[1].toInt())
            } else {
                LocalTime(trimmed.toInt(), 0)
            }
        } catch (t: Throwable) { null }
    }

    /**
     * Converts a teacher's local time to the user's local time.
     */
    fun convertTeacherToLocal(
        dateStr: String, 
        timeStr: String, 
        teacherTZStr: String?, 
        localTZ: TimeZone
    ): LocalDateTime? {
        if (dateStr.isBlank() || timeStr.isBlank()) return null
        
        try {
            val date = LocalDate.parse(dateStr.trim())
            val time = parseLocalTimeSafe(timeStr) ?: return null
            val teacherTZ = safeTimeZone(teacherTZStr, preferMadrid = true)
            
            // If teacher and local timezones match (or are both Europe/Madrid), no shift needed!
            if (teacherTZ.id == localTZ.id || (teacherTZ.id == "Europe/Madrid" && localTZ.id == "Europe/Madrid")) {
                return LocalDateTime(date, time)
            }

            val teacherDT = LocalDateTime(date, time)
            val teacherInstant = try {
                teacherDT.toInstant(teacherTZ)
            } catch (t: Throwable) {
                teacherDT.toInstant(TimeZone.UTC)
            }
            
            val teacherOffset = try { teacherTZ.offsetAt(teacherInstant) } catch (t: Throwable) { UtcOffset.ZERO }
            val localOffset = try { localTZ.offsetAt(teacherInstant) } catch (t: Throwable) { UtcOffset.ZERO }
            
            val diffSeconds = localOffset.totalSeconds - teacherOffset.totalSeconds
            if (diffSeconds == 0) {
                return teacherDT
            }

            val utcInstant = teacherDT.toInstant(TimeZone.UTC)
            val adjustedInstant = utcInstant.plus(diffSeconds, DateTimeUnit.SECOND)
            return adjustedInstant.toLocalDateTime(TimeZone.UTC)
        } catch (t: Throwable) {
            return null
        }
    }
}
