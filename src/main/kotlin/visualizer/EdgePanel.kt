package visualizer

import org.jgrapht.Graph
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.DirectedMultigraph
import org.jungrapht.visualization.VisualizationScrollPane
import org.jungrapht.visualization.VisualizationViewer
import org.jungrapht.visualization.control.DefaultGraphMouse
import org.jungrapht.visualization.control.GraphMousePlugin
import org.jungrapht.visualization.layout.algorithms.*
import org.jungrapht.visualization.layout.algorithms.util.InitialDimensionFunction
import org.jungrapht.visualization.layout.model.LayoutModel
import org.jungrapht.visualization.layout.model.Rectangle
import org.jungrapht.visualization.renderers.Renderer
import visualizer.layout.TreeEdge
import visualizer.layout.TreeVertex
import visualizer.utils.*
import visualizer.utils.ActiveEvent
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.geom.Ellipse2D
import java.net.Inet4Address
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.ChangeEvent


class EdgePanel(private val allEvents: List<TreeEvent>, maxIntervalIdx: Int) : JPanel() {

    private val dateFormat = SimpleDateFormat("HH:mm:ss,SSS")

    private val fullGraph: Graph<TreeVertex, TreeEdge>
    private var currentEvent: Int = -1

    private val vv: VisualizationViewer<TreeVertex, TreeEdge>

    private val staticLayoutAlgorithm: StaticLayoutAlgorithm<TreeVertex>
    private val treeLayoutAlgorithm: LayoutAlgorithm<TreeVertex>
    private val radialLayoutAlgorithm: LayoutAlgorithm<TreeVertex>

    private val sliderLabelCurrentTime: JLabel
    private val sliderLabelCurrentLine: JLabel
    private val timeSlider: JSlider
    private val startTimeMillis: Long
    private val endTimeMillis: Long
    private var internalChanging = false


