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

    fun createActivityRecognitionCacheProvider(context: Context): CacheProviderInterface {
        return CacheProvider(cacheKey = CacheKey.ActivityRecognitionCache, context = context)
    }
    //TODO WARNING: does this singleton live even if the only thing that runs is an intent service? What happens when the Intent Service is shut down and GC'ed because it has run out of work?
}

interface Configuration {
    val detectionIntervalInMilliseconds: Long
    val entriesDataProvider: EntriesDataProviderInterface
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
                val localObject = UserLocationProvider(context)
                userLocationProvider = localObject
                localObject
            }.invoke()
        }

    }

    override fun getCollisionDetectorProvider(context: Context): CollisionDetectorProviderInterface {
        synchronized(this) {
            return collisionDetectorProvider ?: {
                val localObject = CollisionDetectorProvider(locationProvider = getUserLocationProvider(context), entriesDataProvider = entriesDataProvider, alertProvider = alertProvider)
                collisionDetectorProvider = localObject
                localObject
            }.invoke()
        }
    }
}

class ProductionConfig : BaseProductionConfiguration() {
    override val detectionIntervalInMilliseconds = 30_000L

    override val alertProvider = AlertProvider(AndroidSoundProvider())
    override val entriesDataProvider = EntriesDataProvider()
}

class TestConfig : BaseProductionConfiguration() {
    override val detectionIntervalInMilliseconds = 1000L
    override val alertProvider = AlertProvider(MockedSoundProvider())
    override val entriesDataProvider = MockedEntriesDataProvider()

    override fun createActivityRecognitionCacheProvider(context: Context): CacheProviderInterface {
        return CacheProvider(cacheKey = CacheKey.ActivityRecognitionCache, context = context)
    }

    //TODO create a mock user location provider that emits fx 5 user location updates along a straight path hitting one or more fences.
}