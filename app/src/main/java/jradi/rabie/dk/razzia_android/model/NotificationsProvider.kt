package jradi.rabie.dk.razzia_android.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_ALARM
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import jradi.rabie.dk.razzia_android.R
import jradi.rabie.dk.razzia_android.view.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.concurrent.timer
import kotlin.concurrent.timerTask


/**
 * @author rabie
 *
 *
 */
private const val serviceChannelId = "patrulje_oste_kanalen"
private const val alertChannelId = "oste_kanalen"

sealed class NotificationId(val value: Int) {
    object Alert : NotificationId(1)
    object Collision : NotificationId(2)
}


interface CollisionServiceNotificationPresenterInterface {
    suspend fun showServiceActiveNotification()
    suspend fun hideServiceActiveNotification()
}

class CollisionServiceNotificationPresenter : CollisionServiceNotificationPresenterInterface {

    private val notificationManager = App.appContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = NotificationId.Collision.value

    override suspend fun showServiceActiveNotification() {
        withContext(Dispatchers.Main) {
            val mapsActivityIntent = Intent(App.appContext, MapsActivity::class.java)
            //TODO fix bug with new task intent flag causing the app to open up on already existing instance
            val mapsActivityPendingIntent = PendingIntent.getActivity(App.appContext, mapsActivityAlertNotificationIntentRequestCode, mapsActivityIntent, 0)

            //Create notification channel so newer Android versions are supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(serviceChannelId, stringResource(R.string.notifications), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = stringResource(R.string.notifications)
                }
                notificationManager.createNotificationChannel(notificationChannel)
            }

            //Build the actual notification
            val notificationBuilder = NotificationCompat.Builder(App.appContext, serviceChannelId).apply {
                setContentIntent(mapsActivityPendingIntent)
                setPriority(Notification.PRIORITY_MAX)
                setAutoCancel(false)
                setSmallIcon(jradi.rabie.dk.razzia_android.R.drawable.ic_launcher_foreground) //TODO replace with a proper icon
                setTicker(stringResource(R.string.watching_your_back))
                setContentTitle(stringResource(R.string.watching_your_back))
                setContentText(stringResource(R.string.watching_your_back_description))

            }

            //Show the notification
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    override suspend fun hideServiceActiveNotification() {
        withContext(Dispatchers.Main) {
            notificationManager.cancel(notificationId)
        }
    }
}


interface AlertNotificationPresenterInterface {
    suspend fun alertUser()
}

class AlertNotificationPresenter : AlertNotificationPresenterInterface {

    private val notificationManager = App.appContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val alertNotificationId = NotificationId.Alert.value
    private val oneMinutesInMilliSec: Long = 60000
    private var cleanupNotificationJob: Job? = null

    /**
     * Warn the user about incoming collision
     */
    override suspend fun alertUser() {
        withContext(Dispatchers.Main) {

            val soundUri = createSoundUri()

            val alertVibrationPattern = longArrayOf(0, 1000, 200, 200, 500, 1000, 200, 200, 500, 1000, 200, 200)

            val mapsActivityIntent = Intent(App.appContext, MapsActivity::class.java)
            //TODO fix bug with new task intent flag causing the app to open up on already existing instance
            val mapsActivityPendingIntent = PendingIntent.getActivity(App.appContext, mapsActivityAlertNotificationIntentRequestCode, mapsActivityIntent, 0)

            //Create notification channel so newer Android versions are supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(alertChannelId, stringResource(R.string.notifications), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = stringResource(R.string.notifications)
                    enableVibration(true)
                    vibrationPattern = alertVibrationPattern
                    setSound(soundUri, AudioAttributes.Builder().setUsage(USAGE_ALARM).build())
                }
                notificationManager.createNotificationChannel(notificationChannel)
            }

            //Build the actual notification
            val notificationBuilder = NotificationCompat.Builder(App.appContext, alertChannelId).apply {
                setContentIntent(mapsActivityPendingIntent)
                setPriority(Notification.PRIORITY_MAX)
                setSound(soundUri)
                setAutoCancel(true)
                setSmallIcon(jradi.rabie.dk.razzia_android.R.drawable.ic_launcher_foreground) //TODO replace with a proper icon
                setVibrate(alertVibrationPattern)
                setTicker(stringResource(R.string.watch_out))
                setContentTitle(stringResource(R.string.watch_out))
                setContentText(stringResource(R.string.watch_out_description))

            }

            //Since we are about to show a new notification lets make sure to remove the old cleanup job
            cleanupNotificationJob?.cancel()

            //Show the notification
            notificationManager.notify(alertNotificationId, notificationBuilder.build())

            cleanupNotificationJob = launch {
                delay(oneMinutesInMilliSec)
                notificationManager.cancel(alertNotificationId)
            }
        }
    }

    private fun createSoundUri(): Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).let { alarmSoundUri ->
        if (alarmSoundUri != null) {
            return alarmSoundUri
        }

        val notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (notificationSoundUri != null) {
            return notificationSoundUri
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

}

/**
 * Class used for testing
 */
class MockedAlertNotifcationPresenter : AlertNotificationPresenterInterface {
    suspend override fun alertUser() {
        logPrint("Oh no! The smell of cheese is closing in!")
    }
}