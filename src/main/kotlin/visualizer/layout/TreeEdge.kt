package visualizer.layout

import org.jungrapht.visualization.VisualizationViewer
import java.awt.Color

class TreeEdge(val origin: TreeVertex, val destiny: TreeVertex, var type: Type) {

    companion object {
        fun treeStateToEdgeType(treeState: TreeVertex.TreeState): Type {
            return when (treeState) {
                TreeVertex.TreeState.PARENT_CONNECTING -> Type.CONNECTING_PARENT
                TreeVertex.TreeState.PARENT_SYNC -> Type.SYNC_PARENT
                TreeVertex.TreeState.PARENT_READY -> Type.READY_PARENT
                else -> throw IllegalArgumentException("Invalid tree state $treeState")
            }
        }
    }

    enum class Type { CONNECTING_PARENT, CONNECTED_PARENT, SYNC_PARENT, READY_PARENT,
        CONNECTED_CHILD, SYNC_CHILD, READY_CHILD, VIEW_ACTIVE, VIEW_PASSIVE }

    fun paintMe(vv: VisualizationViewer<TreeVertex, TreeEdge>): Color {
        return when (type) {
            Type.CONNECTING_PARENT -> Color.CYAN
            Type.SYNC_PARENT -> Color.BLUE.brighter()
            Type.CONNECTED_PARENT -> Color.BLUE
            Type.READY_PARENT -> Color.BLUE.darker()

            Type.CONNECTED_CHILD -> Color.GREEN.brighter()
            Type.SYNC_CHILD -> Color.GREEN
            Type.READY_CHILD -> Color.GREEN.darker()

            Type.VIEW_PASSIVE -> Color.YELLOW.darker()
            Type.VIEW_ACTIVE -> Color.ORANGE.darker()
        }
    }

    fun includeMe(vv: VisualizationViewer<TreeVertex, TreeEdge>): Boolean {
        return if(vv.selectedVertices.contains(origin)){
            true
        } else {
            this.type != Type.VIEW_ACTIVE && this.type != Type.VIEW_PASSIVE
        }
    }

    fun labelMe(vv: VisualizationViewer<TreeVertex, TreeEdge>): String {
        var ret = ""
        if (type == Type.READY_CHILD && vv.selectedVertices.contains(origin) && origin.children[destiny] != null)
            ret = origin.children[destiny]!!
        else if (type == Type.READY_PARENT) {
            /*vv.selectedVertices.forEach {
                if (it.parents[destiny] != null && (origin == it || it.parents[origin] != null))
                    ret += " "+it.parents[destiny]!!
            }*/
        }
        return ret
    }

    override fun toString(): String {
        return "TreeEdge($origin->$destiny $type)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TreeEdge

        if (origin != other.origin) return false
        if (destiny != other.destiny) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = origin.hashCode()
        result = 31 * result + destiny.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }


}