    init {

        //fullGraph = SimpleDirectedGraph(TreeEdge::class.java)
        fullGraph = DirectedMultigraph(TreeEdge::class.java)

        layout = BorderLayout()

        val width = 30
        val height = 30
        val vertexShapeFunction = Function<TreeVertex, Shape> {
            Ellipse2D.Float(-width / 2f, -height / 2f, width.toFloat(), height.toFloat())
        }
        val boundWidth = 60.0
        val boundHeight = 60.0
        val vertexBoundsFunction = Rectangle.of(-boundWidth / 2, -boundHeight / 2, boundWidth, boundHeight)
        staticLayoutAlgorithm = StaticLayoutAlgorithm()
        //staticLayoutAlgorithm.visit()


        val graphMouse = DefaultGraphMouse<TreeVertex, TreeEdge>()

        treeLayoutAlgorithm = TreeLayoutAlgorithm()
        treeLayoutAlgorithm.setVertexBoundsFunction { vertexBoundsFunction }
        radialLayoutAlgorithm = RadialTreeLayoutAlgorithm()
        radialLayoutAlgorithm.setVertexBoundsFunction { vertexBoundsFunction }


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
        prev.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent - 1, true) }
        val prev10 = JButton("<<")
        prev10.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent - 10, true) }
        val prev100 = JButton("<<<")
        prev100.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent - 100, true) }
        val next = JButton(">")
        next.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent + 1, true) }
        val next10 = JButton(">>")
        next10.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent + 10, true) }
        val next100 = JButton(">>>")
        next100.addActionListener { e: ActionEvent? -> jumpToEvent(currentEvent + 100, true) }
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
            sliderLabelCurrentTime.text = dateFormat.format(date)
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
                is StateEvent -> markers[(event.timestamp.time - startTimeMillis).toInt()] = eventLabelState
                is GoodbyeEvent -> markers[(event.timestamp.time - startTimeMillis).toInt()] = eventLabelGoodbye
                is TreeViewEvent -> markers[(event.timestamp.time - startTimeMillis).toInt()] = eventLabelView
            }
        }
        timeSlider.labelTable = markers
        timeSlider.paintLabels = true

        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        val infoPanelLabel = JLabel("Picked Vertex Info:")
        val infoPanelText = JTextArea()
        infoPanelText.isEditable = false
        infoPanelText.lineWrap = true
        infoPanel.add(infoPanelLabel)
        infoPanel.add(infoPanelText)
        add(infoPanel, BorderLayout.EAST)

        graphMouse.add(object : MouseListener, GraphMousePlugin {
            override fun mouseClicked(e: MouseEvent?) {
                infoPanelText.text = ""
                vv.selectedVertices.forEach {
                    infoPanelText.text += it.panelText() + "\n"
                }
                println(infoPanelText.text)
            }

            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}

        })

        jumpToEvent(maxIntervalIdx, true)
    }

    private fun jumpToTime(ts: Int) {
        println("jumpToTime $ts")
        val targetTime = ts + startTimeMillis
        var target = -1
        while (target < allEvents.size - 1 && allEvents[target + 1].timestamp.time <= targetTime) {
            target++
        }
        jumpToEvent(target, false)
    }


    private fun jumpToEvent(_targetEvent: Int, updateSlider: Boolean) {
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

            redraw()
        }
    }

    private fun redraw() {
        val treeGraph = AsSubgraph(
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
        //val newAlgorithm = StaticLayoutAlgorithm<TreeVertex>()
        //LayoutAlgorithmTransition.animate(vv, treeLayoutAlgorithm)
        //vv.visualizationModel.layoutModel.accept(layoutAlgorithm)
        //vv.scaleToLayout(true)
        vv.fireStateChanged()
        vv.repaint()
    }

    private val vertexByName: MutableMap<String, TreeVertex> = mutableMapOf()
    private val vertexByAddr: MutableMap<Inet4Address, TreeVertex> = mutableMapOf()
    private fun processEvent(event: TreeEvent) {
        //println(event)
        when (event) {
            is HelloEvent -> {
                val vertex = TreeVertex(event.node, event.addr)
                vertexByName[event.node] = vertex
                vertexByAddr[event.addr] = vertex
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
                if (event.added)
                    fullGraph.addEdge(vertex, peer, TreeEdge(vertex, peer, TreeEdge.Type.VIEW_ACTIVE))
                else
                    fullGraph.removeEdge(vertex, peer)
            }
            is PassiveEvent -> {
                val vertex = vertexByName[event.node]!!
                val peer = vertexByAddr[event.peer]!!
                if (event.added)
                    fullGraph.addEdge(vertex, peer, TreeEdge(vertex, peer, TreeEdge.Type.VIEW_PASSIVE))
                else
                    fullGraph.removeEdge(vertex, peer)
            }

            is StateEvent -> {
                val vertex = vertexByName[event.node]!!
                vertex.state = event.state
            }

            is MetadataEvent -> {
                val vertex = vertexByName[event.node]!!
                vertex.ts = event.ts
                vertex.stableTs = event.stableTs
                vertex.children.clear()
                vertex.parents.clear()
                event.children.forEach {
                    val childVertex = vertexByAddr[it.first]!!
                    vertex.children[childVertex] = it.second
                }
                event.parents.forEach {
                    val parentVertex = vertexByAddr[it.first]!!
                    vertex.parents[parentVertex] = it.second
                }
            }

            is ParentEvent -> {
                val vertex = vertexByName[event.node]!!
                val parent = vertexByAddr[event.parent]!!
                when (event.state) {
                    ParentState.CONNECTING ->
                        fullGraph.addEdge(vertex, parent, TreeEdge(vertex, parent, TreeEdge.Type.CONNECTING_PARENT))

                    ParentState.SYNC -> {
                        fullGraph.removeEdge(vertex, parent)
                        fullGraph.addEdge(vertex, parent, TreeEdge(vertex, parent, TreeEdge.Type.SYNC_PARENT))
                    }

                    ParentState.READY -> {
                        fullGraph.removeEdge(vertex, parent)
                        fullGraph.addEdge(vertex, parent, TreeEdge(vertex, parent, TreeEdge.Type.READY_PARENT))
                    }

                    ParentState.DISCONNECTED -> {
                        fullGraph.removeEdge(vertex, parent)
                        checkIfCanDelete(parent)
                    }
                }
            }

            is ChildEvent -> {
                val vertex = vertexByName[event.node]!!
                val child = vertexByAddr[event.child]!!
                when (event.state) {
                    ChildState.SYNC ->
                        fullGraph.addEdge(vertex, child, TreeEdge(vertex, child, TreeEdge.Type.SYNC_CHILD))

                    ChildState.READY -> {
                        fullGraph.removeEdge(vertex, child)
                        fullGraph.addEdge(vertex, child, TreeEdge(vertex, child, TreeEdge.Type.READY_CHILD))
                    }

                    ChildState.DISCONNECTED -> {
                        fullGraph.removeEdge(vertex, child)
                        checkIfCanDelete(child)
                    }
                }
            }
        }
    }

    private fun checkIfCanDelete(vertex: TreeVertex) {
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