package com.example.speedometer_nextgen_1

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.abs

data class SpeedStatus(val category: SpeedCategory, val targetSpeed: Int? = null)

/**
 * SpeedManagement: Handles speed classification, threshold ranges (speeding up, cruising, slowing down),
 * speed change detection, and background color transitions.
 */
class SpeedManagement(context: Context) {

    // ================= ================= =================
    // Properties & State
    // ================= ================= =================

    var previousSpeed: Int? = null
    private var previousStatus: SpeedStatus? = null
    private var categoryChangedFlag = false

    private val colorUnder30 = ContextCompat.getColor(context, R.color.yellow)
    private val colorUnder50 = ContextCompat.getColor(context, R.color.green)
    private val colorUnder70 = ContextCompat.getColor(context, R.color.red)
    private val colorUnder80 = ContextCompat.getColor(context, R.color.blue)
    private val colorOver80 = ContextCompat.getColor(context, R.color.black)

    // Multiple ideal cruise speeds and margins
    val idealCruiseSpeeds = listOf(30, 50, 70, 80)
    private val belowCruiseMargin = 3
    private val aboveCruiseMargin = 2
    private val speedUpMargin = 5
    private val slowDownMargin = 5

    // ================= ================= =================
    // Speed Classification & Status Logic
    // ================= ================= =================

    fun getSpeedStatus(speed: Int): SpeedStatus {
        for (idealCruise in idealCruiseSpeeds) {
            when {
                speed in getSpeedingUpRange(idealCruise) -> return SpeedStatus(SpeedCategory.SPEEDING_UP, idealCruise)
                speed in getCruisingRange(idealCruise) -> return SpeedStatus(SpeedCategory.CRUISING, idealCruise)
                speed in getSlowingDownRange(idealCruise) -> return SpeedStatus(SpeedCategory.SLOWING_DOWN, idealCruise)
            }
        }
        return SpeedStatus(SpeedCategory.UNKNOWN)
    }

    fun hasSpeedChanged(currentSpeed: Int): Boolean {
        val changed = previousSpeed == null || abs(currentSpeed - (previousSpeed ?: 0)) > 1
        if (changed) previousSpeed = currentSpeed
        return changed
    }

    fun hasCategoryChanged(speed: Int): Boolean {
        val currentStatus = getSpeedStatus(speed)
        if (currentStatus != previousStatus) {
            previousStatus = currentStatus
            categoryChangedFlag = true
            return true
        } else if (categoryChangedFlag) {
            categoryChangedFlag = false
        }
        return false
    }

    fun hasCategoryChangedFlag(): Boolean {
        return categoryChangedFlag
    }

    fun reset() {
        previousSpeed = null
        previousStatus = null
        categoryChangedFlag = false
    }

    // ================= ================= =================
    // Range Calculations
    // ================= ================= =================

    private fun getCruisingRange(idealCruise: Int): IntRange {
        return (idealCruise - belowCruiseMargin)..(idealCruise + aboveCruiseMargin)
    }

    private fun getSpeedingUpRange(idealCruise: Int): IntRange {
        return (idealCruise - speedUpMargin - belowCruiseMargin) until (idealCruise - belowCruiseMargin)
    }

    private fun getSlowingDownRange(idealCruise: Int): IntRange {
        return (idealCruise + aboveCruiseMargin + 1)..(idealCruise + aboveCruiseMargin + slowDownMargin)
    }

    // ================= ================= =================
    // UI & Color Updates
    // ================= ================= =================

    fun updateBackgroundColor(speed: Int, updateView: (Int) -> Unit) {
        val color = when {
            speed < 30 -> colorUnder30
            speed < 50 -> colorUnder50
            speed < 70 -> colorUnder70
            speed < 80 -> colorUnder80
            else -> colorOver80
        }
        Log.v("SpeedManagement", "updateBackgroundColor with speed $speed, color $color")
        updateView(color)
    }

    fun getSpeedCategory(speed: Int): SpeedCategory = getSpeedStatus(speed).category
}

enum class SpeedCategory {
    SPEEDING_UP, CRUISING, SLOWING_DOWN, UNKNOWN
}
