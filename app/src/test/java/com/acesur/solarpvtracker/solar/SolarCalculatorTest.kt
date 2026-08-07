package com.acesur.solarpvtracker.solar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.*

class SolarCalculatorTest {

    private lateinit var calculator: SolarCalculator

    @Before
    fun setUp() {
        calculator = SolarCalculator()
    }

    @Test
    fun calculateOptimalTiltAngle_atEquator_isCorrect() {
        // At the equator (lat 0) on spring equinox (day 81), declination is 0
        // Optimal tilt should be |0 - 0| = 0
        val tilt = calculator.calculateOptimalTiltAngle(0.0, 81)
        assertEquals(0.0, tilt, 0.1)
    }

    @Test
    fun calculateOptimalTiltAngle_atLatitude_isCorrect() {
        // At lat 40 on spring equinox (day 81), declination is 0
        // Optimal tilt should be |40 - 0| = 40
        val tilt = calculator.calculateOptimalTiltAngle(40.0, 81)
        assertEquals(40.0, tilt, 0.1)
    }

    @Test
    fun calculateSunPosition_atNoon_isCorrect() {
        val calendar = Calendar.getInstance()
        calendar.set(2026, Calendar.MARCH, 21, 12, 0) // Equinox at noon
        
        val position = calculator.calculateSunPosition(0.0, 0.0, calendar)
        
        // At equator, equinox, noon: altitude should be close to 90
        assertTrue("Altitude should be high at noon on equinox at equator", position.altitude > 80.0)
    }

    @Test
    fun estimateSolarRadiation_decreasesWithLatitude() {
        val equatorRadiation = calculator.estimateSolarRadiation(0.0)
        val polarRadiation = calculator.estimateSolarRadiation(70.0)
        
        assertTrue(equatorRadiation.dailyIrradiance > polarRadiation.dailyIrradiance)
    }

    @Test
    fun calculatePVOutput_isCorrect() {
        val output = calculator.calculatePVOutput(
            latitude = 40.0,
            panelWattage = 400,
            panelCount = 10,
            efficiency = 0.2f
        )
        
        // 400 * 10 = 4000W = 4kW
        // Daily radiation at lat 40 is ~4.5 kWh/m² (from estimateSolarRadiation)
        // output = 4 * 4.5 * 0.2 * 0.85 = 3.06 kWh
        assertEquals(3.06, output.dailyOutput, 0.01)
    }
}
