package com.example.speedometer_nextgen_1

import android.content.BroadcastReceiver
import android.content.Context
import org.junit.Test

import org.junit.Assert.*

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}


@RunWith(AndroidJUnit4::class)
class MainActivityBroadcastTest {

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var latch: CountDownLatch
    private var receivedSpeed: Int? = null
    private var receivedSpeedDecimal: String? = null

    @Before
    fun setup() {
        // Launch MainActivity
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Set up a countdown latch to wait for the broadcast response
        latch = CountDownLatch(1)

        // Register a temporary broadcast receiver to simulate receiving speed data
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                receivedSpeed = intent?.getIntExtra("speed", -1)
                receivedSpeedDecimal = intent?.getStringExtra("speedDecimal")
                latch.countDown()  // Signal that we received the broadcast
            }
        }


        // Register the broadcast receiver with the same filter used in MainActivity
        val filter = IntentFilter("com.example.speedometer_nextgen_1.LOCATION_UPDATE")
        InstrumentationRegistry.getInstrumentation().targetContext.registerReceiver(receiver, filter,
            Context.RECEIVER_NOT_EXPORTED)
    }

    @Test
    fun testBroadcastReceiverReceivesSpeedData() {
        // Prepare a test broadcast intent
        val intent = Intent("com.example.speedometer_nextgen_1.LOCATION_UPDATE").apply {
            putExtra("speed", 5)
            putExtra("speedDecimal", "7")
        }

        // Send the broadcast
        InstrumentationRegistry.getInstrumentation().targetContext.sendBroadcast(intent)

        // Wait for the broadcast to be received
        latch.await(3, TimeUnit.SECONDS)

        // Verify that we received the correct speed data
        assertEquals(5, receivedSpeed)
        assertEquals("7", receivedSpeedDecimal)
    }

    @After
    fun cleanup() {
        // Cleanup: unregister the broadcast receiver and close the activity
        scenario.close()
    }
}