package com.example.speedometer_nextgen_1

import android.content.Context
import androidx.core.content.ContextCompat
import kotlin.math.abs

class SpeedManagement(context: Context) {

    var previousSpeed: Int? = null
    private var previousCategory: SpeedCategory? = null
    private var categoryChangedFlag = false

    private val colorUnder30 = ContextCompat.getColor(context, R.color.yellow)
    private val colorUnder50 = ContextCompat.getColor(context, R.color.green)
    private val colorUnder70 = ContextCompat.getColor(context, R.color.red)
    private val colorUnder80 = ContextCompat.getColor(context, R.color.blue)
    private val colorOver80 = ContextCompat.getColor(context, R.color.black)


    // Multiple ideal cruise speeds
    private val idealCruiseSpeeds = listOf(30, 50, 70, 80)  // Define multiple ideal cruise speeds here
    private val belowCruiseMargin = 3  // Margin below the ideal cruise speed
    private val aboveCruiseMargin = 2  // Margin above the ideal cruise speed
    private val speedUpMargin = 5       // Range below cruise for speeding up
    private val slowDownMargin = 5      // Range above cruise for slowing down

    fun getSpeedCategory(speed: Int): SpeedCategory {
        for (idealCruise in idealCruiseSpeeds) {
            when {
                speed in getSpeedingUpRange(idealCruise) -> return SpeedCategory.SPEEDING_UP
                speed in getCruisingRange(idealCruise) -> return SpeedCategory.CRUISING
                speed in getSlowingDownRange(idealCruise) -> return SpeedCategory.SLOWING_DOWN
            }
        }
        return SpeedCategory.UNKNOWN
    }

    // Calculate the cruising range with asymmetrical margins
    private fun getCruisingRange(idealCruise: Int): IntRange {
        return (idealCruise - belowCruiseMargin)..(idealCruise + aboveCruiseMargin)
    }

    // Calculate the speeding up range (below cruise)
    private fun getSpeedingUpRange(idealCruise: Int): IntRange {
        return (idealCruise - speedUpMargin - belowCruiseMargin) until (idealCruise - belowCruiseMargin)
    }

    // Calculate the slowing down range (above cruise)
    private fun getSlowingDownRange(idealCruise: Int): IntRange {
        return (idealCruise + aboveCruiseMargin + 1)..(idealCruise + aboveCruiseMargin + slowDownMargin)
    }




    // Function to check if the speed has changed significantly
    fun hasSpeedChanged(currentSpeed: Int): Boolean {
        val changed = previousSpeed == null || abs(currentSpeed - (previousSpeed ?: 0)) > 1
        if (changed) previousSpeed = currentSpeed
        return changed
    }

    // Function to check if the speed category has changed
    fun hasCategoryChanged(speed: Int): Boolean {
        val currentCategory = getSpeedCategory(speed)
        if (currentCategory != previousCategory) {
            previousCategory = currentCategory
            categoryChangedFlag = true
        }
        if (categoryChangedFlag) {
            categoryChangedFlag = false
            return true
        }
        return false
    }

    fun hasCategoryChangedFlag(): Boolean {
        return categoryChangedFlag
    }

    // Function to update the background color based on the speed
    fun updateBackgroundColor(speed: Int, updateView: (Int) -> Unit) {
        val color = when {
            speed < 30 -> colorUnder30
            speed < 50 -> colorUnder50
            speed < 70 -> colorUnder70
            speed < 80 -> colorUnder80
            else -> colorOver80
        }
        updateView(color)
    }
}

// Enum for Speed Categories
enum class SpeedCategory {
    SPEEDING_UP, CRUISING, SLOWING_DOWN, UNKNOWN
}
