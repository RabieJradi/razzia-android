package jradi.rabie.dk.razzia_android.model

import android.location.Location
import jradi.rabie.dk.razzia_android.model.entities.CircularFence
import jradi.rabie.dk.razzia_android.model.entities.Id
import jradi.rabie.dk.razzia_android.view.logPrint
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CancellationException


interface CollisionDetectorProviderInterface {
    suspend fun watch()
    fun abort()
}

/**
 * Responsible for interacting with the AlertProvider should there be a collision between user location and an entry boundary
 */
class CollisionDetectorProvider(private val locationProvider: UserLocationProviderInterface,
                                private val locationObserverHandler: LocationObserverHandlerInterface) : CollisionDetectorProviderInterface {

    //TODO OBS potential raceconditions with this channel variable
    private var channel: Channel<Location>? = null

    /**
     * Calling this method will result in a looping execution. It will process a user location update
     * for each iteration and look at potential collisions with current known entries.
     */
    override suspend fun watch() {
        logPrint("[CollisionDetectorProvider] watch")
        val currentChannel = locationProvider.observeLocationUpdates()
        channel = currentChannel
        for (locationUpdate in currentChannel) {
            locationObserverHandler.checkForCollisions(locationUpdate = locationUpdate)
        }
    }

    override fun abort() {
        logPrint("[CollisionDetectorProvider] abort called")
        channel?.cancel()
    }
}

sealed class CollisionCheckResult {
    object DataProviderError : CollisionCheckResult()
    object NoCollisions : CollisionCheckResult()
    data class Collisions(val entityIds: List<Id>, val didAlertUser: Boolean) : CollisionCheckResult()
}

interface LocationObserverHandlerInterface {
    suspend fun checkForCollisions(locationUpdate: Location): CollisionCheckResult
}

//TODO unit test this class
/**
 * This class is responsible for reacting on location updates by checking up on any collisions between user location and current entities and making sure to alert the user about that collision.
 */
class LocationObserverHandler(private val entriesDataProvider: EntriesDataProviderInterface,
                              private val alertFilterProvider: AlertFilterProviderInterface,
                              private val fenceCollisionDetector: FenceCollisionDetectorInterface,
                              private val alertNotificationPresenter: AlertNotificationPresenterInterface) : LocationObserverHandlerInterface {

    override suspend fun checkForCollisions(locationUpdate: Location): CollisionCheckResult {
        logPrint("[CollisionDetectorProvider] checking for collisions")
        try {
            val entries = entriesDataProvider.getEntries()
            val fences = entries.map { CircularFence(id = it.id, radiusInMeters = it.radiusInMeters, center = it.location) }
            val collisions = fenceCollisionDetector.findCollisions(fences = fences, userLocation = locationUpdate)
            if (collisions.isNotEmpty()) {
                val shouldAlertUser = alertFilterProvider.isAlertAllowed()
                if (shouldAlertUser) {
                    //TODO consider sending the distance to the closest collision so that we can display an informative push notification. the returned list of collisions should be sorted so closest is first index
                    alertNotificationPresenter.alertUser()
                }
                return CollisionCheckResult.Collisions(entityIds = collisions.map { it.id }, didAlertUser = shouldAlertUser)
            }
            return CollisionCheckResult.NoCollisions

        } catch (e: CancellationException) {
            //We caught an entry data provider error
            logPrint("[CollisionDetectorProvider] CancellationException e=${e.toString()}")
            return CollisionCheckResult.DataProviderError
        }
    }

}