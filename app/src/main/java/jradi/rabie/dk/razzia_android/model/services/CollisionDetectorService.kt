package jradi.rabie.dk.razzia_android.model.services

import android.app.IntentService
import android.content.Intent
import jradi.rabie.dk.razzia_android.model.ConfigurationProvider
import jradi.rabie.dk.razzia_android.view.App
import jradi.rabie.dk.razzia_android.view.logPrint
import kotlinx.coroutines.runBlocking

/**
 * When this service is started, it will launch a blocking coroutine which will not release the service Thread unless the coroutine job is cancelled from outside, so the service isn't killed and the coroutine GC'ed.
 */
class CollisionDetectorService : IntentService("CollisionDetectorService") {

    private val collisionDetectorProvider = ConfigurationProvider.config.getCollisionDetectorProvider(context = App.appContext)
    private val collisionServiceNotificationPresenter = ConfigurationProvider.config.collisionServiceNotificationPresenter

    override fun onHandleIntent(intent: Intent?) = runBlocking<Unit> {
        collisionServiceNotificationPresenter.showServiceActiveNotification()
        logPrint("[CollisionDetectorService] onHandleIntent called")
        collisionDetectorProvider.watch()
        logPrint("[CollisionDetectorService] Stopped observing for collisions")
        stopSelf() //Kill this service as we the watching have been cancelled anyway.
        collisionServiceNotificationPresenter.hideServiceActiveNotification()
    }
}