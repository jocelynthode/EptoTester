package epto


import epto.utilities.Event
import net.sf.neem.impl.Application
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import java.util.*

/**
 * Implementation of the Ordering Component.
 * The main task of this procedure is to move events from
 * the received set to the delivered set, preserving the total
 * order of the events.
 */
class OrderingComponent(private val oracle: StabilityOracle, internal var app: Application) {
    val received = HashMap<UUID, Event>()
    private val delivered = HashMap<UUID, Event>()
    private var lastDeliveredTs: Long = 0

    /**
     * Update the received hash map TTL values and either add the new events to received or
     * update their ttl

     * @param ball the received ball
     */
    private fun updateReceived(ball: HashMap<UUID, Event>) {
        // update TTL of received events
        received.values.forEach { it.incrementTtl() }

        // update set of received events with events in the ball
        ball.values.filter { event -> !delivered.containsKey(event.id) && event.timeStamp >= lastDeliveredTs }
                .forEach { event ->
                    val receivedEvent = received[event.id]
                    if (receivedEvent != null) {
                        if (receivedEvent.ttl < event.ttl) {
                            receivedEvent.ttl = event.ttl
                        }
                    } else {
                        received.put(event.id, event)
                    }
                }
    }

    /**
     * Deliver events mature enough that haven't been yet delivered to the application

     * @param deliverableEvents events mature enough to be delivered
     */
    private fun deliver(deliverableEvents: List<Event>) {
        for (event in deliverableEvents) {
            delivered.put(event.id, event)

            lastDeliveredTs = event.timeStamp

            val byteOut = ByteArrayOutputStream()
            try {
                val out = ObjectOutputStream(byteOut)
                out.writeObject(event)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            //delivering the event
            val bb = ByteBuffer.wrap(byteOut.toByteArray())
            app.deliver(arrayOf(bb))
        }
    }


    /**
     * this is the main function, OrderEvents procedure. Dissemination component will invoke this method periodically.

     * @param ball the ball containing the received events
     */
    fun orderEvents(ball: HashMap<UUID, Event>) {

        updateReceived(ball)

        // collect deliverable events and determine smallest
        // timestamp of non deliverable events
        var minQueuedTs = Long.MAX_VALUE
        val deliverableEvents = ArrayList<Event>()

        for (event in received.values) {
            if (oracle.isDeliverable(event)) {
                deliverableEvents.add(event)
            } else if (minQueuedTs > event.timeStamp) {
                minQueuedTs = event.timeStamp
            }
        }

        val eventsToRemove = ArrayList<Event>()

        for (event in deliverableEvents) {
            if (event.timeStamp >= minQueuedTs) {
                // ignore deliverable events with timestamp greater or equal than all non-deliverable events
                eventsToRemove.add(event)
            } else {
                // event can be delivered, remove from received events
                received.remove(event.id)
            }
        }
        deliverableEvents.removeAll(eventsToRemove)

        //sort deliverable Events by Ts and ID, ascending
        deliverableEvents.sort(null)

        deliver(deliverableEvents)
    }
}