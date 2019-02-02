package jradi.rabie.dk.razzia_android

import android.location.Location
import android.support.test.runner.AndroidJUnit4
import jradi.rabie.dk.razzia_android.model.AlertProvider
import jradi.rabie.dk.razzia_android.model.MockedSoundProvider
import jradi.rabie.dk.razzia_android.model.NaiveFenceCollisionDetector
import jradi.rabie.dk.razzia_android.model.entities.CircularFence
import jradi.rabie.dk.razzia_android.model.entities.GPSLocation
import jradi.rabie.dk.razzia_android.model.entities.Id
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
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
class AlertProviderTest {

    private lateinit var alertProvider: AlertProvider
    private val alertThresholdInSeconds = 1
    @Before
    fun setup() {
        alertProvider = AlertProvider(MockedSoundProvider(), alertThresholdInSeconds)
    }

    @Test
    fun oneCallWithAlert() =  runBlocking{
        val shouldAlertUser = alertProvider.alert()
        assertTrue(shouldAlertUser)
    }

    @Test
    fun twoQuickCallsWithNoSecondAlert() =  runBlocking{
        var shouldAlertUser = alertProvider.alert()
        assertTrue(shouldAlertUser)
        shouldAlertUser = alertProvider.alert()
        //Not enough time has passed
        assertTrue(!shouldAlertUser)
    }

    @Test
    fun fiveQuickCallsWithNoSecondAlerts() =  runBlocking{
        var shouldAlertUser = alertProvider.alert()
        assertTrue(shouldAlertUser)

        repeat(5) {
            shouldAlertUser = alertProvider.alert()
            //Not enough time has passed
            assertTrue(!shouldAlertUser)
        }
    }

    @Test
    fun oneCallThenDelayAbitAndThenCallAgain() = runBlocking {
        var shouldAlertUser = alertProvider.alert()
        assertTrue(shouldAlertUser)

        delay(alertThresholdInSeconds * 500L)
        shouldAlertUser = alertProvider.alert()
        //Not enough time has passed
        assertTrue(!shouldAlertUser)
    }

    @Test
    fun oneCallThenDelayUntilThresholdAndThenCallAgain() = runBlocking {
        var shouldAlertUser = alertProvider.alert()
        assertTrue(shouldAlertUser)

        delay(alertThresholdInSeconds * 1000L)
        shouldAlertUser = alertProvider.alert()
        //enough time has passed
        assertTrue(shouldAlertUser)
    }
}