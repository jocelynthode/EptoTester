package epto.utilities

import java.io.Serializable
import java.util.*

/**
 * Implementation of the events. This class implements the structure of an event as described in EpTO.
 */
data class Event(val id: UUID = UUID.randomUUID()) : Comparable<Event>, Serializable {

    var timeStamp: Long = 0
    var ttl: Int = 0
    var sourceId: UUID? = null

    /**
     * Initializes an event

     * @param id        The id of the event
     * *
     * @param timeStamp the time stamp of the event
     * *
     * @param ttl       the time to live of the vent
     * *
     * @param sourceId  the id of the peer sending this event
     */
    constructor(id: UUID, timeStamp: Long, ttl: Int, sourceId: UUID) : this(id) {
        this.timeStamp = timeStamp
        this.ttl = ttl
        this.sourceId = sourceId
    }

    /**
     * Increments the event's ttl
     */
    @Synchronized fun incrementTtl() {
        this.ttl++
    }

    /**
     * compareTo method for Event class

     * @param other a non-null Event
     * *
     * @return int (1 if before, -1 if after)
     */
    override fun compareTo(other: Event): Int {
        val compare = timeStamp.compareTo(other.timeStamp)

        return if (compare == 0) {
            //in case of tie
            sourceId?.compareTo(other.sourceId) ?: 0
        } else {
            compare
        }
    }

    /**
     * @return A string representing the Event object
     */
    override fun toString(): String {
        return "Event{id=$id, timeStamp=$timeStamp, ttl=$ttl, sourceId=$sourceId}"
    }

}