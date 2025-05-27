package org.example.camticketkotlin.kafka

import org.example.camticketkotlin.event.KafkaEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class GenericKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun send(topic: String, key: String?, event: KafkaEvent) {
        val message = event.toKafkaMessage()
        if (key != null) kafkaTemplate.send(topic, key, message)
        else kafkaTemplate.send(topic, message)
    }
}
