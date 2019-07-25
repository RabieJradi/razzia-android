package jradi.rabie.dk.razzia_android

import android.support.test.runner.AndroidJUnit4
import jradi.rabie.dk.razzia_android.model.AlertFilterProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author rabie
 *
 *
 */
@RunWith(AndroidJUnit4::class)
class AlertFilterProviderTest {

    private lateinit var alertFilterProvider: AlertFilterProvider
    private val alertThresholdInSeconds = 1
    @Before
    fun setup() {
        alertFilterProvider = AlertFilterProvider(alertThresholdInSeconds)
    }

    @Test
    fun oneCallWithAlert() =  runBlocking{
        val shouldAlertUser = alertFilterProvider.isAlertAllowed()
        assertTrue(shouldAlertUser)
    }

    @Test
    fun twoQuickCallsWithNoSecondAlert() =  runBlocking{
        var shouldAlertUser = alertFilterProvider.isAlertAllowed()
        assertTrue(shouldAlertUser)
        shouldAlertUser = alertFilterProvider.isAlertAllowed()
        //Not enough time has passed
        assertTrue(!shouldAlertUser)
    }

    @Test
    fun fiveQuickCallsWithNoSecondAlerts() =  runBlocking{
        var shouldAlertUser = alertFilterProvider.isAlertAllowed()
        assertTrue(shouldAlertUser)

        repeat(5) {
            shouldAlertUser = alertFilterProvider.isAlertAllowed()
            //Not enough time has passed
            assertTrue(!shouldAlertUser)
        }
    }

    @Test
    fun oneCallThenDelayAbitAndThenCallAgain() = runBlocking {
        var shouldAlertUser = alertFilterProvider.isAlertAllowed()
        assertTrue(shouldAlertUser)

        delay(alertThresholdInSeconds * 500L)
        shouldAlertUser = alertFilterProvider.isAlertAllowed()
        //Not enough time has passed
        assertTrue(!shouldAlertUser)
    }

    @Test
    fun oneCallThenDelayUntilThresholdAndThenCallAgain() = runBlocking {
        var shouldAlertUser = alertFilterProvider.isAlertAllowed()
        assertTrue(shouldAlertUser)

        delay(alertThresholdInSeconds * 1000L)
        shouldAlertUser = alertFilterProvider.isAlertAllowed()
        //enough time has passed
        assertTrue(shouldAlertUser)
    }
}