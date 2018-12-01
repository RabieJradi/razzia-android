package jradi.rabie.dk.razzia_android.viewmodel

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import jradi.rabie.dk.razzia_android.model.ActivityRecognitionClientProvider
import jradi.rabie.dk.razzia_android.model.BikeActivityRecognitionClientProviderCreator
import jradi.rabie.dk.razzia_android.model.ConfigurationProvider
import jradi.rabie.dk.razzia_android.view.PermissionProviderInterface
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @author rabie
 *
 *
 */


class MapViewModel {

    suspend fun init(googleMapsProvider: GoogleMapsProvider,
                     bikeActivityRecognitionClientProviderCreator: BikeActivityRecognitionClientProviderCreator) {
        val mapPageProvider = MapPageProvider(googleMapsProvider = googleMapsProvider, ,activityRecognitionClientProvider = bikeActivityRecognitionClientProviderCreator.create())
        try {
            mapPageProvider.plotMarkersOnMap()
        } catch (e: CancellationException) {
            //TODO show error state so user can hit retry
            throw e //re-throw exception so view can handle it too
        }
    }
}


class GoogleMapsProvider(private val mapFragment: SupportMapFragment) {

    suspend fun getGoogleMaps() = withContext(Dispatchers.Main) {
        suspendCoroutine<GoogleMap> { continuation ->
            mapFragment.getMapAsync { googleMap ->
                continuation.resume(googleMap)
            }
        }
    }
}


class MapPageProvider(private val googleMapsProvider: GoogleMapsProvider,
                      private val permissionProvider: PermissionProviderInterface,
                      private val activityRecognitionClientProvider: ActivityRecognitionClientProvider) {

    private val entriesProvider = ConfigurationProvider.config.entriesDataProvider

    suspend fun plotMarkersOnMap() {
        val googleMap = googleMapsProvider.getGoogleMaps()

        val hasLocationPermission = permissionProvider.getLocationPermission()
        if (hasLocationPermission) {
            googleMap.isMyLocationEnabled = true
            activityRecognitionClientProvider.requestActivityUpdates()
        } else{
            activityRecognitionClientProvider.stopActivityUpdates()
        }
        val mapMarkers = entriesProvider.getEntries().map {
            val latLng = LatLng(it.location.latitude, it.location.longitude)
            return@map MarkerOptions().position(latLng).title(it.description)
        }

        //TODO note, not sure if we are allowed to do this on a background thread.
        mapMarkers.forEach { googleMap.addMarker(it) }
    }

}