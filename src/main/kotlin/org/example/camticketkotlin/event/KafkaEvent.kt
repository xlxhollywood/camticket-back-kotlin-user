package org.example.camticketkotlin.event

interface KafkaEvent {
    fun toKafkaMessage(): Any
}