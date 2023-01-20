import visualizer.EdgePanel
import visualizer.layout.TreeVertex
import visualizer.utils.*
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.text.SimpleDateFormat
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants

val dateFormat = SimpleDateFormat("yyyy/MM/dd-HH:mm:ss,SSS")

fun main(args: Array<String>) {

    if(args.isEmpty()){
        println("Usage: java -jar visualizer.jar <path-to-logs>")
        return
    }

    val allEvents: MutableList<TreeEvent> = mutableListOf()

    println("Reading log files from ${args[0]}... ")
    val folder = File(args[0])
    val files = folder.listFiles()
    for (file in files) {
        if (file.isDirectory) {
            println("Skipping directory ${file.name}")
            continue
        }
        if (file.extension != "log") {
            println("Skipping file ${file.name}")
            continue
        }
        println(file.name)
        var node: String? = null
        file.inputStream().bufferedReader().forEachLine {
            val tokens = it.split(" ")
            //val logLevel = tokens[0]
            val date = dateFormat.parse(tokens[1])
            //val className = tokens[2]
            val eventType = tokens[3]
            when(eventType){
                "Hello" -> {
                    node = tokens[6]
                    val nodeAddr = InetAddress.getByName(tokens[7]) as Inet4Address
                    allEvents.add(HelloEvent(date, node!!, nodeAddr))
                }
                "Goodbye" -> allEvents.add(GoodbyeEvent(date, node!!))
                /*"PASSIVE" -> {
                    val peer = InetAddress.getByName(tokens[5]) as Inet4Address
                    allEvents.add(PassiveEvent(date, node!!, peer, tokens[4] == "Added"))
                }*/
                "ACTIVE" -> {
                    val peer = InetAddress.getByName(tokens[5]) as Inet4Address
                    allEvents.add(ActiveEvent(date, node!!, peer, tokens[4] == "Added"))
                }
                "STATE" -> {
                    val newState = TreeVertex.State.valueOf(tokens[4])
                    allEvents.add(StateEvent(date, node!!, newState))
                }
                "CHILD" -> {
                    val newState = ChildState.valueOf(tokens[4])
                    val childAddr = InetAddress.getByName(tokens[5].split(":")[0]) as Inet4Address
                    allEvents.add(ChildEvent(date, node!!, childAddr, newState))
                }
                "PARENT" -> {
                    val newState = ParentState.valueOf(tokens[4])
                    val parentAddr = InetAddress.getByName(tokens[5].split(":")[0]) as Inet4Address
                    allEvents.add(ParentEvent(date, node!!, parentAddr, newState))
                }
                "METADATA" -> {
                    val children = mutableListOf<Pair<Inet4Address, String>>()
                    val parents = mutableListOf<Pair<Inet4Address, String>>()
                    val ts = tokens[4]
                    val stableTs = tokens[5]
                    val childrenString = tokens[6].split("_")
                    val parentsString = tokens[7].split("_")

                    if(childrenString[1].isNotEmpty()){
                        val allChildren = childrenString[1].split(";")
                        for(child in allChildren){
                            if(child.isEmpty()) continue
                            val childTokens = child.split("-")
                            val childAddr = InetAddress.getByName(childTokens[0].split(":")[0]) as Inet4Address
                            val childState = childTokens[1]
                            children.add(Pair(childAddr, childState))
                        }
                    }
                    if(parentsString[1].isNotEmpty()){
                        val allParents = parentsString[1].split(";")
                        for(parent in allParents){
                            if(parent.isEmpty())continue
                            val parentTokens = parent.split("-")
                            val parentAddr = InetAddress.getByName(parentTokens[0].split(":")[0]) as Inet4Address
                            val parentState = parentTokens[1]
                            parents.add(Pair(parentAddr, parentState))
                        }
                    }
                    val event = MetadataEvent(date, node!!, ts, stableTs, children, parents)
                    allEvents.add(event)
                }
            }
        }
    }
    allEvents.sortBy { it.timestamp }

    //allEvents.forEach(::println)
    println("Done reading logs. ${allEvents.size} events read. ")

    println("Looking for stable periods... ")

    //Find largest event interval
    val filter = allEvents.filter { it !is MetadataEvent && it !is HyParViewEvent }

    var maxInterval = 0L
    var maxIntervalEvent = filter[0]
    for (i in 0 until filter.size - 1) {
        val interval = filter[i + 1].timestamp.time - filter[i].timestamp.time
        if (interval > maxInterval) {
            maxInterval = interval
            maxIntervalEvent = filter[i]
        }
    }
    val maxIntervalIdx = allEvents.indexOf(maxIntervalEvent)

    val jp: JPanel = EdgePanel(allEvents, maxIntervalIdx)

    val frame = JFrame()

    frame.title = "Edge Tree Viewer"
    frame.contentPane.add(jp)
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.pack()
    frame.isVisible = true

}