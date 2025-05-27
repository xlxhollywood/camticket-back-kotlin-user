package org.example.camticketkotlin.event

data class UserUpdatedEvent(
    val userId: Long,
    val nickname: String?,
    val profileImageUrl: String?
) : KafkaEvent {
    override fun toKafkaMessage(): Any = this
}
