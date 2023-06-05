package visualizer.utils

import visualizer.layout.TreeVertex
import java.net.Inet4Address
import java.text.SimpleDateFormat
import java.util.*

val dateFormat = SimpleDateFormat("yy/MM/dd HH:mm:ss,SSS")
val dateFormatNoDate = SimpleDateFormat("HH:mm:ss,SSS")

abstract class Event(val timestamp: Date, val node: String, var index: Int = -1) {
    override fun toString(): String {
        return "${dateFormatNoDate.format(timestamp)} $node"
    }
}

class HelloEvent(timestamp: Date, node: String, val addr: Inet4Address, val location: Pair<Double, Double>) : Event(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Hello ${addr.hostAddress} ${location.first} ${location.second}"
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

class ManagerStateEvent(timestamp: Date, node: String, val state: TreeVertex.ManagerState) :
    ManagerEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} MANAGER $state"
    }
}

abstract class TreeEvent(timestamp: Date, node: String) : Event(timestamp, node)

class TreeStateEvent(
    timestamp: Date, node: String, val parent: Inet4Address?, val state: TreeVertex.TreeState,
    val grandparents: List<Inet4Address>,
) :
    TreeEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} TREE $state ${parent?.hostAddress} $grandparents"
    }
}


enum class ChildState {CONNECTED, SYNC, READY, DISCONNECTED }

class ChildEvent(timestamp: Date, node: String, val child: Inet4Address, val state: ChildState) :
    TreeEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} CHILD ${child.hostAddress} $state"
    }
}

abstract class MetadataEvent(timestamp: Date, node: String) : Event(timestamp, node)

class ParentMetadata(timestamp: Date, node: String, val metadata: List<String>) :
    MetadataEvent(timestamp, node) {

    override fun toString(): String {
        return "${super.toString()} P-META $metadata"
    }
}

class ChildMetadata(timestamp: Date, node: String, val child: Inet4Address, val metadata: String) :
    MetadataEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} C-META ${child.hostAddress} $metadata"
    }
}
