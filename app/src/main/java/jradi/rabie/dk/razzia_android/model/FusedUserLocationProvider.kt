package jradi.rabie.dk.razzia_android.model

import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import jradi.rabie.dk.razzia_android.view.logPrint
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

//TODO unit test fused user location provider class

//TODO fix bug with google maps and location where we don't get any user location if "high accuracy" is turned off (but location is turned on). We need to get the permission dialog.

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
                logPrint("[FusedUserLocationProvider] sending last known location")
                channel.send(currentKnownLocation)
            }

            val callback = UserLocationCallback {
                logPrint("[FusedUserLocationProvider] sending new location event")
                channel.send(it)
            }

            try {
                logPrint("[FusedUserLocationProvider] setup 1")
                fusedLocationClient.requestLocationUpdates(locationConfigurationRequestProvider.locationRequest(), callback, Looper.getMainLooper()).addOnFailureListener {
                    logPrint("[FusedUserLocationProvider] on failure listener triggered")
                    cleanup(callback, channel)
                }
                callback.executeHandlerForEachLocationResult() //TODO OBS I moved this line of code down here from instead of before starting the reuqests
                logPrint("[FusedUserLocationProvider] setup 2")
            } catch (e: SecurityException) {
                logPrint("[FusedUserLocationProvider] SecurityException e:\n" + Log.getStackTraceString(e))
                cleanup(callback, channel)
            } catch (e: CancellationException) {
                logPrint("[FusedUserLocationProvider] CancellationException e:\n" + Log.getStackTraceString(e))
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
        logPrint("[FusedUserLocationProvider] handling result on thread ${Thread.currentThread().name}")
        val lastLocation = locationResult?.lastLocation ?: return
        /**
         * Ideas for solutions.
         *
         * 1. ignore the event update if the continuation is not ready yet (check if it is active). be aware that this isn't in fact hiding an underlying problem where new updates never will be processed again, which might be likely, why otherwise does it only cash on a collision?
         *
         * 2.
         */
        continuation?.resume(lastLocation) //This resume call will execute the handler right away //FIXME crash on this line of code with exception seen below

    }
}

/** FIXME
2019-03-23 15:58:04.644 6263-6343/jradi.rabie.dk.razzia_android D/Osteklokken: [FusedUserLocationProvider] sending new location event
2019-03-23 15:58:04.645 6263-6335/jradi.rabie.dk.razzia_android D/Osteklokken: [CollisionDetectorProvider] checking for collisions
2019-03-23 15:58:06.183 6263-6343/jradi.rabie.dk.razzia_android D/Osteklokken: [FusedUserLocationProvider] sending new location event
2019-03-23 15:58:06.184 6263-6335/jradi.rabie.dk.razzia_android D/Osteklokken: [CollisionDetectorProvider] checking for collisions
2019-03-23 15:58:07.979 6263-6342/jradi.rabie.dk.razzia_android D/Osteklokken: [FusedUserLocationProvider] sending new location event
2019-03-23 15:58:07.980 6263-6335/jradi.rabie.dk.razzia_android D/Osteklokken: [CollisionDetectorProvider] checking for collisions
2019-03-23 15:58:09.664 6263-6332/jradi.rabie.dk.razzia_android D/Osteklokken: [FusedUserLocationProvider] sending new location event
2019-03-23 15:58:11.189 6263-6263/jradi.rabie.dk.razzia_android D/AndroidRuntime: Shutting down VM
2019-03-23 15:58:11.192 6263-6263/jradi.rabie.dk.razzia_android E/AndroidRuntime: FATAL EXCEPTION: main
Process: jradi.rabie.dk.razzia_android, PID: 6263
java.lang.IllegalStateException: Already resumed
at kotlin.coroutines.SafeContinuation.resumeWith(SafeContinuationJvm.kt:46)
at jradi.rabie.dk.razzia_android.model.UserLocationCallback.onLocationResult(FusedUserLocationProvider.kt:143)
at com.google.android.gms.internal.location.zzau.notifyListener(Unknown Source:4)
at com.google.android.gms.common.api.internal.ListenerHolder.notifyListenerInternal(Unknown Source:17)
at com.google.android.gms.common.api.internal.ListenerHolder$zaa.handleMessage(Unknown Source:5)
at android.os.Handler.dispatchMessage(Handler.java:106)
at android.os.Looper.loop(Looper.java:193)
at android.app.ActivityThread.main(ActivityThread.java:6718)
at java.lang.reflect.Method.invoke(Native Method)
at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:493)
at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:858)
2019-03-23 15:58:11.208 6263-6263/jradi.rabie.dk.razzia_android I/Process: Sending signal. PID: 6263 SIG: 9

        */