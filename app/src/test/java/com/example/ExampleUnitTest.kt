package com.example

import com.example.data.SatelliteTle
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSatelliteTleHistoryGeneration() {
    // A standard real ISS TLE example
    val sampleLineA = "ISS (ZARYA)"
    val sampleLine1 = "1 25544U 98067A   26163.29215037  .00016717  00000-0  30198-3 0  9997"
    val sampleLine2 = "2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.5005822154211"

    val tle = SatelliteTle(sampleLineA, sampleLine1, sampleLine2)
    val history = tle.generateHistory(5)

    // Check we get exactly 5 elements
    assertEquals(5, history.size)

    // Verify epochs and date transitions are logical
    val latestDate = history[0].epochDate
    val oldestDate = history[4].epochDate
    
    assertEquals("2026-06-12", latestDate)
    
    // Ensure the date of older history elements is earlier than the latest
    assertNotEquals(latestDate, oldestDate)
    assertTrue(oldestDate < latestDate)

    // Match checksum calculation
    for (hist in history) {
        assertEquals(69, hist.line1.length)
        assertEquals(69, hist.line2.length)
        
        // Checksum matches manually computed modulo 10 checksum
        val expectedSum1 = computeManualTleChecksum(hist.line1.substring(0, 68))
        val actualSum1 = hist.line1.last().toString().toInt()
        assertEquals(expectedSum1, actualSum1)
    }
  }

  private fun computeManualTleChecksum(line: String): Int {
    var sum = 0
    for (char in line) {
        if (char.isDigit()) {
            sum += char.toString().toInt()
        } else if (char == '-') {
            sum += 1
        }
    }
    return sum % 10
  }
}
