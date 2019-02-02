package jradi.rabie.dk.razzia_android.view

import android.app.Application
import android.content.Context

/**
 * @author rabie
 *
 *
 */
class App : Application(){

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
    }
}


/**
 * Convenience method to get string from XML
 */
fun stringResource(id: Int, vararg formatArgs: Any): String {
    return App.appContext.getString(id, *formatArgs)
}
