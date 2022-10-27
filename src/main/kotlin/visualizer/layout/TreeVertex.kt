package visualizer.layout

import org.jungrapht.visualization.VisualizationViewer
import java.awt.Color
import java.net.Inet4Address

class TreeVertex(val node: String, val addr: Inet4Address) {

    enum class State { DORMANT, LEAF, ROOT }

    var alive = true
    var state = State.DORMANT

    var ts = ""
    var stableTs = ""
    var children: List<Pair<Inet4Address, String>> = listOf()
    var parents: List<Pair<Inet4Address, String>> = listOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TreeVertex

        if (node != other.node) return false

        return true
    }

    override fun hashCode(): Int {
        return node.hashCode()
    }

    fun paintMe(vv: VisualizationViewer<TreeVertex, TreeEdge>): Color {
        return if (!alive) Color.BLACK
        else if (vv.selectedVertices.contains(this)) Color.RED
        else when (state){
            State.DORMANT -> Color.GRAY
            State.LEAF -> Color.GREEN
            State.ROOT -> Color.BLUE
        }
    }

    override fun toString(): String {
        return "TreeVertex(node='$node', addr=$addr)"
    }


}