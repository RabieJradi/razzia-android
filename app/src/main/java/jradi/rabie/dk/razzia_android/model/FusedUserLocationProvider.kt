package jradi.rabie.dk.razzia_android.model

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
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
    suspend fun getLastKnownLocation(): Location?
    suspend fun observeLocationUpdates(locationConfigurationRequestProvider: LocationConfigurationRequestProviderInterface = FastLocationUpdates()): Channel<Location>
}

/**
 * Is responsible for providing user location updates by using Google's fused location API.
 */
class FusedUserLocationProvider(context: Context) : UserLocationProviderInterface, CoroutineScope {

    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Default
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Gives you a single location (if any is known)
     */
    override suspend fun getLastKnownLocation(): Location? {
        return suspendCoroutine { cont ->
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    cont.resume(it)
                }.addOnFailureListener {
                    cont.resume(null)
                }
            } catch (e: SecurityException) {
                cont.resume(null)
            }
        }
    }

    /**
     * This method returns a channel immediately and sends location updates through the channel from the coroutine scope defined by this class.
     *
     * Note by making this method an extension function of CoroutineScope we make sure that if this method should fail, the parent coroutine will have to handle that or fail itself.
     */
    override suspend fun observeLocationUpdates(locationConfigurationRequestProvider: LocationConfigurationRequestProviderInterface): Channel<Location> {
        //NB: We could also have simplified this method using the produce coroutine builder
        val channel = Channel<Location>()
        launch {
            val currentKnownLocation: Location? = getLastKnownLocation()
            if (currentKnownLocation != null) {
                channel.send(currentKnownLocation)
            }

            val callback = UserLocationCallback {
                channel.send(it)
            }

            try {
                callback.executeHandlerForEachLocationResult()
                fusedLocationClient.requestLocationUpdates(locationConfigurationRequestProvider.locationRequest(), callback, Looper.getMainLooper()).addOnFailureListener {
                    cleanup(callback, channel)
                }
            } catch (e: SecurityException) {
                cleanup(callback, channel)
            } catch (e: CancellationException) {
                cleanup(callback, channel)
            }
        }
        return channel
    }

    private fun cleanup(callback: UserLocationCallback, channel: Channel<Location>) {
        channel.cancel()
        this.fusedLocationClient.removeLocationUpdates(callback)
    }
}

/**
 * This class is capable of turning asynchronous user location notifications into a suspending function that can execute a handler for each such value.
 */
class UserLocationCallback(private val channelHandler: suspend (Location) -> Unit) : LocationCallback() {

    private var continuation: Continuation<Location>? = null

    suspend fun executeHandlerForEachLocationResult() {
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