package jradi.rabie.dk.razzia_android.model

import android.content.Context
import jradi.rabie.dk.razzia_android.BuildConfig

/**
 * @author rabie
 *
 * This class holds on to references that we want access to
 */
object ConfigurationProvider {

    val config: Configuration = if (BuildConfig.BUILD_TYPE == "release") {
        ProductionConfig()
    } else {
        TestConfig()
    }
}

interface Configuration {
    val detectionIntervalInMilliseconds: Long
    val entriesDataProvider: EntriesDataProviderInterface
    val alertFilterProvider: AlertFilterProviderInterface
    val alertNotificationPresenter: AlertNotificationPresenterInterface
    val collisionServiceNotificationPresenter: CollisionServiceNotificationPresenterInterface

    fun createActivityRecognitionCacheProvider(context: Context): CacheProviderInterface
    fun getUserLocationProvider(context: Context): UserLocationProviderInterface
    fun getCollisionDetectorProvider(context: Context): CollisionDetectorProviderInterface
}

/**
 * NB: we hold references (instead of recreating the objects each time) to instances because we either want to retrain the state that these classes contain and/or want to avoid using synchronized/locks for lazy initialisation
 */
abstract class BaseProductionConfiguration : Configuration {

    //FIXME use lazy property delegates to reduce singleton sync boiler plate code! https://kotlinlang.org/docs/reference/delegated-properties.html
    private var collisionDetectorProvider: CollisionDetectorProviderInterface? = null
    private var userLocationProvider: UserLocationProviderInterface? = null

    override fun createActivityRecognitionCacheProvider(context: Context): CacheProviderInterface {
        return CacheProvider(cacheKey = CacheKey.ActivityRecognitionCache, context = context)
    }

    override fun getUserLocationProvider(context: Context): UserLocationProviderInterface {
        synchronized(this) {
            return userLocationProvider ?: {
                val localObject = FusedUserLocationProvider(context)
                userLocationProvider = localObject
                localObject
            }.invoke()
        }
    }

    override fun getCollisionDetectorProvider(context: Context): CollisionDetectorProviderInterface {
        synchronized(this) {
            return collisionDetectorProvider ?: {
                val locationObserverHandler = LocationObserverHandler(entriesDataProvider = entriesDataProvider, alertFilterProvider = alertFilterProvider, fenceCollisionDetector = NaiveFenceCollisionDetector(), alertNotificationPresenter = alertNotificationPresenter)
                val localObject = CollisionDetectorProvider(locationProvider = getUserLocationProvider(context), locationObserverHandler = locationObserverHandler)
                collisionDetectorProvider = localObject
                localObject
            }.invoke()
        }
    }
}

/**
 * Production setup
 */
class ProductionConfig : BaseProductionConfiguration() {
    override val detectionIntervalInMilliseconds = 30_000L
    override val entriesDataProvider = EntriesDataProvider()
    override val alertFilterProvider = AlertFilterProvider()
    override val alertNotificationPresenter = AlertNotificationPresenter()
    override val collisionServiceNotificationPresenter = CollisionServiceNotificationPresenter()
}

/**
 * Debug setup
 */
class TestConfig : BaseProductionConfiguration() {
    override val detectionIntervalInMilliseconds = 1000L
    override val entriesDataProvider = MockedEntriesDataProvider()
    override val alertFilterProvider = AlertFilterProvider()
    override val alertNotificationPresenter = AlertNotificationPresenter()
    override val collisionServiceNotificationPresenter = CollisionServiceNotificationPresenter()

    private var userLocationProvider: UserLocationProviderInterface? = null

    override fun createActivityRecognitionCacheProvider(context: Context): CacheProviderInterface {
        return CacheProvider(cacheKey = CacheKey.ActivityRecognitionCache, context = context)
    }

    override fun getUserLocationProvider(context: Context): UserLocationProviderInterface {
        synchronized(this) {
            return userLocationProvider ?: {
                val localObject = FusedUserLocationProvider(context)
                userLocationProvider = localObject
                localObject
            }.invoke()
        }
    }
}