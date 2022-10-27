package visualizer.utils

import visualizer.layout.TreeVertex
import java.net.Inet4Address
import java.util.*

abstract class TreeEvent(val timestamp: Date, val node: String) {
    override fun toString(): String {
        return "TreeEvent($timestamp: $node"
    }
}

abstract class ViewEvent(timestamp: Date, node: String) : TreeEvent(timestamp, node)

class HelloEvent(timestamp: Date, node: String, val addr: Inet4Address) : TreeEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Hello ${addr.hostAddress})"
    }
}

class GoodbyeEvent(timestamp: Date, node: String) : TreeEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Goodbye)"
    }
}

class StateEvent(timestamp: Date, node: String, val state: TreeVertex.State) : TreeEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} $state)"
    }
}

class ParentEvent(timestamp: Date, node: String, val parent: Inet4Address, val state: ParentState) :
    ViewEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Parent ${parent.hostAddress} is $state)"
    }
}

class ChildEvent(timestamp: Date, node: String, val child: Inet4Address, val state: ChildState) :
    ViewEvent(timestamp, node) {
    override fun toString(): String {
        return "${super.toString()} Child ${child.hostAddress} is $state)"
    }
}

class MetadataEvent(
    timestamp: Date,
    node: String,
    val ts: String,
    val stableTs: String,
    val children: List<Pair<Inet4Address, String>>,
    val parents: List<Pair<Inet4Address, String>>,
) : TreeEvent(timestamp, node){
    override fun toString(): String {
        return "${super.toString()} Metadata $children $parents)"
    }
}

enum class ChildState { SYNC, READY, DISCONNECTED }

enum class ParentState { CONNECTING, SYNC, READY, DISCONNECTED }