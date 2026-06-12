package com.example.data

import java.time.Instant

data class GroundStation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double, // in meters
    val isPrimary: Boolean = false
)

data class SatellitePass(
    val startEpochMs: Long,
    val endEpochMs: Long,
    val maxElevationTimeMs: Long,
    val maxElevation: Double
)

object SatellitePassCalculator {

    private const val GM = 398600.4418 // km^3 / s^2
    private const val RE = 6378.137 // km
    private const val J2 = 1.08263e-3

    fun calculateNextPass(
        tle: SatelliteTle,
        station: GroundStation,
        startEpochMs: Long = System.currentTimeMillis(),
        durationHours: Int = 24
    ): SatellitePass? {
        return calculatePasses(tle, station, startEpochMs, durationHours).firstOrNull()
    }

    fun calculatePasses(
        tle: SatelliteTle,
        station: GroundStation,
        startEpochMs: Long = System.currentTimeMillis(),
        durationHours: Int = 24
    ): List<SatellitePass> {
        val epochJd = getTleEpochJd(tle.epoch)
        
        // Settings for pass scanning
        val scanMinutes = durationHours * 60
        val stepMinutes = 1.0 // Scan every 1 minute
        
        var activePassStart: Long? = null
        var maxElevationInActivePass = -90.0
        var maxElevationTimeMsInActivePass = 0L
        
        val passes = mutableListOf<SatellitePass>()
        
        for (i in 0..scanMinutes) {
            val offsetMs = (i * stepMinutes * 60 * 1000).toLong()
            val targetTimeMs = startEpochMs + offsetMs
            val targetJd = 2440587.5 + (targetTimeMs / 86400000.0)
            
            val elevation = getSatelliteElevation(tle, epochJd, targetJd, station)
            
            if (elevation >= 0.0) {
                // Currently in a pass
                if (activePassStart == null) {
                    activePassStart = targetTimeMs
                    maxElevationInActivePass = elevation
                    maxElevationTimeMsInActivePass = targetTimeMs
                } else {
                    if (elevation > maxElevationInActivePass) {
                        maxElevationInActivePass = elevation
                        maxElevationTimeMsInActivePass = targetTimeMs
                    }
                }
            } else {
                // Elevated below horizon
                if (activePassStart != null) {
                    // Pass ended! Save it.
                    val activePassEnd = targetTimeMs
                    if (maxElevationInActivePass >= 10.0) { // Keep passes details with at least 10 degrees elevation
                        passes.add(
                            SatellitePass(
                                startEpochMs = activePassStart,
                                endEpochMs = activePassEnd,
                                maxElevationTimeMs = maxElevationTimeMsInActivePass,
                                maxElevation = maxElevationInActivePass
                            )
                        )
                    }
                    activePassStart = null
                    maxElevationInActivePass = -90.0
                }
            }
        }
        
        // If scanning completed and we were still in a pass
        if (activePassStart != null && maxElevationInActivePass >= 10.0) {
            passes.add(
                SatellitePass(
                    startEpochMs = activePassStart,
                    endEpochMs = startEpochMs + (scanMinutes * 60 * 1000),
                    maxElevationTimeMs = maxElevationTimeMsInActivePass,
                    maxElevation = maxElevationInActivePass
                )
            )
        }
        
        return passes
    }

