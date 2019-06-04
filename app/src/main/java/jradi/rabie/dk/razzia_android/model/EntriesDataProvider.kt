package jradi.rabie.dk.razzia_android.model

import jradi.rabie.dk.razzia_android.model.api.HTTPService
import jradi.rabie.dk.razzia_android.model.entities.*

/**
 * @author rabie
 *
 * This class is responsible for providing RazziaEntry objects via the API
 */

interface EntriesDataProviderInterface {
    suspend fun getEntries(): List<RazziaEntry>
    suspend fun addEntry(razziaEntry: RazziaEntry)
}


class MockedEntriesDataProvider : EntriesDataProviderInterface {
    val entries = arrayListOf<RazziaEntry>().apply {
        add(RazziaEntry(id = Id("ID1"), creationDate = Timestamp(System.currentTimeMillis()), description = "Ost på hjørnet", location = GPSLocation(latitude = 55.712885, longitude = 12.559324), timeToLive = Hour(2), radiusInMeters = 10))
        add(RazziaEntry(id = Id("ID2"), creationDate = Timestamp(System.currentTimeMillis()), description = "Ost på hjørnet 2", location = GPSLocation(latitude = 55.711060, longitude = 12.554067), timeToLive = Hour(2), radiusInMeters = 10))
        add(RazziaEntry(id = Id("ID3"), creationDate = Timestamp(System.currentTimeMillis()), description = "Ost på hjørnet 3", location = GPSLocation(latitude = 55.709621, longitude = 12.559699), timeToLive = Hour(2), radiusInMeters = 10))
        add(RazziaEntry(id = Id("ID4"), creationDate = Timestamp(System.currentTimeMillis()), description = "For enden af guldbergsgade", location = GPSLocation(latitude = 55.692640, longitude = 12.556332), timeToLive = Hour(2), radiusInMeters = 10))
    }

    override suspend fun getEntries(): List<RazziaEntry> {
        synchronized(entries) {
            return ArrayList<RazziaEntry>().apply { addAll(entries) }
        }
    }

    override suspend fun addEntry(razziaEntry: RazziaEntry) {
        synchronized(entries) {
            entries.add(razziaEntry)
        }
    }

}

class EntriesDataProvider : EntriesDataProviderInterface {

    override suspend fun getEntries(): List<RazziaEntry> {
        return HTTPService.service.getEntries().await()
    }

    override suspend fun addEntry(razziaEntry: RazziaEntry) {
        //TODO post the data
    }
}