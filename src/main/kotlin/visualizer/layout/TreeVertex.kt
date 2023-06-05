package visualizer.layout

import org.jungrapht.visualization.VisualizationViewer
import java.awt.Color
import java.net.Inet4Address
import java.text.DecimalFormat

class TreeVertex(val node: String, val addr: Inet4Address, val location: Pair<Double, Double>) {

    enum class ManagerState { ACTIVE, INACTIVE }
    enum class TreeState { INACTIVE, DATACENTER, PARENT_CONNECTING, PARENT_CONNECTED, PARENT_SYNC, PARENT_READY }

    var alive = true

    var managerState = ManagerState.INACTIVE
    var treeState = TreeState.INACTIVE

    var ts = ""
    var stableTs = ""


    var parent: TreeVertex? = null
    var grandparents: MutableList<TreeVertex> = mutableListOf()
    var parentMetadata: List<String> = emptyList()

    var children: MutableMap<TreeVertex, String> = mutableMapOf()

    var active: MutableList<TreeVertex> = mutableListOf()
    var passive: MutableList<TreeVertex> = mutableListOf()

    fun panelText(): String {
        val sb = StringBuilder()
        sb.append("$node\n")
        sb.append("  ManagerState: $managerState\n")
        sb.append("  TreeState: $treeState\n")
        sb.append("  Location: \n")
        sb.append("    ${location.first}\n")
        sb.append("    ${location.second}\n")
        sb.append("\n")
        sb.append("  Parents (${1+grandparents.size}):\n")
        if (parent != null) {
            sb.append("    ${parent!!.node}")
            if (parentMetadata.isNotEmpty())
                sb.append(": ${parentMetadata[0]}\n")
            else
                sb.append("\n")
        }

        grandparents.forEachIndexed  { index, it ->
            sb.append("    ${it.node}")
            if (parentMetadata.size > index + 1)
                sb.append(": ${parentMetadata[index + 1]}\n")
            else
                sb.append("\n")
        }
        sb.append("\n")

        sb.append("  Children: ${children.size}\n")
        children.forEach { sb.append("    ${it.key.node} ${it.value}\n") }

        sb.append("\n")
        sb.append("  Active: ${active.size}\n")
        active.forEach { sb.append("    ${it.node}\n") }
        sb.append("  Passive: ${passive.size}\n")
        passive.forEach { sb.append("    ${it.node}\n") }

        return sb.toString()
    }

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
        val selected = vv.selectedVertices.contains(this)
        return if (!alive && !selected ) Color.BLACK
        else if (!alive && selected) Color.DARK_GRAY.brighter()
        else if (selected) Color.RED
        else when (treeState){
            TreeState.INACTIVE -> Color.GRAY
            TreeState.DATACENTER -> Color.BLUE
            TreeState.PARENT_CONNECTING -> Color.GREEN.brighter()
            TreeState.PARENT_CONNECTED -> Color.GREEN
            TreeState.PARENT_SYNC -> Color.GREEN.darker()
            TreeState.PARENT_READY -> Color.GREEN.darker().darker()
        }
    }

    override fun toString(): String {
        return "TreeVertex(node='$node', addr=$addr)"
    }


}