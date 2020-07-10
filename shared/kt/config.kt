package shared

import Address

object Configuration{
    val routingServiceAddress = Address("192.168.1.147", 5000)
    val cassandraAddress = Address("127.0.0.1", 9042)
}