    private fun getSatelliteElevation(
        tle: SatelliteTle,
        epochJd: Double,
        targetJd: Double,
        station: GroundStation
    ): Double {
        val dtDays = targetJd - epochJd
        val dtSecs = dtDays * 86400.0
        
        val inclination = tle.inclination.toDoubleOrNull() ?: 51.64
        val raan = tle.raan.toDoubleOrNull() ?: 0.0
        val eccentricity = tle.eccentricity.toDoubleOrNull() ?: 0.0001
        val perigee = tle.perigee.toDoubleOrNull() ?: 0.0
        val meanAnomaly = tle.meanAnomaly.toDoubleOrNull() ?: 0.0
        val meanMotion = tle.meanMotion.toDoubleOrNull() ?: 15.5
        
        // Kepler's Third Law
        val n_rad_sec = (meanMotion * 2.0 * Math.PI) / 86400.0
        val a3 = GM / (n_rad_sec * n_rad_sec)
        val a = Math.pow(a3, 1.0 / 3.0)
        
        // J2 Secular Perturbations
        val cos_i = Math.cos(Math.toRadians(inclination))
        val sin_i = Math.sin(Math.toRadians(inclination))
        val semi_latus_rectum = a * (1.0 - eccentricity * eccentricity)
        
        val secular_multiplier = 1.5 * J2 * (RE * RE) / (semi_latus_rectum * semi_latus_rectum) * n_rad_sec
        val d_omega_dt = -secular_multiplier * cos_i
        val d_w_dt = secular_multiplier * (2.0 - 2.5 * sin_i * sin_i)
        val d_M_dt = secular_multiplier * 0.5 * Math.sqrt(1.0 - eccentricity * eccentricity) * (3.0 * cos_i * cos_i - 1.0)
        
        // Updated angles
        val Omega_t = Math.toRadians(raan) + d_omega_dt * dtSecs
        val omega_t = Math.toRadians(perigee) + d_w_dt * dtSecs
        val M_t = Math.toRadians(meanAnomaly) + (n_rad_sec + d_M_dt) * dtSecs
        
        // Solve Kepler's equation
        var M_norm = M_t % (2 * Math.PI)
        if (M_norm < 0.0) M_norm += 2 * Math.PI
        
        var E = M_norm
        for (i in 0 until 10) {
            val dE = (E - eccentricity * Math.sin(E) - M_norm) / (1.0 - eccentricity * Math.cos(E))
            E -= dE
            if (Math.abs(dE) < 1e-8) break
        }
        
        // Perifocal coordinates
        val rx_peri = a * (Math.cos(E) - eccentricity)
        val ry_peri = a * Math.sqrt(1.0 - eccentricity * eccentricity) * Math.sin(E)
        
        // Rotation to ECI
        val cos_O = Math.cos(Omega_t)
        val sin_O = Math.sin(Omega_t)
        val cos_w = Math.cos(omega_t)
        val sin_w = Math.sin(omega_t)
        
        val Px = cos_w * cos_O - sin_w * sin_O * cos_i
        val Py = cos_w * sin_O + sin_w * cos_O * cos_i
        val Pz = sin_w * sin_i
        
        val Qx = -sin_w * cos_O - cos_w * sin_O * cos_i
        val Qy = -sin_w * sin_O + cos_w * cos_O * cos_i
        val Qz = cos_w * sin_i
        
        val x_eci = rx_peri * Px + ry_peri * Qx
        val y_eci = rx_peri * Py + ry_peri * Qy
        val z_eci = rx_peri * Pz + ry_peri * Qz
        
        // Rotate to ECEF by Earth's rotation angle (ERA)
        val jd_minus_j2000 = targetJd - 2451545.0
        val era_rot = 2 * Math.PI * (0.779057273264 + 1.00273781191135448 * jd_minus_j2000)
        val cos_era = Math.cos(era_rot)
        val sin_era = Math.sin(era_rot)
        
        val x_ecef = x_eci * cos_era + y_eci * sin_era
        val y_ecef = -x_eci * sin_era + y_eci * cos_era
        val z_ecef = z_eci
        
        // Observer coordinates on WGS84
        val latRad = Math.toRadians(station.latitude)
        val lngRad = Math.toRadians(station.longitude)
        val altKm = station.altitude / 1000.0
        
        val f_WGS = 1.0 / 298.257223563
        val e2 = 2 * f_WGS - f_WGS * f_WGS
        val N = RE / Math.sqrt(1.0 - e2 * Math.sin(latRad) * Math.sin(latRad))
        
        val obs_X = (N + altKm) * Math.cos(latRad) * Math.cos(lngRad)
        val obs_Y = (N + altKm) * Math.cos(latRad) * Math.sin(lngRad)
        val obs_Z = (N * (1.0 - e2) + altKm) * Math.sin(latRad)
        
        // Range Vector
        val rx = x_ecef - obs_X
        val ry = y_ecef - obs_Y
        val rz = z_ecef - obs_Z
        
        // SEZ Topocentric
        val S = Math.sin(latRad) * Math.cos(lngRad) * rx + Math.sin(latRad) * Math.sin(lngRad) * ry - Math.cos(latRad) * rz
        val East = -Math.sin(lngRad) * rx + Math.cos(lngRad) * ry
        val Z_up = Math.cos(latRad) * Math.cos(lngRad) * rx + Math.cos(latRad) * Math.sin(lngRad) * ry + Math.sin(latRad) * rz
        
        val range = Math.sqrt(S * S + East * East + Z_up * Z_up)
        if (range < 1e-3) return -90.0
        
        return Math.toDegrees(Math.asin(Z_up / range))
    }

    private fun getTleEpochJd(epochStr: String): Double {
        val trimmed = epochStr.trim()
        val yearPartVal = trimmed.substring(0, 2).toIntOrNull() ?: 26
        val fullYear = if (yearPartVal < 57) 2000 + yearPartVal else 1900 + yearPartVal
        val dayFraction = trimmed.substring(2).toDoubleOrNull() ?: 1.0
        val jan1Jd = getJulianDate(fullYear, 1, 1, 0, 0, 0)
        return jan1Jd + (dayFraction - 1.0)
    }

    private fun getJulianDate(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + (a / 4)
        val jd = (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + day + b - 1524.5
        return jd + (hour + minute / 60.0 + second / 3600.0) / 24.0
    }
}
