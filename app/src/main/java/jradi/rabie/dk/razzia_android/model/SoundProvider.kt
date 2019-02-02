package jradi.rabie.dk.razzia_android.model

import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.media.RingtoneManager
import android.net.Uri
import android.support.v4.app.NotificationCompat
import jradi.rabie.dk.razzia_android.R
import jradi.rabie.dk.razzia_android.view.App
import jradi.rabie.dk.razzia_android.view.logPrint
import jradi.rabie.dk.razzia_android.view.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * @author rabie
 *
 *
 */

interface AlertProviderInterface {
    suspend fun alertUser()
}

/**
 * Class responsible for interacting with Android API so user can get a warning audio sample played
 */
class AndroidAlertProvider : AlertProviderInterface {
    suspend override fun alertUser() {

    }

}

class NotificationPresenter{

    private val notificationManager = App.appContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    suspend fun showCollisionServiceIsActive(){
        showNotification(title = "Collision Service Active", body = "Collision Service Active")
    }

    suspend fun showNotification(){
       showNotification(title = stringResource(R.string.app_name), body = stringResource(R.string.app_name))
    }

    suspend private fun showNotification(title : String, body: String){
        withContext(Dispatchers.Main) {
            var soundUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            if (soundUri == null) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (soundUri == null) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }
            }

            val notificationBuilder = NotificationCompat.Builder(App.appContext)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setSound(soundUri)

            //TODO this is just test code
            notificationManager.notify(1,notificationBuilder.build())
        }
    }
}

/**
 * Class used for testing
 */
class MockedAlertProvider : AlertProviderInterface {
    suspend override fun alertUser() {
        logPrint("Oh no! The smell of cheese is closing in!")
    }
}