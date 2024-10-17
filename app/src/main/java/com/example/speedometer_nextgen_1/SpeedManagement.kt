package com.example.speedometer_nextgen_1

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.abs

class SpeedManagement(private val context: Context, private val view: View) {

    var previousSpeed: Int? = null
    private var previousCategory: SpeedCategory? = null


    private val colorUnder30 = ContextCompat.getColor(context, R.color.yellow)
    private val colorUnder50 = ContextCompat.getColor(context, R.color.green)
    private val colorUnder70 = ContextCompat.getColor(context, R.color.red)
    private val colorUnder80 = ContextCompat.getColor(context, R.color.blue)
    private val colorOver80 = ContextCompat.getColor(context, R.color.black)

    // Function to determine the speed category
    fun getSpeedCategory(speed: Int): SpeedCategory {
        return when (speed) {
            in 20..25, in 40..45 -> SpeedCategory.SPEEDING_UP
            in 26..30, in 46..50 -> SpeedCategory.CRUISING
            in 31..35, in 50..54 -> SpeedCategory.SLOWING_DOWN
            else -> SpeedCategory.UNKNOWN
        }
    }

    // Function to check if the speed has changed significantly
    fun hasSpeedChanged(currentSpeed: Int): Boolean {
        val changed = previousSpeed == null || kotlin.math.abs(currentSpeed - (previousSpeed ?: 0)) > 1
        if (changed) previousSpeed = currentSpeed
        return changed
    }

    // Function to check if the speed category has changed
    fun hasCategoryChanged(currentCategory: SpeedCategory): Boolean {
        val changed = currentCategory != previousCategory
        if (changed) previousCategory = currentCategory
        return changed
    }

    // Check if speed has changed significantly (more than 1 unit)
    fun didSpeedChange(speed: Int): Boolean {
        val previous = previousSpeed ?: 0 // Use 0 if previousSpeed is null
        return abs(speed - previous) > 1
    }

    // Check if the speed category has changed
    fun didCategoryChange(speed: Int): Boolean {
        val currentCategory = when {
            belowCruse(speed) -> SpeedCategory.SPEEDING_UP
            onCruse(speed) -> SpeedCategory.CRUISING
            aboveCruse(speed) -> SpeedCategory.SLOWING_DOWN
            else -> SpeedCategory.UNKNOWN
        }

        val changed = currentCategory != previousCategory
        if (changed) {
            previousCategory = currentCategory
        }
        return changed
    }

    // Function to update the background color based on the speed
    fun updateBackgroundColor(speed: Int) {
        val color = when {
            speed < 30 -> colorUnder30
            speed < 50 -> colorUnder50
            speed < 70 -> colorUnder70
            speed < 80 -> colorUnder80
            else -> colorOver80
        }
        view.setBackgroundColor(color)
    }

    // Speed range checking functions
    private fun belowCruse(speed: Int): Boolean {
        return speed in 20..25 || speed in 40..45 || speed in 60..65 || speed in 70..75
    }

    private fun onCruse(speed: Int): Boolean {
        return speed in 26..30 || speed in 46..50 || speed in 66..70 || speed in 76..80
    }

    private fun aboveCruse(speed: Int): Boolean {
        return speed in 31..35 || speed in 50..54 || speed in 70..74 || speed in 80..84
    }
}

// Enum for Speed Categories
enum class SpeedCategory {
    SPEEDING_UP, CRUISING, SLOWING_DOWN, UNKNOWN
}
