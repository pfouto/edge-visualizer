package visualizer.layout

import org.jungrapht.visualization.VisualizationViewer
import java.awt.Color

class TreeEdge(val origin: TreeVertex, val destiny: TreeVertex, var type: Type) {

    enum class Type {CONNECTING_PARENT, SYNC_PARENT, READY_PARENT, SYNC_CHILD, READY_CHILD }

    var label = ""

    fun paintMe(vv: VisualizationViewer<TreeVertex, TreeEdge>): Color {
        return when (type){
            Type.CONNECTING_PARENT -> Color.CYAN
            Type.SYNC_PARENT -> Color.BLUE.brighter()
            Type.READY_PARENT -> Color.BLUE.darker()
            Type.SYNC_CHILD -> Color.GREEN.brighter()
            Type.READY_CHILD -> Color.GREEN.darker()
        }
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