package jradi.rabie.dk.razzia_android.view

import android.Manifest
import android.app.IntentService
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.maps.SupportMapFragment
import jradi.rabie.dk.razzia_android.R
import jradi.rabie.dk.razzia_android.databinding.ActivityMapsBinding
import jradi.rabie.dk.razzia_android.model.BikeActivityRecognitionClientProviderCreator
import jradi.rabie.dk.razzia_android.model.CollisionDetectorService
import jradi.rabie.dk.razzia_android.model.ConfigurationProvider
import jradi.rabie.dk.razzia_android.viewmodel.GoogleMapsProvider
import jradi.rabie.dk.razzia_android.viewmodel.MapViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


//TODO implement a broadcast receiver that is started on each phone restart in order to subscribe to google activity recognition framework

interface PermissionProviderInterface {
    suspend fun getLocationPermission(): Boolean
}

abstract class PermissionRequestActivity : PermissionProviderInterface, ScopedAppActivity() {
    private val LOCATION_PERMISSION_REQUEST_CODE = 12
    private var onPermissionRequestHandled: (Boolean) -> Unit = {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) return
        onPermissionRequestHandled((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED))
        onPermissionRequestHandled = {}
    }

    override suspend fun getLocationPermission() = suspendCoroutine<Boolean> { continuation ->
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            onPermissionRequestHandled = { isHandled ->
                continuation.resume(isHandled)
            }
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE)

        }
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
                viewModel.init(googleMapsProvider = GoogleMapsProvider(mapFragment),
                        permissionProvider = this@MapsActivity,
                        bikeActivityRecognitionClientProviderCreator = BikeActivityRecognitionClientProviderCreator(activity = this@MapsActivity))
            } catch (e: CancellationException) {
                showToast(e.message ?: "Something went wrong")
            }
        }
    }

    private suspend fun showToast(text: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MapsActivity, text, Toast.LENGTH_LONG).show()
        }
    }

}

class BikeActivityRecognitionService : IntentService("BikeActivityRecognitionService") {
    private val collisionDetectorProvider = ConfigurationProvider.config.getCollisionDetectorProvider(context = this)

    private fun startCollisionDetectorService() = startService(Intent(this, CollisionDetectorService::class.java))

    override fun onHandleIntent(incomingIntent: Intent?) {
        val intent = incomingIntent ?: return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val events = result.transitionEvents ?: return

        Log.d("[RecognitionService]", "events:\n" + events)
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

