package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class CelestrakRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun fetchTleByCatalogNumber(catnr: String): Result<List<SatelliteTle>> = withContext(Dispatchers.IO) {
        val url = "https://celestrak.org/NORAD/elements/gp.php?CATNR=$catnr&FORMAT=tle"
        executeTleRequest(url)
    }

    suspend fun fetchTleByName(name: String): Result<List<SatelliteTle>> = withContext(Dispatchers.IO) {
        val url = "https://celestrak.org/NORAD/elements/gp.php?NAME=$name&FORMAT=tle"
        executeTleRequest(url)
    }

    private fun executeTleRequest(url: String): Result<List<SatelliteTle>> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "TLEFinder-Android-App")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("خطا در ارتباط با سرور Celestrak (کد خطا: ${response.code})"))
                }
                val bodyText = response.body?.string() ?: ""
                if (bodyText.isBlank() || bodyText.contains("No GP data found", ignoreCase = true)) {
                    return Result.failure(Exception("ماهواره‌ای با این مشخصات یافت نشد."))
                }

                val satellites = parseCelestrakTleResponse(bodyText)
                if (satellites.isEmpty()) {
                    return Result.failure(Exception("اطلاعات TLE معتبری پیدا نشد."))
                }
                return Result.success(satellites)
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun parseCelestrakTleResponse(responseBody: String): List<SatelliteTle> {
        val lines = responseBody.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val list = mutableListOf<SatelliteTle>()
        
        // Loop through lines grouping by 3
        var i = 0
        while (i < lines.size - 2) {
            val lineA = lines[i]
            val lineB = lines[i+1]
            val lineC = lines[i+2]
            
            if (lineB.startsWith("1 ") && lineC.startsWith("2 ")) {
                list.add(SatelliteTle(lineA, lineB, lineC))
                i += 3
            } else {
                // If the lines don't line up perfectly, skip one line and continue to stabilize scanning
                i++
            }
        }
        return list
    }
}
