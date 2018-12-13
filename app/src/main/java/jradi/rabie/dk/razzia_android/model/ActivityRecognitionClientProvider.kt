package jradi.rabie.dk.razzia_android.model

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import jradi.rabie.dk.razzia_android.view.BikeActivityRecognitionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * Helper class to setup the recognition client with dependencies so that it doesn't rely on an Activity reference
 */
class BikeActivityRecognitionClientProviderCreator(private val activity: Activity) {
    fun create(): ActivityRecognitionClientProvider {
        val client = ActivityRecognitionClient(activity)

        val serviceHandlingActivityTransitionsCallbacks = BikeActivityRecognitionService::class.java

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling requestActivityUpdates() and removeActivityUpdates().
        val pendingIntent = PendingIntent.getService(activity, 0, Intent(activity, serviceHandlingActivityTransitionsCallbacks), PendingIntent.FLAG_UPDATE_CURRENT);

        val bikeActivityTransitions: ActivityTransitionRequest = ActivityTransitionRequest(ArrayList<ActivityTransition>().apply {
            add(ActivityTransition.Builder().setActivityType(DetectedActivity.ON_BICYCLE).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build())
            add(ActivityTransition.Builder().setActivityType(DetectedActivity.ON_BICYCLE).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build())
        })

        val cacheProvider = ConfigurationProvider.config.createActivityRecognitionCacheProvider(activity)

        return ActivityRecognitionClientProvider(client = client, pendingIntent = pendingIntent, activityTransitions = bikeActivityTransitions,cacheProvider = cacheProvider)
    }
}

/**
 * Is responsible for being able to setup the recognition client and expose convenient methods for registering and unregistering activity callbacks.
 */
class ActivityRecognitionClientProvider(private val client: ActivityRecognitionClient,
                                        private val pendingIntent: PendingIntent,
                                        private val activityTransitions: ActivityTransitionRequest,
                                        private val cacheProvider: CacheProviderInterface) {

    suspend fun requestActivityUpdates() {
        withContext(Dispatchers.Main) {
            val hasRequestedUpdates = cacheProvider.getBooleanValue()
            if (hasRequestedUpdates) {
                return@withContext
            }

            val didSucceed = start()
            cacheProvider.saveValue(didSucceed)
        }
    }

    private suspend fun start() = suspendCoroutine<Boolean> { cont ->
        client.requestActivityTransitionUpdates(activityTransitions, pendingIntent)
                .addOnSuccessListener {
                    cont.resume(true)
                }.addOnFailureListener {
                    cont.resume(false)
                }
    }

    //TODO call this method if the user should disable location permission or recognition permissions?
    suspend fun stopActivityUpdates() {
        withContext(Dispatchers.Main) {
            val result = stop()
            cacheProvider.saveValue(result)
        }
    }

    private suspend fun stop() = suspendCoroutine<Boolean> {continuation ->
        client.removeActivityTransitionUpdates(pendingIntent).addOnSuccessListener {
            continuation.resume(true)
        }
    }


}