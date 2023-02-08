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

    if (args.isEmpty()) {
        println("Usage: java -jar visualizer.jar <path-to-logs>")
        return
    }

    val allEvents: MutableList<Event> = mutableListOf()

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
        var goodbye = false
        file.inputStream().bufferedReader().forEachLine {
            if (goodbye) return@forEachLine

            val tokens = it.split(" ")
            //val logLevel = tokens[0]
            val date = dateFormat.parse(tokens[1])
            //val className = tokens[2]
            val eventType = tokens[3]
            when (eventType) {
                //General events
                "Hello" -> {
                    node = tokens[6]
                    val nodeAddr = InetAddress.getByName(tokens[7]) as Inet4Address
                    allEvents.add(HelloEvent(date, node!!, nodeAddr))
                }

                "Goodbye" -> {
                    allEvents.add(GoodbyeEvent(date, node!!))
                    goodbye = true
                }

                //HyParView events
                "ACTIVE" -> {
                    val peer = InetAddress.getByName(tokens[5]) as Inet4Address
                    allEvents.add(ActiveEvent(date, node!!, peer, tokens[4] == "Added"))
                }
                /*"PASSIVE" -> {
                val peer = InetAddress.getByName(tokens[5]) as Inet4Address
                allEvents.add(PassiveEvent(date, node!!, peer, tokens[4] == "Added"))
            }*/

                //Manager Events
                "MANAGER-STATE" -> {
                    val newState = TreeVertex.ManagerState.valueOf(tokens[4])
                    allEvents.add(ManagerStateEvent(date, node!!, newState))
                }

                //Tree structure events
                "TREE-STATE" -> {
                    val newState = TreeVertex.TreeState.valueOf(tokens[4])

                    var parentAddress: Inet4Address? = null
                    val grandparents: MutableList<Inet4Address> = mutableListOf()

                    if (newState != TreeVertex.TreeState.INACTIVE && newState != TreeVertex.TreeState.DATACENTER) {
                        parentAddress = InetAddress.getByName(tokens[5].split(":")[0]) as Inet4Address

                        val rawGrandparents = tokens[6]
                        //Remove surrounding brackets and split by comma and trim
                        val grandparentsString =
                            rawGrandparents.substring(1, rawGrandparents.length - 1).trim().split(",")

                        for (grandparent in grandparentsString) {
                            if (grandparent.isEmpty()) continue
                            grandparents.add(InetAddress.getByName(grandparent.split(":")[0]) as Inet4Address)
                        }
                    }

                    allEvents.add(TreeStateEvent(date, node!!, parentAddress, newState, grandparents))
                }

                "PARENT-METADATA" -> {
                    val metadataRaw = tokens[4]
                    val metadatas = metadataRaw.substring(1, metadataRaw.length - 1).split(":")
                    allEvents.add(ParentMetadata(date, node!!, metadatas))
                }

                "CHILD" -> {
                    val newState = ChildState.valueOf(tokens[4])
                    val childAddr = InetAddress.getByName(tokens[5].split(":")[0]) as Inet4Address
                    allEvents.add(ChildEvent(date, node!!, childAddr, newState))
                }

                "CHILD-METADATA" -> {
                    val childAddr = InetAddress.getByName(tokens[4].split(":")[0]) as Inet4Address
                    val metadata = tokens[5]
                    allEvents.add(ChildMetadata(date, node!!, childAddr, metadata))
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