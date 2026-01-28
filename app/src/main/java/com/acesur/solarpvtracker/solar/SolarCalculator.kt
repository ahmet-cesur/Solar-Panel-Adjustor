package com.acesur.solarpvtracker.solar

import java.util.*
import kotlin.math.*
import com.acesur.solarpvtracker.R

data class SunPosition(
    val altitude: Double,  // Elevation angle above horizon in degrees
    val azimuth: Double    // Compass direction in degrees (0 = North, 90 = East)
)

data class SolarRadiation(
    val dailyIrradiance: Double,     // kWh/m² per day
    val monthlyIrradiance: Double,   // kWh/m² per month
    val yearlyIrradiance: Double     // kWh/m² per year
)

data class PVOutput(
    val dailyOutput: Double,    // kWh per day
    val monthlyOutput: Double,  // kWh per month
    val yearlyOutput: Double    // kWh per year
)

data class OptimalTiltAngle(
    val angle: Double,
    val description: String = "",
    val descriptionResId: Int? = null
)

data class DailyTiltAngle(
    val dayOfYear: Int,
    val date: String,
    val optimalAngle: Double
)

data class MonthlyTiltAngle(
    val month: Int,
    val monthName: String = "",
    val optimalAngle: Double,
    val notes: String = "",
    val notesResId: Int? = null
)

class SolarCalculator {
    
    companion object {
        private const val SOLAR_CONSTANT = 1361.0  // W/m² (Solar constant)
        private const val AVERAGE_PEAK_SUN_HOURS = 5.0  // Average hours of peak sunlight
    }
    
