package org.example.camticketkotlin.event

data class UserUpdatedEvent(
    val userId: Long,
    val nickname: String?,
    val introduction: String?,
    val bankAccount: String?
) : KafkaEvent {
    override fun toKafkaMessage(): Any = this
}
