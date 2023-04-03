import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import kotlinx.coroutines.channels.Channel


suspend fun readFile(file: File, channel: Channel<Event>) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd-HH:mm:ss,SSS")

    if (file.isDirectory) {
        println("Skipping directory ${file.name}")
        return
    }
    if (file.extension != "log") {
        println("Skipping file ${file.name}")
        return
    }
    println(file.name + " " + Thread.currentThread())
    var node: String? = null
    var goodbye = false
    val reader = file.inputStream().bufferedReader()

    for (it in reader.lines()){
        if (goodbye) break

        val tokens = it.split(" ")
        //val logLevel = tokens[0]
        val date = dateFormat.parse(tokens[1])
        //val className = tokens[2]
        val eventType = tokens[4]
        when (eventType) {
            //General events
            "Hello" -> {
                node = tokens[7]
                val nodeAddr = InetAddress.getByName(tokens[8]) as Inet4Address
                channel.send(HelloEvent(date, node!!, nodeAddr))
            }

            "Goodbye" -> {
                channel.send(GoodbyeEvent(date, node!!))
                goodbye = true
            }

            //HyParView events
            "ACTIVE" -> {
                val peer = InetAddress.getByName(tokens[6]) as Inet4Address
                channel.send(ActiveEvent(date, node!!, peer, tokens[5] == "Added"))
            }
            /*"PASSIVE" -> {
            val peer = InetAddress.getByName(tokens[5]) as Inet4Address
            allEvents.add(PassiveEvent(date, node!!, peer, tokens[4] == "Added"))
            }*/

            //Manager Events
            "MANAGER-STATE" -> {
                val newState = TreeVertex.ManagerState.valueOf(tokens[5])
                channel.send(ManagerStateEvent(date, node!!, newState))
            }

            //Tree structure events
            "TREE-STATE" -> {
                val newState = TreeVertex.TreeState.valueOf(tokens[5])

                var parentAddress: Inet4Address? = null
                val grandparents: MutableList<Inet4Address> = mutableListOf()

                if (newState != TreeVertex.TreeState.INACTIVE && newState != TreeVertex.TreeState.DATACENTER) {
                    parentAddress = InetAddress.getByName(tokens[6].split(":")[0]) as Inet4Address

                    val rawGrandparents = tokens[7]
                    //Remove surrounding brackets and split by comma and trim
                    val grandparentsString =
                        rawGrandparents.substring(1, rawGrandparents.length - 1).trim().split(",")

                    for (grandparent in grandparentsString) {
                        if (grandparent.isEmpty()) continue
                        grandparents.add(InetAddress.getByName(grandparent.split(":")[0]) as Inet4Address)
                    }
                }

                channel.send(TreeStateEvent(date, node!!, parentAddress, newState, grandparents))
            }

            "PARENT-METADATA" -> {
                val metadataRaw = tokens[5]
                val metadatas = metadataRaw.substring(1, metadataRaw.length - 1).split(":")
                channel.send(ParentMetadata(date, node!!, metadatas))
            }

            "CHILD" -> {
                val newState = ChildState.valueOf(tokens[5])
                val childAddr = InetAddress.getByName(tokens[6].split(":")[0]) as Inet4Address
                channel.send(ChildEvent(date, node!!, childAddr, newState))
            }

            "CHILD-METADATA" -> {
                val childAddr = InetAddress.getByName(tokens[5].split(":")[0]) as Inet4Address
                val metadata = tokens[6]
                channel.send(ChildMetadata(date, node!!, childAddr, metadata))
            }
        }
    }
}

fun main(args: Array<String>) = runBlocking{

    if (args.isEmpty()) {
        println("Usage: java -jar visualizer.jar <path-to-logs>")
        return@runBlocking
    }

    val allEvents: MutableList<Event> = mutableListOf()

    println("Reading log files from ${args[0]}... ")
    val folder = File(args[0])
    val files = folder.listFiles()

    val channel: Channel<Event> = Channel(1000)
    launch(Dispatchers.Default) {
        for (file in files) {
            launch { readFile(file, channel)}
        }
    }.invokeOnCompletion { channel.close() }


    for (event in channel) {
        allEvents.add(event)
    }

    allEvents.sortBy { it.timestamp }
    allEvents.forEachIndexed { index, event -> event.index = index }

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