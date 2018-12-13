package jradi.rabie.dk.razzia_android.model

import android.app.IntentService
import android.content.Intent
import jradi.rabie.dk.razzia_android.model.entities.CircularFence
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


/**
 * When this service is started, it will launch a blocking coroutine which will release the service Thread unless the coroutine job is cancelled from outside.
 */
class CollisionDetectorService : IntentService("CollisionDetectorService") {

    private val collisionDetectorProvider = ConfigurationProvider.config.getCollisionDetectorProvider(context = this)

    override fun onHandleIntent(intent: Intent?) = runBlocking<Unit> {
        collisionDetectorProvider.watch()
    }

    override fun onDestroy() {
        super.onDestroy()
        //We expect the provider to have been aborted already by BikeActivityRecognitionService, but just for safety we abort it here as well
        collisionDetectorProvider.abort()
    }
}

interface CollisionDetectorProviderInterface {
    suspend fun watch()
    fun abort()
}

/**
 * Responsible for interacting with the AlertProvider should there be a collision between user location and an entry boundary
 */
class CollisionDetectorProvider(private val locationProvider: UserLocationProviderInterface,
                                private val entriesDataProvider: EntriesDataProviderInterface,
                                private val alertProvider: AlertProviderInterface) : CollisionDetectorProviderInterface {
    @Volatile
    private var job: Job? = null

    /**
     * Calling this method will result in a looping execution until the job is cancelled, where it will look at potential collisions with reported entries on each location update
     */
    override suspend fun watch() {
        job = GlobalScope.launch {
            val channel = locationProvider.observeLocationUpdates(this)
            val fenceCollisionDetector = NaiveFenceCollisionDetector()
            val entries = entriesDataProvider.getEntries()
            for (locationUpdate in channel) {
                val collisions = fenceCollisionDetector.findCollisions(entries.map { CircularFence(id = it.id, radiusInMeters = it.radiusInMeters, center = it.location) }, locationUpdate)
                if (collisions.isNotEmpty()) {
                    alertProvider.alert()
                }
            }
        }
    }

    override fun abort() {
        job?.cancel()
    }

}