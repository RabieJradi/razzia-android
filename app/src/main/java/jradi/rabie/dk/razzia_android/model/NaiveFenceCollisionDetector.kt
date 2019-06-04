package jradi.rabie.dk.razzia_android.model

import android.location.Location
import jradi.rabie.dk.razzia_android.model.entities.CircularFence
import jradi.rabie.dk.razzia_android.model.entities.toLocation


/**
 * @author rabie
 *
 */

interface FenceCollisionDetectorInterface {
    fun findCollisions(fences: List<CircularFence>, userLocation: Location): List<CircularFence>
}

/**
 * Responsible for looping through current Entry objects and check if user location has crossed the circular "fence" around each point.
 */
class NaiveFenceCollisionDetector : FenceCollisionDetectorInterface {
    override fun findCollisions(fences: List<CircularFence>, userLocation: Location): List<CircularFence> {
        return fences.mapNotNull { fence ->
            val fenceLocation = fence.center.toLocation()
            if (fenceLocation.distanceTo(userLocation) <= fence.radiusInMeters) {
                return@mapNotNull fence
            }
            return@mapNotNull null
        }
    }
}
