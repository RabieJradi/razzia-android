package jradi.rabie.dk.razzia_android.mocked

import android.location.Location
import jradi.rabie.dk.razzia_android.model.LocationConfigurationRequestProviderInterface
import jradi.rabie.dk.razzia_android.model.UserLocationProviderInterface
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext

/**
 * @author rabie
 *
 *
 */

/**
 * This mocked user location provider will just replay the same location updates in a loop so we can easily test the functionality of the app.
 */
class MockedUserLocationProvider : UserLocationProviderInterface, CoroutineScope {

    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Default

    var shouldReplayForever = false

    private val locations: List<Double> = listOf(
            55.698570, 12.554932, //<- north point
            55.698328, 12.554599,
            55.698122, 12.554395,
            55.697717, 12.553880,
            55.697245, 12.553322,
            55.696840, 12.553226,
            55.696272, 12.553204,
            55.695770, 12.553311,
            55.695123, 12.553611,
            55.694591, 12.554030,
            55.694180, 12.554395,
            55.693890, 12.554749,
            55.693560, 12.555184,
            55.693560, 12.555184,
            55.692640, 12.556332)//<- south-eastern point

    override suspend fun getLastKnownLocation(): Location? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Send updates on a route down through Guldbergsgade and then restart once we end the trip.
     */
    override suspend fun observeLocationUpdates(locationConfigurationRequestProvider: LocationConfigurationRequestProviderInterface): Channel<Location> {
        val channel = Channel<Location>()
        launch {
            do {
                locations.zipWithNext().forEachIndexed { index, pair ->
                    delay(1000)
                    channel.send(Location("Location index $index").apply {
                        latitude = pair.first
                        longitude = pair.second
                    })
                }
            } while (shouldReplayForever)
        }
        return channel
    }

}