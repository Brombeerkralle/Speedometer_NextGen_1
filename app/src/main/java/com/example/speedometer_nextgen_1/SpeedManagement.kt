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

    // Function to determine the speed category in a single place
    fun getSpeedCategory(speed: Int): SpeedCategory {
        return when {
            speed in 20..25 || speed in 40..45 || speed in 60..65 || speed in 74..76 -> SpeedCategory.SPEEDING_UP
            speed in 26..30 || speed in 46..50 || speed in 66..70 || speed in 77..80 -> SpeedCategory.CRUISING
            speed in 31..35 || speed in 50..54 || speed in 70..74 || speed in 80..84 -> SpeedCategory.SLOWING_DOWN
            else -> SpeedCategory.UNKNOWN
        }
    }

    // Function to check if the speed has changed significantly
    fun hasSpeedChanged(currentSpeed: Int): Boolean {
        val changed = previousSpeed == null || abs(currentSpeed - (previousSpeed ?: 0)) > 1
        if (changed) previousSpeed = currentSpeed
        return changed
    }

    // Function to check if the speed category has changed
    fun hasCategoryChanged(currentSpeed: Int): Boolean {
        val currentCategory = getSpeedCategory(currentSpeed)
        val changed = currentCategory != previousCategory
        if (changed) previousCategory = currentCategory
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
}

// Enum for Speed Categories
enum class SpeedCategory {
    SPEEDING_UP, CRUISING, SLOWING_DOWN, UNKNOWN
}
