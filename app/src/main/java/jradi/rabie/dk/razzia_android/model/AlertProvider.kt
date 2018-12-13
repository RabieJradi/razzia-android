package jradi.rabie.dk.razzia_android.model


interface AlertProviderInterface {
    suspend fun alert(): Boolean
}

//TODO consider adding a push notification provider, so the user has a fast way to enter the app
/**
 * Responsible for suppressing a request to alert the user about a collision event if user already have had an alert not too long ago.
 */
class AlertProvider(private val soundProvider: SoundProviderInterface, private val alertThresholdInSeconds: Int = 30) : AlertProviderInterface {
    private var systemTimeSinceLastAlert : Long = 0

    override suspend fun alert(): Boolean {
        System.currentTimeMillis()
        val secondsAgoSinceLastAlert = (System.currentTimeMillis() - systemTimeSinceLastAlert).div(1000)
        if (secondsAgoSinceLastAlert >= alertThresholdInSeconds) {
            soundProvider.makeSound()
            systemTimeSinceLastAlert = System.currentTimeMillis()
            return true
        }
        return false
    }
}
