package jradi.rabie.dk.razzia_android.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.SupportMapFragment
import jradi.rabie.dk.razzia_android.R
import jradi.rabie.dk.razzia_android.databinding.ActivityMapsBinding
import jradi.rabie.dk.razzia_android.model.BikeActivityRecognitionClientProviderCreator
import jradi.rabie.dk.razzia_android.model.services.CollisionDetectorService
import jradi.rabie.dk.razzia_android.viewmodel.GoogleMapsProvider
import jradi.rabie.dk.razzia_android.viewmodel.MapViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


//TODO implement a broadcast receiver that is started on each phone restart in order to subscribe to google activity recognition framework

fun logPrint(message: String) {
    Log.d("Osteklokken", message)
}

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

        } else{
            continuation.resume(true)
        }
    }
}

class MapsActivity : PermissionRequestActivity() {

    val viewModel = MapViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ActivityMapsBinding>(this, R.layout.activity_maps)
        binding.viewModel = viewModel

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment

        viewModel.init(googleMapsProvider = GoogleMapsProvider(mapFragment),
                permissionProvider = this@MapsActivity,
                bikeActivityRecognitionClientProviderCreator = BikeActivityRecognitionClientProviderCreator(activity = this@MapsActivity))

        //TODO delete this line of code
        startService(Intent(this, CollisionDetectorService::class.java))
    }

    override fun onResume() {
        super.onResume()
        launch {
            try {
                viewModel.onResume()
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

