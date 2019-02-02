package jradi.rabie.dk.razzia_android.model


interface AlertFilterProviderInterface {
    suspend fun isAlertAllowed(): Boolean
}

//TODO consider adding a push notification provider, so the user has a fast way to enter the app
/**
 * Responsible for suppressing a request to isAlertAllowed the user about a collision event if user already have had an isAlertAllowed not too long ago.
 */
class AlertFilterProvider(private val alertThresholdInSeconds: Int = 30) : AlertFilterProviderInterface {
    private var systemTimeSinceLastAlert : Long = 0

    override suspend fun isAlertAllowed(): Boolean {
        System.currentTimeMillis()
        val secondsAgoSinceLastAlert = (System.currentTimeMillis() - systemTimeSinceLastAlert).div(1000)
        if (secondsAgoSinceLastAlert >= alertThresholdInSeconds) {
            systemTimeSinceLastAlert = System.currentTimeMillis()
            return true
        }
        return false
    }
}
