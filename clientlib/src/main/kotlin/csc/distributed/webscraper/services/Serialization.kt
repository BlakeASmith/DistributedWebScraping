package csc.distributed.webscraper.services

import com.google.gson.GsonBuilder
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer

class ServiceSerializer: Serializer<Service> {
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}

    val gson = GsonBuilder().setPrettyPrinting().create()
    override fun serialize(topic: String, data: Service) = gson.toJson(data).toByteArray()
}

class ServiceDeserializer: Deserializer<Service> {
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}

    val gson = GsonBuilder().setPrettyPrinting().create()
    override fun deserialize(topic: String, data: ByteArray): Service =
        gson.fromJson(String(data), Service::class.java)
}
