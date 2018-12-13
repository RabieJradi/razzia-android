package jradi.rabie.dk.razzia_android.model

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author rabie
 *
 *
 */

interface SoundProviderInterface {
    suspend fun makeSound()
}

/**
 * Class responsible for interacting with Android API so user can get a warning audio sample played
 */
class AndroidSoundProvider : SoundProviderInterface {
    suspend override fun makeSound() {
        withContext(Dispatchers.Main){
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

}

/**
 * Class used for testing
 */
class MockedSoundProvider : SoundProviderInterface {
    suspend override fun makeSound() {
        Log.d(javaClass.simpleName, "Oh no! The smell of cheese is closing in!")
    }
}