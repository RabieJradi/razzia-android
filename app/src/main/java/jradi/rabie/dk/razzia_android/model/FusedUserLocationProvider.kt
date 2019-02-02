package jradi.rabie.dk.razzia_android.model

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @author rabie
 *
 *
 */

interface LocationConfigurationRequestProviderInterface {
    fun locationRequest(): LocationRequest
}

/**
 * Use this class if you want fast location updates.
 */
class FastLocationUpdates : LocationConfigurationRequestProviderInterface {
    override fun locationRequest() = LocationRequest().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = 500
        fastestInterval = 1500
    }
}

interface UserLocationProviderInterface {
    suspend fun getCurrentLocation(): Location?
    suspend fun observeLocationUpdates(locationConfigurationRequestProvider: LocationConfigurationRequestProviderInterface = FastLocationUpdates()): Channel<Location>
}

class UserLocationProvider(context: Context) : UserLocationProviderInterface {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var onGoingLocationCallbacks = ConcurrentHashMap<String, LocationCallback>()

    override suspend fun getCurrentLocation(): Location? {
        return suspendCoroutine { cont ->
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    cont.resume(it)
                }
            } catch (e: SecurityException) {
                cont.resume(null)
            }
        }
    }

    override suspend fun observeLocationUpdates(locationConfigurationRequestProvider: LocationConfigurationRequestProviderInterface): Channel<Location> {
        val channel = Channel<Location>()

        val currentKnownLocation: Location? = getCurrentLocation()
        if (currentKnownLocation != null) {
            channel.send(currentKnownLocation)
        }

        val observerKey = UUID.randomUUID().toString()
        val callback = UserLocationCallback()
        onGoingLocationCallbacks.put(observerKey, callback)

        try {
            callback.handlerForEachLocation {
                channel.send(it)
            }
            fusedLocationClient.requestLocationUpdates(locationConfigurationRequestProvider.locationRequest(), callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            cancelChannelAndStopObserving(observerKey,channel)
        } catch (e: CancellationException) {
            cancelChannelAndStopObserving(observerKey,channel)
        }

        return channel
    }

    private fun cancelChannelAndStopObserving(observerKey: String, channel:Channel<Location>){
        channel.cancel()
        fusedLocationClient.removeLocationUpdates(onGoingLocationCallbacks[observerKey])
        onGoingLocationCallbacks.remove(observerKey)
    }
}

/**
 * This class is capable of turning asynchronous user location notifications into a suspending function that can execute a handler for each such value.
 */
class UserLocationCallback : LocationCallback() {

    private var continuation: Continuation<Location>? = null

    suspend fun handlerForEachLocation(channelHandler: suspend (Location) -> Unit) {
        try {

            while (true) {
                //On each iteration it will suspend here until a location value is received.
                val lastLocation = suspendCoroutine<Location> { cont ->
                    continuation = cont
                }
                //Should the suspending handler function get cancelled (throw a CancellationException), the loop will break and no further locations will get consumed.
                channelHandler(lastLocation)
            }
        } finally {
            //When the loop is broken, we want to ensure no further processing occurs
            continuation = null
        }
    }

    override fun onLocationResult(locationResult: LocationResult?) {
        super.onLocationResult(locationResult)
        val lastLocation = locationResult?.lastLocation ?: return
        continuation?.resume(lastLocation) //This resume call will execute the handler right away

    }
}
