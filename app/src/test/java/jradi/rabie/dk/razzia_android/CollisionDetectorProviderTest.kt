package jradi.rabie.dk.razzia_android

import android.location.Location
import jradi.rabie.dk.razzia_android.model.*
import jradi.rabie.dk.razzia_android.model.entities.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.CoroutineContext

/**
 * @author rabie
 *
 * Note that we only cover one case with this class
 */
class CollisionDetectorProviderTest {

    //TODO cover more cases with unit tests for this class

    /**
     * Simple test just to see if the class stops its execution if we call the abort method after starting to watch
     */
    @Test
    fun testCallingAbortAfterStartingToWatch() = runBlocking {

        val userLocationProvider = object : UserLocationProviderInterface {
            override suspend fun getLastKnownLocation(): Location? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override suspend fun observeLocationUpdates(locationConfigurationRequestProvider: LocationConfigurationRequestProviderInterface): Channel<Location> {
                val channel = Channel<Location>()
                return channel
            }
        }

        val locationObserverHandler = object : LocationObserverHandlerInterface {
            override suspend fun checkForCollisions(locationUpdate: Location): CollisionCheckResult {
                return CollisionCheckResult.NoCollisions
            }

        }
        val collisionDetectorProvider = CollisionDetectorProvider(locationProvider = userLocationProvider,
                locationObserverHandler = locationObserverHandler)

        collisionDetectorProvider.watch()
        collisionDetectorProvider.abort()
        assertTrue(true)
    }

    /**
     * Test if we receive an alert for a given test location that is colliding with a given entry point.
     */
    @Test
    fun testOneSuccessfulReceivedCollisionAlertForCorrectCollision() = runBlocking {
        val latitudeTestInput = 1.0
        val longitudeTestInput = 2.0

        val userLocation = Location("Test location").apply {
            latitude = latitudeTestInput
            longitude = longitudeTestInput
        }


        //Just send location update on the provder channel
        val userLocationProvider = object : UserLocationProviderInterface, CoroutineScope {
            override val coroutineContext: CoroutineContext = Job() + Dispatchers.Default
            override suspend fun getLastKnownLocation(): Location? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override suspend fun observeLocationUpdates(locationConfigurationRequestProvider: LocationConfigurationRequestProviderInterface): Channel<Location> {
                //FIXME why do we need a capacity? the whole point about coroutines is that it shouldn't be blocking.
                val channel = Channel<Location>(2)//A buffered channel will not suspend and therefore not block execution of the remainder of the test
                channel.send(userLocation)
                channel.close()
                return channel
            }
        }

        //Location of the razzia entry is the same as the user location above
        val gpsLocationTestInput = GPSLocation(latitude = latitudeTestInput, longitude = longitudeTestInput)
        //Just return one entry
        val entriesDataProvider = object : EntriesDataProviderInterface {
            override suspend fun getEntries(): List<RazziaEntry> {
                println("[CollisionDetectorProviderTest] getting entries")
                return mutableListOf<RazziaEntry>().apply { add(RazziaEntry(id = Id("Test Id"), creationDate = Timestamp(System.currentTimeMillis()), description = "Ost på hjørnet", location = gpsLocationTestInput, timeToLive = Hour(2), radiusInMeters = 10)) }
            }

            override suspend fun addEntry(razziaEntry: RazziaEntry) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        //Always allow whatever alert there might be
        val alertFilterProvider = object : AlertFilterProviderInterface {
            override suspend fun isAlertAllowed(): Boolean {
                return true
            }
        }

        //All given fences are returned
        val fenceCollisionDetector = object : FenceCollisionDetectorInterface {
            override fun findCollisions(fences: List<CircularFence>, userLocation: Location): List<CircularFence> {
                println("[CollisionDetectorProviderTest] finding collisions")
                return fences
            }
        }

        var didAlert = false
        val alertProvider = object : AlertNotificationPresenterInterface {
            override suspend fun alertUser() {
                println("[CollisionDetectorProviderTest] alerting user")
                didAlert = true
            }
        }

        val locationObserverHandler = LocationObserverHandler(entriesDataProvider = entriesDataProvider,
                alertFilterProvider = alertFilterProvider,
                fenceCollisionDetector = fenceCollisionDetector,
                alertNotificationPresenter = alertProvider)
        val collisionDetectorProvider = CollisionDetectorProvider(locationProvider = userLocationProvider,
                locationObserverHandler = locationObserverHandler)

        try {
            withTimeout(1000) {
                collisionDetectorProvider.watch()
            }
        } catch (e: CancellationException) {
            collisionDetectorProvider.abort()
            assertTrue(false)
        }
        assertEquals(true, didAlert)
    }
}