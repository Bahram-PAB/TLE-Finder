package com.example.data

data class SatelliteTle(
    val name: String,
    val line1: String,
    val line2: String
) {
    val rawTle: String get() = "$name\n$line1\n$line2"

    // Metadata parsed from line1
    val noradId: String by lazy {
        line1.substringSafe(2, 7).trim()
    }

    val classification: String by lazy {
        val raw = line1.substringSafe(7, 8).trim().uppercase()
        when (raw) {
            "U" -> "عمومی (Unclassified)"
            "C" -> "محرمانه (Confidential)"
            "S" -> "سری (Secret)"
            else -> raw.ifEmpty { "نامشخص (Unknown)" }
        }
    }

    val designator: String by lazy {
         line1.substringSafe(9, 17).trim()
    }

    val epoch: String by lazy {
         line1.substringSafe(18, 32).trim()
    }

    // Metadata parsed from line2
    val inclination: String by lazy {
         line2.substringSafe(8, 16).trim()
    }

    val raan: String by lazy {
         line2.substringSafe(17, 25).trim()
    }

    val eccentricityText: String by lazy {
         line2.substringSafe(26, 33).trim()
    }

    val eccentricity: String by lazy {
         if (eccentricityText.isEmpty()) "0.0" else "0.$eccentricityText"
    }

    val perigee: String by lazy {
         line2.substringSafe(34, 42).trim()
    }

    val meanAnomaly: String by lazy {
         line2.substringSafe(43, 51).trim()
    }

    val meanMotion: String by lazy {
         line2.substringSafe(52, 63).trim()
    }

    val revNumber: String by lazy {
         line2.substringSafe(63, 68).trim()
    }

    val epochDate: String by lazy {
        parseTleEpochToDateString(epoch)
    }

    fun generateHistory(count: Int = 5): List<SatelliteTle> {
        val result = mutableListOf<SatelliteTle>()
        try {
            val epochStr = this.epoch
            if (epochStr.length < 5) {
                for (i in 0 until count) {
                    result.add(this)
                }
                return result
            }
            val yearPart = epochStr.substring(0, 2).toIntOrNull() ?: 26
            val dayPartStr = epochStr.substring(2)
            val dayPart = dayPartStr.toDoubleOrNull() ?: 163.29215037

            for (i in 0 until count) {
                var dummyDay = dayPart - (i * 1.5)
                var dummyYear = yearPart
                if (dummyDay < 1.0) {
                    dummyYear -= 1
                    val isLeap = (2000 + dummyYear) % 4 == 0
                    dummyDay += if (isLeap) 366.0 else 365.0
                }

                val newEpochFormatted = String.format(java.util.Locale.US, "%02d%012.8f", dummyYear, dummyDay)

                val prefix1 = line1.substringSafe(0, 18).padEnd(18, ' ')
                val suffix1BeforeChecksum = line1.substringSafe(32, 68).padEnd(36, ' ')

                val line1WithoutChecksum = prefix1 + newEpochFormatted + suffix1BeforeChecksum
                val checksum1 = calculateTleChecksum(line1WithoutChecksum)
                val newLine1 = line1WithoutChecksum + checksum1.toString()

                val meanMotionVal = this.meanMotion.toDoubleOrNull() ?: 15.5
                val currentRev = this.revNumber.toIntOrNull() ?: 10000
                val revOffset = (meanMotionVal * i * 1.5).toInt()
                val newRev = maxOf(0, currentRev - revOffset)
                val formattedRev = String.format(java.util.Locale.US, "%5d", newRev)

                val prefix2 = line2.substringSafe(0, 63).padEnd(63, ' ')
                val line2WithoutChecksum = prefix2 + formattedRev
                val checksum2 = calculateTleChecksum(line2WithoutChecksum)
                val newLine2 = line2WithoutChecksum + checksum2.toString()

                result.add(SatelliteTle(this.name, newLine1, newLine2))
            }
        } catch (e: Exception) {
            for (i in 0 until count) {
                result.add(this)
            }
        }
        return result
    }
}

// Extra safety substring
private fun String.substringSafe(startIndex: Int, endIndex: Int): String {
    if (startIndex >= this.length) return ""
    val adjustedEnd = minOf(endIndex, this.length)
    if (startIndex >= adjustedEnd) return ""
    return this.substring(startIndex, adjustedEnd)
}

private fun calculateTleChecksum(line: String): Int {
    var sum = 0
    for (i in 0 until line.length) {
        val char = line[i]
        if (char.isDigit()) {
            sum += char.toString().toInt()
        } else if (char == '-') {
            sum += 1
        }
    }
    return sum % 10
}

private fun parseTleEpochToDateString(epochStr: String): String {
    try {
        val trimmed = epochStr.trim()
        if (trimmed.length < 5) return "تاریخ نامشخص"

        val yearPart = trimmed.substring(0, 2).toIntOrNull() ?: return "تاریخ نامشخص"
        val dayPartStr = trimmed.substring(2)
        val dayPartDecimal = dayPartStr.toDoubleOrNull() ?: return "تاریخ نامشخص"

        val fullYear = if (yearPart < 57) 2000 + yearPart else 1900 + yearPart
        val dayOfYear = dayPartDecimal.toInt()

        val date = java.time.LocalDate.of(fullYear, 1, 1).plusDays(dayOfYear.toLong() - 1)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.ENGLISH)
        return date.format(formatter)
    } catch (e: Exception) {
        return "تاریخ نامشخص"
    }
}
