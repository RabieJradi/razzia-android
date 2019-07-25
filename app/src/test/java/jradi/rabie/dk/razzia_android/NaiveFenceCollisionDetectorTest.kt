package jradi.rabie.dk.razzia_android

import android.location.Location
import android.support.test.runner.AndroidJUnit4
import jradi.rabie.dk.razzia_android.model.NaiveFenceCollisionDetector
import jradi.rabie.dk.razzia_android.model.entities.CircularFence
import jradi.rabie.dk.razzia_android.model.entities.GPSLocation
import jradi.rabie.dk.razzia_android.model.entities.Id
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
class NaiveFenceCollisionDetectorTest {

    private lateinit var naiveFenceCollisionDetector: NaiveFenceCollisionDetector
    private lateinit var userLocation: Location

    @Before
    fun setup() {
        naiveFenceCollisionDetector = NaiveFenceCollisionDetector()
        userLocation = Location("Tagensvej/Jagtvej").apply {
            latitude = 55.699052
            longitude = 12.554282
        }
    }

    @Test
    fun noCollisionsTest() {
        val collisions = naiveFenceCollisionDetector.findCollisions(emptyList(), userLocation)
        assertTrue(collisions.isEmpty())
    }

    @Test
    fun noCollisionsTestWithOneFence() {
        val fenceList = ArrayList<CircularFence>().apply {
            add(CircularFence(id = Id("Nørrebro runddel"), radiusInMeters = 10, center = GPSLocation(latitude = 55.69429044443225, longitude = 12.548927444941683)))
        }
        val collisions = naiveFenceCollisionDetector.findCollisions(fenceList, userLocation)
        assertTrue(collisions.isEmpty())
    }

    @Test
    fun collisionTestWithOneFenceLargeRadius() {
        val fenceList = ArrayList<CircularFence>().apply {
            add(CircularFence(id = Id("Nørrebro runddel"), radiusInMeters = 630, center = GPSLocation(latitude = 55.69429044443225, longitude = 12.548927444941683)))
        }
        val collisions = naiveFenceCollisionDetector.findCollisions(fenceList, userLocation)
        assertTrue(collisions.size == 1)
    }

    @Test
    fun collisionTestWithSameGPSLocationOnFenceAndUserWithNoRadius() {
        val fenceList = ArrayList<CircularFence>().apply {
            add(CircularFence(id = Id("Same as user location"), radiusInMeters = 0, center = GPSLocation(latitude = userLocation.latitude, longitude = userLocation.longitude)))
        }
        val collisions = naiveFenceCollisionDetector.findCollisions(fenceList, userLocation)
        assertTrue(collisions.size == 1)
    }

    @Test
    fun collisionTestWithSameGPSLocationOnFenceAndUserWithRadius() {
        val fenceList = ArrayList<CircularFence>().apply {
            add(CircularFence(id = Id("Same as user location"), radiusInMeters = 1, center = GPSLocation(latitude = userLocation.latitude, longitude = userLocation.longitude)))
        }
        val collisions = naiveFenceCollisionDetector.findCollisions(fenceList, userLocation)
        assertTrue(collisions.size == 1)
    }
}