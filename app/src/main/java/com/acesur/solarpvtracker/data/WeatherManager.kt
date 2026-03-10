package com.acesur.solarpvtracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

import com.acesur.solarpvtracker.R

data class WeatherData(
    val cloudCover: Int,
    val visibility: Double,
    val uvIndex: Double,
    val humidity: Int,
    val dailyForecast: List<DailyForecast>,
    val lastUpdateTime: String // "HH:mm"
)

data class DailyForecast(
    val dayResId: Int,
    val dateDisplay: String, // "DD/MM"
    val statusResId: Int,
    val yield: Double, // kWh/m²
    val weatherCode: Int
)

class WeatherManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getWeatherData(lat: Double, lon: Double): Pair<WeatherData, String>? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=cloud_cover,relative_humidity_2m,visibility" +
                "&daily=weather_code,uv_index_max,shortwave_radiation_sum" +
                "&timezone=auto"

        val request = Request.Builder().url(url).build()

        try {
            val jsonStr = fetchUrl(request)
            val data = parseWeatherData(jsonStr)
            if (data != null) data to jsonStr else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseWeatherData(jsonStr: String): WeatherData? {
        return try {
            val json = JSONObject(jsonStr)

            val current = json.getJSONObject("current")
            val cloudCover = current.getInt("cloud_cover")
            val visibility = current.getDouble("visibility") / 1000.0 // Convert to km
            val humidity = current.getInt("relative_humidity_2m")

            val daily = json.getJSONObject("daily")
            val time = daily.getJSONArray("time")
            val weatherCodes = daily.getJSONArray("weather_code")
            val uvMax = daily.getJSONArray("uv_index_max")
            val radiationSum = daily.getJSONArray("shortwave_radiation_sum")

            val forecast = mutableListOf<DailyForecast>()
            
            val calendar = java.util.Calendar.getInstance()
            
            for (i in 0 until 5) {
                val code = weatherCodes.getInt(i)
                val rad = radiationSum.getDouble(i) / 3.6 // MJ/m² to kWh/m²
                
                // Get day name
                val dateParts = time.getString(i).split("-")
                calendar.set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt())
                val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                
                val dayResId = when (dayOfWeek) {
                    java.util.Calendar.SUNDAY -> R.string.sunday
                    java.util.Calendar.MONDAY -> R.string.monday
                    java.util.Calendar.TUESDAY -> R.string.tuesday
                    java.util.Calendar.WEDNESDAY -> R.string.wednesday
                    java.util.Calendar.THURSDAY -> R.string.thursday
                    java.util.Calendar.FRIDAY -> R.string.friday
                    java.util.Calendar.SATURDAY -> R.string.saturday
                    else -> R.string.app_name
                }
                
                val dateDisplay = String.format("%02d/%02d", dateParts[2].toInt(), dateParts[1].toInt())

                forecast.add(
                    DailyForecast(
                        dayResId = dayResId,
                        dateDisplay = dateDisplay,
                        statusResId = getWeatherStatusResId(code),
                        yield = rad,
                        weatherCode = code
                    )
                )
            }

            val lastUpdate = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

            WeatherData(
                cloudCover = cloudCover,
                visibility = visibility,
                uvIndex = uvMax.getDouble(0), // Today's UV
                humidity = humidity,
                dailyForecast = forecast,
                lastUpdateTime = lastUpdate
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchUrl(request: Request): String = suspendCancellableCoroutine { continuation ->
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        continuation.resumeWithException(IOException("Unexpected code $response"))
                        return
                    }
                    continuation.resume(response.body?.string() ?: "")
                }
            }
        })
    }

    private fun getWeatherStatusResId(code: Int): Int {
        return when (code) {
            0 -> R.string.status_sunny // Clear sky
            1, 2, 3 -> R.string.status_fair // Mainly clear, partly cloudy, and overcast
            45, 48 -> R.string.status_foggy
            51, 53, 55 -> R.string.status_drizzle
            61, 63, 65 -> R.string.status_rainy
            71, 73, 75 -> R.string.status_snowy
            80, 81, 82 -> R.string.status_rain_showers
            95, 96, 99 -> R.string.status_thunderstorm
            else -> R.string.status_clear
        }
    }
}
