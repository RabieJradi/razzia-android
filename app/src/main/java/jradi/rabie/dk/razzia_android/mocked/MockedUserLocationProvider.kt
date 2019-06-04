package jradi.rabie.dk.razzia_android.mocked

import android.location.Location
import jradi.rabie.dk.razzia_android.model.LocationConfigurationRequestProviderInterface
import jradi.rabie.dk.razzia_android.model.UserLocationProviderInterface
import jradi.rabie.dk.razzia_android.view.logPrint
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

    private val locations: List<Pair<Double, Double>> = listOf(
            Pair(55.698570, 12.554932), //<- north point
            Pair(55.698328, 12.554599),
            Pair(55.698122, 12.554395),
            Pair(55.697717, 12.553880),
            Pair(55.697245, 12.553322),
            Pair(55.696840, 12.553226),
            Pair(55.696272, 12.553204),
            Pair(55.695770, 12.553311),
            Pair(55.695123, 12.553611),
            Pair(55.694591, 12.554030),
            Pair(55.694180, 12.554395),
            Pair(55.693890, 12.554749),
            Pair(55.693560, 12.555184),
            Pair(55.693560, 12.555184),
            Pair(55.692640, 12.556332))//<- south-eastern point

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
                locations.forEachIndexed { index, pair ->
                    delay(1000)
                    channel.send(Location("Location index $index").apply {
                        latitude = pair.first
                        longitude = pair.second
                    })
                    logPrint("[MockedUserLocationProvider] Location index $index, (latitude,longitude)=(${pair.first},${pair.second})")
                }
            } while (shouldReplayForever)
            //Otherwise close the channel
            channel.close()
        }
        return channel
    }

}