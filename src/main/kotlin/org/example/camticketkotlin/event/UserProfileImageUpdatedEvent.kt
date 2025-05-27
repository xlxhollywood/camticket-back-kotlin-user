package org.example.camticketkotlin.event

data class UserProfileImageUpdatedEvent(
    val userId: Long,
    val profileImageUrl: String
) : KafkaEvent {
    override fun toKafkaMessage(): Any = this
}
