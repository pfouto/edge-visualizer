package visualizer.utils

import visualizer.layout.TreeVertex
import java.net.Inet4Address
import java.text.SimpleDateFormat
import java.util.*

val dateFormat = SimpleDateFormat("yy/MM/dd HH:mm:ss,SSS")

abstract class TreeEvent(val timestamp: Date, val node: String) {
    override fun toString(): String {
        return "${dateFormat.format(timestamp)} $node"
    }
}

abstract class TreeViewEvent(timestamp: Date, node: String) : TreeEvent(timestamp, node)
abstract class HyParViewEvent(timestamp: Date, node: String) : TreeEvent(timestamp, node)

class HelloEvent(timestamp: Date, node: String, val addr: Inet4Address) : TreeEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Hello ${addr.hostAddress}"
    }
}

class GoodbyeEvent(timestamp: Date, node: String) : TreeEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Goodbye"
    }
}

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

class StateEvent(timestamp: Date, node: String, val state: TreeVertex.State) : TreeEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} $state"
    }
}

class ParentEvent(timestamp: Date, node: String, val parent: Inet4Address, val state: ParentState) :
    TreeViewEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Parent ${parent.hostAddress} is $state"
    }
}

class ChildEvent(timestamp: Date, node: String, val child: Inet4Address, val state: ChildState) :
    TreeViewEvent(timestamp, node) {
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
) : TreeEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Metadata: \n\t\t$children\n\t\t$parents)"
    }
}

enum class ChildState { SYNC, READY, DISCONNECTED }

enum class ParentState { CONNECTING, SYNC, READY, DISCONNECTED }