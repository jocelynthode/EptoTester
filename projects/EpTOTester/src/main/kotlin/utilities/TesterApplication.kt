package utilities


import epto.Application
import epto.Event
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of an Application to test EpTO
 *
 * @property expectedEvents the number of events we expect to deliver
 * @see Application
 *
 * @author Jocelyn Thode
 */
class TesterApplication(ttl: Int, k: Int, trackerURL: String, val peerNumber: Int,
                        delta: Long, myIp: InetAddress, gossipPort: Int, pssPort: Int) :
        Application(ttl, k, trackerURL, delta, myIp, gossipPort, pssPort) {

    var deliveredEvents = 0
    private val sendTimes = ConcurrentHashMap<UUID, Long>()

    /**
     * Delivers the event to STDOUT
     *
     * {@inheritDoc}
     */
    override fun deliver(event: Event) {
        deliveredEvents++
        if(sendTimes.containsKey(event.id)) {
            val sendTime = sendTimes[event.id] as Long
            val delta = System.nanoTime() - sendTime
            logger.info("Delivered: ${event.toIdentifier()} -- Local Delta: $delta")
        } else {
            logger.info("Delivered: ${event.toIdentifier()}")
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun broadcast(event: Event) {
        super.broadcast(event)
        logger.info("Sending: ${event.toIdentifier()}")
        sendTimes[event.id] = System.nanoTime()
    }

    /**
     * {@inheritDoc}
     */
    override fun start() {
        Thread(peer).start()
        logger.info("Started: ${myIp.hostAddress}")
        logger.info("Peer ID: ${peer.uuid}")
        logger.info("Peer Number: $peerNumber")
        logger.info("TTL: ${peer.oracle.TTL}, K: ${peer.disseminationComponent.K}")
        logger.info("Delta: ${peer.disseminationComponent.delta}")
    }

    /**
     * {@inheritDoc}
     */
    override fun stop() {
        peer.stop()
        logger.info("Quitting EpTO tester")
        logger.info("Total Balls sent: ${peer.core.gossipMessagesSent}")
        logger.info("Total Balls received: ${peer.core.gossipMessagesReceived}")
        logger.info("PSS messages sent: ${peer.core.pssMessagesSent}")
        logger.info("PSS messages received: ${peer.core.pssMessagesReceived}")
        logger.info("Events sent: ${Main.Companion.eventsSent}")
        logger.info("Events received: $deliveredEvents")
        logger.info("Events received but not delivered: ${peer.orderingComponent.received.size}")
        logger.debug("Received events: {}", peer.orderingComponent.received.keys.joinToString())
    }
}
