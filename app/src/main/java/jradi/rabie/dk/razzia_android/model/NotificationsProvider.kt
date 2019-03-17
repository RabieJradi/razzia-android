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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * @author rabie
 *
 *
 */

interface AlertNotificationProviderInterface {
    suspend fun alertUser()
}

interface CollisionServiceNotificationProviderInterface {
    suspend fun showServiceActiveNotification()
    suspend fun hideServiceActiveNotification()
}

class NotificationPresenter : AlertNotificationProviderInterface, CollisionServiceNotificationProviderInterface {
    override suspend fun showServiceActiveNotification() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun hideServiceActiveNotification() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val notificationManager = App.appContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    private val channelId = "oste_kanalen"
    private val alertNotificationId = 1

    /**
     * Warn the user about incoming collision
     */
    override suspend fun alertUser() {
        withContext(Dispatchers.Main) {

            val soundUri = createSoundUri()

            val alertVibrationPattern = longArrayOf(0, 1000, 200, 200, 500, 1000, 200, 200, 500, 1000, 200, 200)

            val mapsActivityIntent = Intent(App.appContext, MapsActivity::class.java)
            val mapsActivityPendingIntent = PendingIntent.getActivity(App.appContext, mapsActivityAlertNotificationIntentRequestCode, mapsActivityIntent, 0)

            //Create notification channel so newer Android versions are supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(channelId, stringResource(R.string.notifications), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = stringResource(R.string.notifications)
                    enableVibration(true)
                    vibrationPattern = alertVibrationPattern
                    setSound(soundUri, AudioAttributes.Builder().setUsage(USAGE_ALARM).build())
                }
                notificationManager.createNotificationChannel(notificationChannel)
            }

            //Build the actual notification
            val notificationBuilder = NotificationCompat.Builder(App.appContext, channelId).apply {
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

            //Show the notification
            notificationManager.notify(alertNotificationId, notificationBuilder.build())
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

/*
 WORKING SOLUTION:

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(channelId, "My Notifications", NotificationManager.IMPORTANCE_MAX)

                // Configure the notification channel.
                notificationChannel.description = "Channel description"
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.RED
                notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                notificationChannel.enableVibration(true)
                notificationManager.createNotificationChannel(notificationChannel)
            }


            val notificationBuilder = NotificationCompat.Builder(App.appContext, channelId)

            notificationBuilder.setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(jradi.rabie.dk.razzia_android.R.drawable.ic_launcher_foreground)
                    .setTicker("Hearty365")
                    //     .setPriority(Notification.PRIORITY_MAX)
                    .setContentTitle("Default notification")
                    .setContentText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
                    .setContentInfo("Info")

            notificationManager.notify(/*notification id*/1, notificationBuilder.build())
 */


/**
 * Class used for testing
 */
class MockedAlertNotifcationProvider : AlertNotificationProviderInterface {
    suspend override fun alertUser() {
        logPrint("Oh no! The smell of cheese is closing in!")
    }
}