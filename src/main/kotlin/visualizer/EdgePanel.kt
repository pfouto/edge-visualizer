package visualizer

import org.jgrapht.Graph
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.DirectedMultigraph
import org.jgrapht.graph.SimpleDirectedGraph
import org.jungrapht.visualization.VisualizationScrollPane
import org.jungrapht.visualization.VisualizationViewer
import org.jungrapht.visualization.control.DefaultGraphMouse
import org.jungrapht.visualization.control.GraphMousePlugin
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm
import org.jungrapht.visualization.layout.algorithms.RadialTreeLayoutAlgorithm
import org.jungrapht.visualization.layout.algorithms.StaticLayoutAlgorithm
import org.jungrapht.visualization.layout.algorithms.TreeLayoutAlgorithm
import org.jungrapht.visualization.layout.algorithms.util.InitialDimensionFunction
import org.jungrapht.visualization.layout.model.LayoutModel
import org.jungrapht.visualization.layout.model.Rectangle
import org.jungrapht.visualization.renderers.Renderer
import org.jungrapht.visualization.util.LayoutAlgorithmTransition
import visualizer.layout.TreeEdge
import visualizer.layout.TreeVertex
import visualizer.utils.*
import visualizer.utils.ActiveEvent
import visualizer.utils.Event
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.geom.Ellipse2D
import java.net.Inet4Address
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import java.util.function.Function
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ListSelectionEvent


class EdgePanel(private val allEvents: List<Event>, maxIntervalIdx: Int) : JPanel() {

    private val dateFormat = SimpleDateFormat("HH:mm:ss,SSS")

    private val fullGraph: Graph<TreeVertex, TreeEdge>
    private var currentEvent: Int = -1

    private val vv: VisualizationViewer<TreeVertex, TreeEdge>

    private val staticLayoutAlgorithm: StaticLayoutAlgorithm<TreeVertex>
    //private val treeLayoutAlgorithm: LayoutAlgorithm<TreeVertex>
    //private val radialLayoutAlgorithm: LayoutAlgorithm<TreeVertex>

    private val sliderLabelCurrentTime: JLabel
    private val sliderLabelCurrentLine: JLabel
    private val timeSlider: JSlider
    private val startTimeMillis: Long
    private val endTimeMillis: Long
    private var internalChanging = false

    private val eventsList: JList<Event>

    private val filteredEvents: List<Event>

    private val infoPanelText: JTextArea

