package jradi.rabie.dk.razzia_android.model.api

import jradi.rabie.dk.razzia_android.model.entities.RazziaEntry
import kotlinx.coroutines.Deferred
import retrofit2.http.GET

/**
 * @author rabie
 *
 *
 */

interface HTTPServiceInterface {

    @GET("/v1/")
    fun getEntries(): Deferred<List<RazziaEntry>>
}