package jradi.rabie.dk.razzia_android.model

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
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
    suspend fun observeLocationUpdates(coroutineScope: CoroutineScope, locationConfigurationRequestProvider: LocationConfigurationRequestProviderInterface = FastLocationUpdates()): Channel<Location>
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

    //TODO we need to remove the observer if the caller that is listening is stopped. allow for a remove observer method
    override suspend fun observeLocationUpdates(coroutineScope: CoroutineScope, locationConfigurationRequestProvider: LocationConfigurationRequestProviderInterface): Channel<Location> {
        val channel = Channel<Location>()

        val currentKnownLocation: Location? = getCurrentLocation()
        if (currentKnownLocation != null) {
            channel.send(currentKnownLocation)
        }

        val observerKey = UUID.randomUUID().toString()
        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                val lastLocation = locationResult?.lastLocation ?: return
                //TODO consider checking the accuracy lastLocation.accuracy and other requirements
                coroutineScope.launch {
                    try {
                        channel.send(lastLocation)
                    } catch (e: CancellationException) {
                        fusedLocationClient.removeLocationUpdates(onGoingLocationCallbacks[observerKey])
                    }
                }
            }
        }
        onGoingLocationCallbacks.put(observerKey, callback)

        try {
            fusedLocationClient.requestLocationUpdates(locationConfigurationRequestProvider.locationRequest(), callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            channel.cancel()
            onGoingLocationCallbacks.remove(observerKey)
        }

        return channel
    }
}
