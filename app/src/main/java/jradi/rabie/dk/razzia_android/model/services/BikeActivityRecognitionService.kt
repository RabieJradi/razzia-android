package jradi.rabie.dk.razzia_android.model.services

import android.app.IntentService
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import jradi.rabie.dk.razzia_android.model.ConfigurationProvider
import jradi.rabie.dk.razzia_android.model.services.TransitionActivityRecognitionHelper.DerivedTransitionActivity
import jradi.rabie.dk.razzia_android.model.services.TransitionActivityRecognitionHelper.SupportedActivityType.onBike
import jradi.rabie.dk.razzia_android.view.App
import jradi.rabie.dk.razzia_android.view.logPrint


//FIXME change to activity recognition: service should be started up based on an intent from a registered broadcast receiver so we don't have to have a service running all the time
//https://stackoverflow.com/questions/17445765/android-activity-recognition-with-listener-or-broadcast-receiver

/**
 * Notice that this service compared to CollisionDetectorService doesn't block the thread as we need to be able to handle new
 * activity events from the system. Therefore we don't use the blocking CollisionDetectorProvider#watch method. We instead
 * let the CollisionDetectorService handle that.
 */
class BikeActivityRecognitionService : IntentService("BikeActivityRecognitionService") {

    private val collisionDetectorProvider = ConfigurationProvider.config.getCollisionDetectorProvider(context = App.appContext)

    private fun startCollisionDetectorService() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(Intent(App.appContext, CollisionDetectorService::class.java))
    } else {
        startService(Intent(App.appContext, CollisionDetectorService::class.java))
    }

    override fun onHandleIntent(incomingIntent: Intent?) {
        logPrint("[CollisionDetectorService] onHandleIntent called")
        val intent = incomingIntent ?: return
        val intentResult = ActivityTransitionResult.extractResult(intent) ?: return

        val transitionEvents = intentResult.transitionEvents ?: return

        logPrint("[RecognitionService] events:\n" + transitionEvents)

        val recognitionHelper = TransitionActivityRecognitionHelper()
        //reverse the events order so first index contains latest event
        val reversed = transitionEvents.reversed()

        val derivedActivity = recognitionHelper.checkTransitionActivity(allActivityEvents = reversed, activityTypeToLookFor = onBike)

        when (derivedActivity) {
            DerivedTransitionActivity.unknown -> {
                //If we couldn't derive the current activity we just ignore the event
                // as we don't want to risk stopped the detector prematurely or by mistake.
            }
            DerivedTransitionActivity.started -> {
                //Start watching, user has started biking
                startCollisionDetectorService()
            }
            DerivedTransitionActivity.stopped -> {
                //Stop watching as user have stopped biking
                collisionDetectorProvider.abort()
            }
        }
    }
}

//TODO to complement google's activity recognition we could implement our own system that 

//TODO unit test this class
/**
 * This class can figure out which activity you are transitioning into.
 */
class TransitionActivityRecognitionHelper {

    enum class SupportedActivityType(val value: Int) {
        inVehicle(DetectedActivity.IN_VEHICLE), onBike(DetectedActivity.ON_BICYCLE), onFoot(DetectedActivity.ON_FOOT)
    }

    enum class DerivedTransitionActivity {
        started, stopped, unknown
    }

    fun checkTransitionActivity(allActivityEvents: List<ActivityTransitionEvent>, activityTypeToLookFor: SupportedActivityType): DerivedTransitionActivity {

        val filteredEvents = allActivityEvents.filter { it.activityType == activityTypeToLookFor.value }

        //split the filtered events into two lists of indices
        val exitEvents = filteredEvents.filter { it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT }.mapIndexed { index, event -> index }
        val enterEvents = filteredEvents.filter { it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER }.mapIndexed { index, event -> index }

        if (exitEvents.isEmpty() && enterEvents.isEmpty()) {
            return DerivedTransitionActivity.unknown
        }
        if (exitEvents.isEmpty() && enterEvents.isNotEmpty()) {
            return DerivedTransitionActivity.started
        }
        if (exitEvents.isNotEmpty() && enterEvents.isEmpty()) {
            return DerivedTransitionActivity.stopped
        }
        // We have to look at which of the two events occurred the earliest in order to know the user's current activity
        if (exitEvents.sorted()[0] < enterEvents.sorted()[0]) {
            return DerivedTransitionActivity.stopped
        } else {
            return DerivedTransitionActivity.started
        }
    }
}