package jradi.rabie.dk.razzia_android.model

import android.location.Location
import jradi.rabie.dk.razzia_android.model.entities.CircularFence
import jradi.rabie.dk.razzia_android.view.logPrint
import kotlinx.coroutines.channels.Channel


interface CollisionDetectorProviderInterface {
    suspend fun watch()
    fun abort()
}

//TODO unit this this class with mocked location replay data. Make sure to have a test case where the API call also fail
/**
 * Responsible for interacting with the AlertProvider should there be a collision between user location and an entry boundary
 */
class CollisionDetectorProvider(private val locationProvider: UserLocationProviderInterface,
                                private val entriesDataProvider: EntriesDataProviderInterface,
                                private val alertFilterProvider: AlertFilterProviderInterface,
                                private val fenceCollisionDetector: FenceCollisionDetectorInterface,
                                private val alertProvider: AlertProviderInterface) : CollisionDetectorProviderInterface {

    private var channel: Channel<Location>? = null

    /**
     * Calling this method will result in a looping execution. It will process a user location update
     * for each iteration and look at potential collisions with current known entries.
     */
    override suspend fun watch() {
        logPrint("[CollisionDetectorProvider] watch")
        val currentChannel = locationProvider.observeLocationUpdates()
        channel = currentChannel

        //FIXME how do we handle a HTTP request failure? Should we try up to 3 times before returning completely from this watch method? Otherwise the service will get aborted.
        //TODO currently if the API should fail, the enture suspend method fails and the service will get killed.
        val entries = entriesDataProvider.getEntries()
        for (locationUpdate in currentChannel) {
            val collisions = fenceCollisionDetector.findCollisions(entries.map { CircularFence(id = it.id, radiusInMeters = it.radiusInMeters, center = it.location) }, locationUpdate)
            if (collisions.isNotEmpty()) {
                if(alertFilterProvider.isAlertAllowed()){
                    //TODO consider sending the distance to the closest collision so that we can display an informative push notification
                    alertProvider.alertUser()
                }
            }
        }
    }

    override fun abort() {
        logPrint("[CollisionDetectorProvider] abort called")
        channel?.cancel()
    }
}