    private var lockList: Boolean = false
    init {

        fullGraph = DirectedMultigraph(TreeEdge::class.java)

        layout = BorderLayout()

        val width = 35
        val height = 35
        val vertexShapeFunction = Function<TreeVertex, Shape> {
            Ellipse2D.Float(-width / 2f, -height / 2f, width.toFloat(), height.toFloat())
        }
        val boundWidth = 60.0
        val boundHeight = 60.0
        val vertexBoundsFunction = Rectangle.of(-boundWidth / 2, -boundHeight / 2, boundWidth, boundHeight)
        staticLayoutAlgorithm = StaticLayoutAlgorithm()
        //staticLayoutAlgorithm.visit()


        val graphMouse = DefaultGraphMouse<TreeVertex, TreeEdge>()
/*
        treeLayoutAlgorithm = TreeLayoutAlgorithm()
        treeLayoutAlgorithm.setVertexBoundsFunction { vertexBoundsFunction }
        radialLayoutAlgorithm = RadialTreeLayoutAlgorithm()
        radialLayoutAlgorithm.setVertexBoundsFunction { vertexBoundsFunction }
*/

        //VisViewer builder
        val builder: VisualizationViewer.Builder<TreeVertex, TreeEdge, *, *> = VisualizationViewer.builder(fullGraph)
        builder.initialDimensionFunction(InitialDimensionFunction())
        builder.viewSize(Dimension(1500, 1000))
        builder.graphMouse(graphMouse)
        builder.layoutAlgorithm(staticLayoutAlgorithm)

        vv = builder.build() as VisualizationViewer<TreeVertex, TreeEdge>

        //How to draw vertexes
        vv.renderContext.vertexShapeFunction = vertexShapeFunction
        vv.renderContext.setVertexLabelFunction { v: TreeVertex -> v.node.split("-")[1] }
        vv.renderContext.vertexLabelPosition = Renderer.VertexLabel.Position.CNTR
        vv.renderContext.setVertexLabelDrawPaintFunction { Color.white }
        vv.renderContext.setVertexFontFunction { Font("Helvetica", Font.BOLD, 12) }
        //vv.renderContext.setVertexDrawPaintFunction { Color.YELLOW }
        vv.renderContext.setVertexFillPaintFunction { v -> v.paintMe(vv) }
        vv.renderContext.setEdgeIncludePredicate { e -> e.includeMe(vv) }
        vv.renderContext.setEdgeDrawPaintFunction { e -> e.paintMe(vv) }
        vv.renderContext.setArrowDrawPaintFunction { e -> e.paintMe(vv) }
        vv.renderContext.setArrowFillPaintFunction { e -> e.paintMe(vv) }
        vv.renderContext.edgeWidth = 3f
        vv.renderContext.edgeArrowWidth = 15

        vv.renderContext.edgeLabelCloseness = 0.5f
        vv.renderContext.edgeLabelRenderer
        vv.renderContext.setEdgeLabelFunction { it.labelMe(vv) }
        vv.renderContext.setEdgeFontFunction { Font("Helvetica", Font.ITALIC, 12) }

        vv.setVertexToolTipFunction { v -> "${v.ts}\n${v.stableTs}" }

        val panel = VisualizationScrollPane(vv)
        add(panel)


        // STEPS
        val prev = JButton("<")
        prev.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent - 1, true, true) }
        val prev10 = JButton("<<")
        prev10.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent - 10, true, true) }
        val prev100 = JButton("<<<")
        prev100.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent - 100, true, true) }
        val next = JButton(">")
        next.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent + 1, true, true) }
        val next10 = JButton(">>")
        next10.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent + 10, true, true) }
        val next100 = JButton(">>>")
        next100.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent + 100, true, true) }
        val stepGrid = JPanel(GridLayout(1, 0))
        stepGrid.border = BorderFactory.createTitledBorder("Steps (1, 10 or 100)")

        //CONTROLS
        val controls = JPanel()
        stepGrid.add(prev100)
        stepGrid.add(prev10)
        stepGrid.add(prev)
        stepGrid.add(next)
        stepGrid.add(next10)
        stepGrid.add(next100)
        controls.add(stepGrid)
        add(controls, BorderLayout.SOUTH)

        // SLIDER

        //********** TOP BOX  **********//

        // SLIDER
        val sliderPanel = JPanel(BorderLayout(10, 3))
        sliderPanel.border = EmptyBorder(2, 10, 0, 10)

        sliderLabelCurrentTime = JLabel("Time")
        sliderLabelCurrentLine = JLabel("-1")
        val firstDate = allEvents[0].timestamp
        val lastDate = allEvents.last().timestamp
        startTimeMillis = firstDate.time
        endTimeMillis = lastDate.time

        val sliderLabelMin = JLabel(dateFormat.format(firstDate))
        val sliderLabelMax = JLabel(dateFormat.format(lastDate))

        timeSlider = JSlider(JSlider.HORIZONTAL, 0, (endTimeMillis - startTimeMillis).toInt(), 0)
        timeSlider.addChangeListener { e: ChangeEvent ->
            val source = e.source as JSlider
            val date = Date(source.value + startTimeMillis)
            sliderLabelCurrentTime.text = dateFormat.format(date) +
                    " (+" + Duration.between(firstDate.toInstant(), date.toInstant()).toMillis()/1000f + "s)"
            if (!internalChanging && !source.valueIsAdjusting) jumpToTime(source.value)
        }
        timeSlider.majorTickSpacing = 60000
        timeSlider.minorTickSpacing = 10000
        timeSlider.paintTicks = true

        sliderPanel.add(timeSlider)
        sliderPanel.add(sliderLabelMin, BorderLayout.WEST)
        sliderPanel.add(sliderLabelMax, BorderLayout.EAST)
        val timePanel = JPanel()
        timePanel.add(sliderLabelCurrentTime)
        timePanel.add(sliderLabelCurrentLine)
        sliderPanel.add(timePanel, BorderLayout.SOUTH)
        add(sliderPanel, BorderLayout.NORTH)

        // EVENT MARKERS

        // EVENT MARKERS
        val markers = Hashtable<Int, JLabel>()
        val eventLabelHello = JLabel("|")
        eventLabelHello.foreground = Color.GREEN.darker()
        eventLabelHello.font = Font("Arial", Font.PLAIN, 16)
        val eventLabelGoodbye = JLabel("|")
        eventLabelGoodbye.foreground = Color.RED.darker()
        eventLabelGoodbye.font = Font("Arial", Font.PLAIN, 16)
        val eventLabelView = JLabel("|")
        eventLabelView.foreground = Color(0.5f, 0.5f, 0.5f, 0.2f)
        eventLabelView.font = Font("Arial", Font.PLAIN, 16)
        val eventLabelState = JLabel("|")
        eventLabelState.foreground = Color.BLUE.darker()
        eventLabelState.font = Font("Arial", Font.PLAIN, 16)

        for (event in allEvents) {
            when (event) {
                is HelloEvent -> markers[(event.timestamp.time - startTimeMillis).toInt()] = eventLabelHello
                is ManagerStateEvent -> markers[(event.timestamp.time - startTimeMillis).toInt()] = eventLabelState
                is GoodbyeEvent -> markers[(event.timestamp.time - startTimeMillis).toInt()] = eventLabelGoodbye
                is TreeEvent -> markers[(event.timestamp.time - startTimeMillis).toInt()] = eventLabelView
            }
        }
        timeSlider.labelTable = markers
        timeSlider.paintLabels = true

        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        val infoPanelLabel = JLabel("Picked Vertex Info:")
        infoPanelText = JTextArea()
        infoPanelText.isEditable = false
        infoPanelText.lineWrap = true
        infoPanel.add(infoPanelLabel)
        infoPanel.add(infoPanelText)
        add(infoPanel, BorderLayout.EAST)

        val eventsPanel = JPanel()
        eventsPanel.layout = BoxLayout(eventsPanel, BoxLayout.Y_AXIS)
        val eventsPanelLabel = JLabel("All events:")

        filteredEvents = allEvents.filter { it !is MetadataEvent && it !is ActiveEvent }

        eventsList = JList(filteredEvents.toTypedArray())

        eventsList.addListSelectionListener { e: ListSelectionEvent ->
            if(!lockList) {
                val source = e.source as JList<*>
                jumpToEvent(filteredEvents[source.selectedIndex].index, true, false)
            }
        }

        val scrollPanel = JScrollPane(eventsList)
        scrollPanel.preferredSize = Dimension(500, 0)
        scrollPanel.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS;
        scrollPanel.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS;
        eventsPanel.add(eventsPanelLabel)
        eventsPanel.add(scrollPanel)
        add(eventsPanel, BorderLayout.WEST)


        graphMouse.add(object : MouseListener, GraphMousePlugin {
            override fun mouseClicked(e: MouseEvent?) {}
            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {
                reloadInfoPanel()
            }

            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })

        jumpToEvent(maxIntervalIdx, true, true)
    }

    private fun reloadInfoPanel(){
        println("reloadInfoPanel")
        infoPanelText.text = ""
        vv.selectedVertices.forEach {
            infoPanelText.text += it.panelText() + "\n"
        }
    }

    private fun jumpToTime(ts: Int) {
        println("jumpToTime $ts")
        val targetTime = ts + startTimeMillis
        var target = -1
        while (target < allEvents.size - 1 && allEvents[target + 1].timestamp.time <= targetTime) {
            target++
        }
        jumpToEvent(target, false, true)
    }


    private fun jumpToEvent(_targetEvent: Int, updateSlider: Boolean, updateList: Boolean) {
        SwingUtilities.invokeLater {
            var targetEvent = _targetEvent
            if (targetEvent > allEvents.size - 1) targetEvent = allEvents.size - 1
            if (targetEvent < 0) targetEvent = 0

            if (targetEvent == currentEvent) return@invokeLater

            if (targetEvent < currentEvent) {
                resetGraph()
                currentEvent = -1
            }

            println("Moving " + (targetEvent - currentEvent) + " events to event $targetEvent")
            for (i in currentEvent + 1..targetEvent) {
                processEvent(allEvents[i])
            }

            currentEvent = targetEvent
            sliderLabelCurrentLine.text = "$currentEvent/${allEvents.size - 1}"
            if (updateSlider) {
                internalChanging = true
                timeSlider.value = ((allEvents[currentEvent].timestamp.time - startTimeMillis).toInt())
                internalChanging = false
            }

            if(updateList){
                //Find the event in filteredList with the index closest to currentEvent
                var closest = 0
                for (i in filteredEvents.indices) {
                    if ((filteredEvents[i].index <= currentEvent) && (i > closest)) {
                        closest = i
                    }
                }

                lockList = true
                eventsList.selectedIndex = closest
                eventsList.ensureIndexIsVisible(closest)
                lockList = false
            }

            reloadInfoPanel()
            redraw()
        }
    }

    private fun redraw() {
/*        val treeGraph = AsSubgraph(
            fullGraph, fullGraph.vertexSet(), fullGraph.edgeSet()
                .filter { it.type == TreeEdge.Type.READY_CHILD }.toSet()
        )
        val treeLayoutModel = LayoutModel.builder<TreeVertex>()
            .graph(treeGraph)
            .size(500, 500)
            .build() as LayoutModel<TreeVertex>


        treeLayoutAlgorithm.visit(treeLayoutModel)
        radialLayoutAlgorithm.visit(treeLayoutModel)
        for (location in treeLayoutModel.locations) {
            vv.visualizationModel.layoutModel.set(location.key, location.value)
        }
 */
        /*val newAlgorithm = StaticLayoutAlgorithm<TreeVertex>()
        LayoutAlgorithmTransition.animate(vv, newAlgorithm)
        vv.visualizationModel.layoutModel.accept(newAlgorithm)*/
        //vv.scaleToLayout(true)
        vv.fireStateChanged()
        vv.repaint()
    }

    private val vertexByName: MutableMap<String, TreeVertex> = mutableMapOf()
    private val vertexByAddr: MutableMap<Inet4Address, TreeVertex> = mutableMapOf()
    private fun processEvent(event: Event) {
        //println(event)
        when (event) {
            is HelloEvent -> {
                val vertex = TreeVertex(event.node, event.addr, event.location)
                vertexByName[event.node] = vertex
                vertexByAddr[event.addr] = vertex
                vv.visualizationModel.layoutModel.set(vertex, vertex.location.first*3, vertex.location.second*3)
                fullGraph.addVertex(vertex)
            }

            is GoodbyeEvent -> {
                val vertex = vertexByName[event.node]!!
                vertex.alive = false
                fullGraph.outgoingEdgesOf(vertex).toSet().forEach {
                    fullGraph.removeEdge(it)
                    checkIfCanDelete(it.destiny)
                }
                checkIfCanDelete(vertex)
            }

            is ActiveEvent -> {
                val vertex = vertexByName[event.node]!!
                val peer = vertexByAddr[event.peer]!!
                if (event.added) {
                    fullGraph.addEdge(vertex, peer, TreeEdge(vertex, peer, TreeEdge.Type.VIEW_ACTIVE))
                    vertex.active.add(peer)
                } else {
                    vertex.active.remove(peer)
                    if (!fullGraph.removeEdge(TreeEdge(vertex, peer, TreeEdge.Type.VIEW_ACTIVE)))
                        throw Exception("Edge not found")
                }
            }

            is PassiveEvent -> {
                val vertex = vertexByName[event.node]!!
                val peer = vertexByAddr[event.peer]!!
                if (event.added) {
                    vertex.passive.add(peer)
                    fullGraph.addEdge(vertex, peer, TreeEdge(vertex, peer, TreeEdge.Type.VIEW_PASSIVE))
                } else {
                    vertex.passive.remove(peer)
                    if (!fullGraph.removeEdge(TreeEdge(vertex, peer, TreeEdge.Type.VIEW_PASSIVE)))
                        throw Exception("Edge not found")
                }
            }

            is ManagerStateEvent -> {
                val vertex = vertexByName[event.node]!!
                vertex.managerState = event.state
            }

            is TreeStateEvent -> {
                val vertex = vertexByName[event.node]!!
                val oldState = vertex.treeState
                val oldParent = vertex.parent

                vertex.treeState = event.state

                when (event.state) {
                    TreeVertex.TreeState.PARENT_CONNECTING -> {
                        val newParent = vertexByAddr[event.parent]!!
                        if(oldParent != null) {
                            if (!fullGraph.removeEdge(TreeEdge(vertex, oldParent, TreeEdge.treeStateToEdgeType(oldState))))
                                throw Exception("Edge not found")
                        }

                        vertex.parent = newParent
                        vertex.grandparents = event.grandparents.map { vertexByAddr[it]!! }.toMutableList()
                        vertex.parentMetadata = mutableListOf()

                        fullGraph.addEdge(vertex, newParent, TreeEdge(vertex, newParent, TreeEdge.Type.CONNECTING_PARENT))
                    }

                    TreeVertex.TreeState.PARENT_CONNECTED -> {
                        val parent = vertexByAddr[event.parent]!!
                        if (!fullGraph.removeEdge(TreeEdge(vertex, parent, TreeEdge.Type.CONNECTING_PARENT)))
                            throw Exception("Edge not found")
                        fullGraph.addEdge(vertex, parent, TreeEdge(vertex, parent, TreeEdge.Type.CONNECTED_PARENT))

                        vertex.grandparents = event.grandparents.map { vertexByAddr[it]!! }.toMutableList()
                        vertex.parentMetadata = mutableListOf()
                    }
                    TreeVertex.TreeState.PARENT_SYNC -> {
                        val parent = vertexByAddr[event.parent]!!
                        if (!fullGraph.removeEdge(TreeEdge(vertex, parent, TreeEdge.Type.CONNECTED_PARENT)))
                            throw Exception("Edge not found")
                        fullGraph.addEdge(vertex, parent, TreeEdge(vertex, parent, TreeEdge.Type.SYNC_PARENT))

                        vertex.grandparents = event.grandparents.map { vertexByAddr[it]!! }.toMutableList()
                        vertex.parentMetadata = mutableListOf()
                    }

                    TreeVertex.TreeState.PARENT_READY -> {
                        val parent = vertexByAddr[event.parent]!!
                        if (!fullGraph.removeEdge(TreeEdge(vertex, parent, TreeEdge.treeStateToEdgeType(oldState))))
                            throw Exception("Edge not found")
                        fullGraph.addEdge(vertex, parent, TreeEdge(vertex, parent, TreeEdge.Type.READY_PARENT))

                        vertex.grandparents = event.grandparents.map { vertexByAddr[it]!! }.toMutableList()
                        vertex.parentMetadata = mutableListOf()

                    }

                    TreeVertex.TreeState.INACTIVE -> {
                        if(oldParent != null) {
                            if (!fullGraph.removeEdge(TreeEdge(vertex, oldParent, TreeEdge.treeStateToEdgeType(oldState))))
                                throw Exception("Edge not found")
                            checkIfCanDelete(oldParent)
                        }
                        vertex.parent = null
                        vertex.grandparents = mutableListOf()
                        vertex.parentMetadata = mutableListOf()
                    }

                    TreeVertex.TreeState.DATACENTER -> {
                        vertex.grandparents = mutableListOf()
                        vertex.parentMetadata = mutableListOf()
                    }
                }
            }

            is ChildEvent -> {
                val vertex = vertexByName[event.node]!!
                val child = vertexByAddr[event.child]!!
                when (event.state) {
                    ChildState.CONNECTED -> {

                        fullGraph.addEdge(vertex, child, TreeEdge(vertex, child, TreeEdge.Type.CONNECTED_CHILD))
                        vertex.children[child] = ""

                    }

                    ChildState.SYNC -> {
                        if (!fullGraph.removeEdge(TreeEdge(vertex, child, TreeEdge.Type.CONNECTED_CHILD)))
                            throw Exception("Edge not found")
                        fullGraph.addEdge(vertex, child, TreeEdge(vertex, child, TreeEdge.Type.SYNC_CHILD))
                    }

                    ChildState.READY -> {
                        if (!fullGraph.removeEdge(TreeEdge(vertex, child, TreeEdge.Type.SYNC_CHILD)))
                            throw Exception("Edge not found")
                        fullGraph.addEdge(vertex, child, TreeEdge(vertex, child, TreeEdge.Type.READY_CHILD))
                    }

                    ChildState.DISCONNECTED -> {
                        val r1 = fullGraph.removeEdge(TreeEdge(vertex, child, TreeEdge.Type.SYNC_CHILD))
                        val r2= fullGraph.removeEdge(TreeEdge(vertex, child, TreeEdge.Type.READY_CHILD))
                        if(!(r1 xor r2)) throw Exception("Child disconnect edge not found: $vertex $child")
                        if(vertex.children.remove(child) == null) throw Exception("Child not found")
                        checkIfCanDelete(child)
                    }
                }
            }

            is ParentMetadata -> {
                val vertex = vertexByName[event.node]!!
                vertex.parentMetadata = event.metadata
            }

            is ChildMetadata -> {
                val vertex = vertexByName[event.node]!!
                val child = vertexByAddr[event.child]!!
                if(!vertex.children.containsKey(child)) throw Exception("Child not found")
                vertex.children[child] = event.metadata
            }
        }
    }

    //TODO
    private fun checkIfCanDelete(vertex: TreeVertex) {
        return
        if (!vertex.alive && fullGraph.incomingEdgesOf(vertex).isEmpty()) {
            fullGraph.removeVertex(vertex)
            vertexByName.remove(vertex.node)
            vertexByAddr.remove(vertex.addr)
        }
    }

    private fun resetGraph() {
        val allEdges = fullGraph.edgeSet().toSet()
        val allVertex = fullGraph.vertexSet().toSet()
        fullGraph.removeAllEdges(allEdges)
        fullGraph.removeAllVertices(allVertex)
        vertexByAddr.clear()
        vertexByName.clear()
        println("Reset")
    }

}