    /**
     * Calculate sun position at a given location, date and time
     */
    fun calculateSunPosition(
        latitude: Double,
        longitude: Double,
        calendar: Calendar = Calendar.getInstance()
    ): SunPosition {
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0
        
        // Calculate declination angle
        val declination = 23.45 * sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 81)))
        
        // Calculate hour angle
        val solarNoon = 12.0  // Simplified, assuming local solar time
        val hourAngle = 15.0 * (hour - solarNoon)
        
        // Calculate altitude (elevation angle)
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)
        val haRad = Math.toRadians(hourAngle)
        
        val sinAltitude = sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(haRad)
        val altitude = Math.toDegrees(asin(sinAltitude))
        
        // Calculate azimuth
        val cosAzimuth = (sin(decRad) - sin(latRad) * sinAltitude) / (cos(latRad) * cos(asin(sinAltitude)))
        var azimuth = Math.toDegrees(acos(cosAzimuth.coerceIn(-1.0, 1.0)))
        
        if (hourAngle > 0) {
            azimuth = 360.0 - azimuth
        }
        
        return SunPosition(
            altitude = altitude.coerceIn(-90.0, 90.0),
            azimuth = azimuth
        )
    }
    
    /**
     * Calculate optimal tilt angle for a specific day of the year
     */
    fun calculateOptimalTiltAngle(latitude: Double, dayOfYear: Int): Double {
        // Calculate solar declination
        val declination = 23.45 * sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 81)))
        
        // Optimal tilt = |latitude - declination|
        // If result is negative, it implies facing away from equator, which we avoid by taking absolute value
        // The panel usually faces the equator (South in Northern Hemisphere, North in Southern Hemisphere)
        val optimalTilt = abs(latitude - declination)
        
        return optimalTilt.coerceIn(0.0, 90.0)
    }
    
    /**
     * Calculate optimal tilt angle for a specific month
     */
    fun calculateMonthlyOptimalTilt(latitude: Double, month: Int): MonthlyTiltAngle {
        // Calculate middle day of month for average tilt
        val middleDayOfMonth = when (month) {
            1 -> 15
            2 -> 46
            3 -> 74
            4 -> 105
            5 -> 135
            6 -> 166
            7 -> 196
            8 -> 227
            9 -> 258
            10 -> 288
            11 -> 319
            12 -> 349
            else -> 1
        }
        
        val optimalAngle = calculateOptimalTiltAngle(latitude, middleDayOfMonth)
        
        val notesResId = when {
            month in listOf(6, 7, 8) && latitude > 0 -> R.string.summer_lower_tilt
            month in listOf(12, 1, 2) && latitude > 0 -> R.string.winter_higher_tilt
            month in listOf(6, 7, 8) && latitude < 0 -> R.string.winter_higher_tilt
            month in listOf(12, 1, 2) && latitude < 0 -> R.string.summer_lower_tilt
            else -> R.string.transition_season
        }
        
        return MonthlyTiltAngle(
            month = month,
            optimalAngle = optimalAngle,
            notesResId = notesResId
        )
    }
    
    /**
     * Generate optimal tilt angles for all 365 days
     */
    fun generateDailyTiltAngles(latitude: Double, year: Int = Calendar.getInstance().get(Calendar.YEAR)): List<DailyTiltAngle> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        
        val dailyAngles = mutableListOf<DailyTiltAngle>()
        val isLeapYear = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        val daysInYear = if (isLeapYear) 366 else 365
        
        for (dayOfYear in 1..daysInYear) {
            calendar.set(Calendar.DAY_OF_YEAR, dayOfYear)
            
            val dateStr = String.format(
                "%02d/%02d/%d",
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                year
            )
            
            val optimalAngle = calculateOptimalTiltAngle(latitude, dayOfYear)
            
            dailyAngles.add(
                DailyTiltAngle(
                    dayOfYear = dayOfYear,
                    date = dateStr,
                    optimalAngle = optimalAngle
                )
            )
        }
        
        return dailyAngles
    }
    
    /**
     * Generate optimal tilt angles for all 12 months
     */
    fun generateMonthlyTiltAngles(latitude: Double): List<MonthlyTiltAngle> {
        return (1..12).map { month ->
            calculateMonthlyOptimalTilt(latitude, month)
        }
    }
    
    /**
     * Get seasonal tilt recommendations
     */
    fun getSeasonalTiltAngles(latitude: Double): List<OptimalTiltAngle> {
        val absLatitude = abs(latitude)
        
        return listOf(
            OptimalTiltAngle(
                angle = absLatitude - 15.0,
                descriptionResId = R.string.summer_description
            ),
            OptimalTiltAngle(
                angle = absLatitude,
                descriptionResId = R.string.spring_fall_description
            ),
            OptimalTiltAngle(
                angle = absLatitude + 15.0,
                descriptionResId = R.string.winter_description
            ),
            OptimalTiltAngle(
                angle = absLatitude,
                descriptionResId = R.string.year_round_description
            )
        )
    }
    
    /**
     * Estimate solar radiation based on location
     */
    fun estimateSolarRadiation(latitude: Double): SolarRadiation {
        // Simplified estimation based on latitude
        // More accurate data would come from NASA or local weather services
        
        val absLatitude = abs(latitude)
        
        // Base daily irradiance decreases with latitude
        val dailyIrradiance = when {
            absLatitude < 25 -> 6.5
            absLatitude < 35 -> 5.5
            absLatitude < 45 -> 4.5
            absLatitude < 55 -> 3.5
            else -> 2.5
        }
        
        return SolarRadiation(
            dailyIrradiance = dailyIrradiance,
            monthlyIrradiance = dailyIrradiance * 30,
            yearlyIrradiance = dailyIrradiance * 365
        )
    }
    
    /**
     * Calculate PV output based on panel specs and location
     */
    fun calculatePVOutput(
        latitude: Double,
        panelWattage: Int,
        panelCount: Int,
        efficiency: Float
    ): PVOutput {
        val radiation = estimateSolarRadiation(latitude)
        val systemSizeKw = (panelWattage * panelCount) / 1000.0
        
        // Daily output = System size (kW) × Peak sun hours × Efficiency × Performance ratio
        val performanceRatio = 0.85  // Typical system losses
        
        val dailyOutput = systemSizeKw * radiation.dailyIrradiance * efficiency * performanceRatio
        
        return PVOutput(
            dailyOutput = dailyOutput,
            monthlyOutput = dailyOutput * 30,
            yearlyOutput = dailyOutput * 365
        )
    }
}
