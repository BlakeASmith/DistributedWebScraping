package ca.blakeasmith.kkafka.jvm.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.apache.kafka.common.serialization.*

abstract class KeyValueSerialization<K, V>{
    abstract val keySerializer: Serializer<K>
    abstract val keyDeserializer: Deserializer<K>
    abstract val valueSerializer: Serializer<V>
    abstract val valueDeserializer: Deserializer<V>
}

abstract class KeyValueSerde<K, V>(override val keySerializer: Serializer<K>,
                          override val keyDeserializer: Deserializer<K>,
                          override val valueSerializer: Serializer<V>,
                          override val valueDeserializer: Deserializer<V>
): KeyValueSerialization<K, V>(){

    constructor(keySerde: Serde<K>, valueSerde: Serde<V>): this(
            keySerde.serializer(),
            keySerde.deserializer(),
            valueSerde.serializer(),
            valueSerde.deserializer()
    )
}

object StringSerialization : KeyValueSerialization<String, String>(){
    override val keySerializer: Serializer<String> = StringSerializer()
    override val keyDeserializer: Deserializer<String> = StringDeserializer()
    override val valueSerializer: Serializer<String> = StringSerializer()
    override val valueDeserializer: Deserializer<String> = StringDeserializer()
}

abstract class JsonSerde<K>(val kSerializer: KSerializer<K>, config: JsonConfiguration): Serializer<K>, Deserializer<K>, Serde<K>{
    private val json = Json(config)
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
    override fun serialize(topic: String?, data: K): ByteArray {
        return json.stringify(kSerializer, data).toByteArray(Charsets.UTF_8)
    }
    override fun deserialize(topic: String, data: ByteArray): K {
        return json.parse(kSerializer, String(data, Charsets.UTF_8))
    }

    override fun deserializer(): Deserializer<K> = this
    override fun serializer(): Serializer<K> = this
}

abstract class JsonSerialization<K, V>(
        _keySerializer: JsonSerde<K>,
        _valueSerde: JsonSerde<V>
): KeyValueSerialization<K, V>(){
    override val keySerializer: Serializer<K> = _keySerializer
    override val keyDeserializer: Deserializer<K> = _keySerializer
    override val valueSerializer: Serializer<V> = _valueSerde
    override val valueDeserializer: Deserializer<V> = _valueSerde
}

