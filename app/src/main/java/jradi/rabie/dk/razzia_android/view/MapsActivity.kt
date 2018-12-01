package jradi.rabie.dk.razzia_android.view

import android.app.IntentService
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.maps.SupportMapFragment
import jradi.rabie.dk.razzia_android.R
import jradi.rabie.dk.razzia_android.databinding.ActivityMapsBinding
import jradi.rabie.dk.razzia_android.model.*
import jradi.rabie.dk.razzia_android.viewmodel.GoogleMapsProvider
import jradi.rabie.dk.razzia_android.viewmodel.MapViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


//TODO consider making clean interfaces with code that is platform specific (for Android) so that we can reuse logic in a Kotlin Native project so we can support iOS devices

//TODO implement a broadcast receiver that is started on each phone restart in order to subscripe to google activity recognition framework

//FIXME calls to location and recognition services can throw an execption even if we check for permission. we need to wrap them in try catch


interface PermissionProviderInterface {
    suspend fun getLocationPermission(): Boolean
}

//TODO implement this class so we can handle permission requests
abstract class PermissionRequestActivity : PermissionProviderInterface, ScopedAppActivity() {

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override suspend fun getLocationPermission(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class MapsActivity : PermissionRequestActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ActivityMapsBinding>(this, R.layout.activity_maps)
        val viewModel = MapViewModel()
        binding.viewModel = viewModel

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment

        launch {
            try {
                viewModel.init(googleMapsProvider = GoogleMapsProvider(mapFragment), bikeActivityRecognitionClientProviderCreator = BikeActivityRecognitionClientProviderCreator())
            } catch (e: CancellationException) {
                showToast(e.message ?: "Something went wrong")
            }
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

}

class BikeActivityRecognitionService : IntentService("BikeActivityRecognitionService") {
    private val collisionDetectorProvider = ConfigurationProvider.config.getCollisionDetectorProvider(context = this)

    private fun startCollisionDetectorService() = startService(Intent(this,CollisionDetectorService::class.java))

    override fun onHandleIntent(incomingIntent: Intent?) {
        val intent = incomingIntent ?: return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val events = result.transitionEvents ?: return

        //reverse the events order so first index contains latest event
        val bikeEvents = events.reversed().filter { it.activityType == DetectedActivity.ON_BICYCLE }

        val bikeExitEvents = bikeEvents.filter { it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT }.mapIndexedNotNull { index, event -> index }
        val bikeEnterEvents = bikeEvents.filter { it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER }.mapIndexedNotNull { index, event -> index }

        if (bikeExitEvents.isEmpty() && bikeEnterEvents.isEmpty()) {
            //Why did we get an intent to handle then? Halt execution.
            return
        }

        if (bikeExitEvents.isEmpty() && bikeEnterEvents.isNotEmpty()) {
            //User started biking
            startCollisionDetectorService()
            return
        }
        if (bikeExitEvents.isNotEmpty() && bikeEnterEvents.isEmpty()) {
            collisionDetectorProvider.abort()
            return
        }
        // We have to look at which of the two events occurred the earliest in order to know the user's current activity
        if (bikeExitEvents.sorted()[0] < bikeEnterEvents.sorted()[0]) {
            //Stop watching as user have stopped biking
            collisionDetectorProvider.abort()
        } else {
            //Start watching, user has started biking
            startCollisionDetectorService()
        }
        return

    }
}

