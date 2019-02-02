package jradi.rabie.dk.razzia_android.model

import android.content.Context
import jradi.rabie.dk.razzia_android.BuildConfig
import jradi.rabie.dk.razzia_android.mocked.MockedUserLocationProvider

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
    val alertProvider: AlertProviderInterface

    fun createActivityRecognitionCacheProvider(context: Context): CacheProviderInterface
    fun getUserLocationProvider(context: Context): UserLocationProviderInterface
    fun getCollisionDetectorProvider(context: Context): CollisionDetectorProviderInterface
}

/**
 * NB: we hold references (instead of recreating the objects each time) to instances because we either want to retrain the state that these classes contain and/or want to avoid using synchronized/locks for lazy initialisation
 */
abstract class BaseProductionConfiguration : Configuration {

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
                val localObject = CollisionDetectorProvider(locationProvider = getUserLocationProvider(context), entriesDataProvider = entriesDataProvider, alertFilterProvider = alertFilterProvider, fenceCollisionDetector = NaiveFenceCollisionDetector(), alertProvider = alertProvider)
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
    override val alertProvider = AndroidAlertProvider()
}

/**
 * Debug setup
 */
class TestConfig : BaseProductionConfiguration() {
    override val detectionIntervalInMilliseconds = 1000L
    override val entriesDataProvider = MockedEntriesDataProvider()
    override val alertFilterProvider = AlertFilterProvider()
    override val alertProvider = MockedAlertProvider()

    private var userLocationProvider: UserLocationProviderInterface? = null

    override fun createActivityRecognitionCacheProvider(context: Context): CacheProviderInterface {
        return CacheProvider(cacheKey = CacheKey.ActivityRecognitionCache, context = context)
    }

    override fun getUserLocationProvider(context: Context): UserLocationProviderInterface {
        synchronized(this) {
            return userLocationProvider ?: {
                val localObject = MockedUserLocationProvider()
                userLocationProvider = localObject
                localObject
            }.invoke()
        }
    }
}