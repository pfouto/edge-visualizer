package visualizer.utils

import java.net.Inet4Address
import java.net.InetAddress
import java.util.*

class Host(val address: InetAddress, val port: Int) {

    init {
        if (address !is Inet4Address)
            throw AssertionError("$address not and IPv4 address")
    }

    override fun toString(): String {
        return "Host(address=$address, port=$port)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Host

        if (address != other.address) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + port
        return result
    }

}
