package visualizer.utils

import visualizer.layout.TreeVertex
import java.net.Inet4Address
import java.text.SimpleDateFormat
import java.util.*

val dateFormat = SimpleDateFormat("yy/MM/dd HH:mm:ss,SSS")

abstract class Event(val timestamp: Date, val node: String) {
    override fun toString(): String {
        return "${dateFormat.format(timestamp)} $node"
    }
}

class HelloEvent(timestamp: Date, node: String, val addr: Inet4Address) : Event(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Hello ${addr.hostAddress}"
    }
}

class GoodbyeEvent(timestamp: Date, node: String) : Event(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Goodbye"
    }
}

abstract class HyParViewEvent(timestamp: Date, node: String) : Event(timestamp, node)

class ActiveEvent(timestamp: Date, node: String, val peer: Inet4Address, val added: Boolean) :
    HyParViewEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Active ${peer.hostAddress} ${if (added) "added" else "removed"}"
    }
}

class PassiveEvent(timestamp: Date, node: String, val peer: Inet4Address, val added: Boolean) :
    HyParViewEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Passive ${peer.hostAddress} ${if (added) "added" else "removed"}"
    }
}

abstract class ManagerEvent(timestamp: Date, node: String) : Event(timestamp, node)

class ManagerStateEvent(timestamp: Date, node: String, val state: TreeVertex.State) : ManagerEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} $state"
    }
}

abstract class TreeEvent(timestamp: Date, node: String) : Event(timestamp, node)

class TreeStateEvent(timestamp: Date, node: String, val parent: Inet4Address, val state: ParentState) :
    TreeEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Parent ${parent.hostAddress} is $state"
    }
}

class ChildEvent(timestamp: Date, node: String, val child: Inet4Address, val state: ChildState) :
    TreeEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Child ${child.hostAddress} is $state"
    }
}

class MetadataEvent(
    timestamp: Date,
    node: String,
    val ts: String,
    val stableTs: String,
    val children: List<Pair<Inet4Address, String>>,
    val parents: List<Pair<Inet4Address, String>>,
) : Event(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Metadata: \n\t\t$children\n\t\t$parents)"
    }
}

enum class ChildState { SYNC, READY, DISCONNECTED }

enum class ParentState { CONNECTING, SYNC, READY, DISCONNECTED }