package jradi.rabie.dk.razzia_android.model.entities

/**
 * @author rabie
 *
 *
 */
data class RazziaEntry(val id: Id,
                       val creationDate: Timestamp,
                       val description: String,
                       val location: GPSLocation,
                       val radiusInMeters: Int,
                       val timeToLive: Hour)

data class Id(val value: String)

/**
 * UTC milliseconds from the epoch
 */
data class Timestamp(val value: Long)

data class GPSLocation(val latitude: Double,
                       val longitude: Double)


data class Hour(val value: Int)
