package org.example.camticketkotlin.event

import com.camticket.user.domain.UserDomainEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDateTime
import java.util.*

@Component
class UserEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    @Value("\${app.kafka.topics.user-events}") 
    private val userEventsTopic: String
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(UserEventPublisher::class.java)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleUserRegistered(event: UserDomainEvent.UserRegistered) {
        val kafkaEvent = UserKafkaEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = "user.registered",
            timestamp = LocalDateTime.now(),
            version = "1.0",
            data = mapOf(
                "userId" to event.userId,
                "kakaoId" to event.kakaoId,
                "name" to event.name,
                "email" to event.email,
                "role" to event.role.name
            )
        )
        
        publishEvent(kafkaEvent, event.userId.toString())
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleProfileUpdated(event: UserDomainEvent.ProfileUpdated) {
        val kafkaEvent = UserKafkaEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = "user.profile.updated",
            timestamp = LocalDateTime.now(),
            version = "1.0",
            data = mapOf(
                "userId" to event.userId,
                "changes" to mapOf(
                    "nickName" to mapOf("old" to event.oldNickName, "new" to event.newNickName),
                    "introduction" to mapOf("old" to event.oldIntroduction, "new" to event.newIntroduction),
                    "bankAccount" to mapOf("old" to event.oldBankAccount, "new" to event.newBankAccount)
                )
            )
        )
        
        publishEvent(kafkaEvent, event.userId.toString())
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleProfileImageUpdated(event: UserDomainEvent.ProfileImageUpdated) {
        val kafkaEvent = UserKafkaEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = "user.profile.image.updated",
            timestamp = LocalDateTime.now(),
            version = "1.0",
            data = mapOf(
                "userId" to event.userId,
                "oldImageUrl" to event.oldImageUrl,
                "newImageUrl" to event.newImageUrl
            )
        )
        
        publishEvent(kafkaEvent, event.userId.toString())
    }

    private fun publishEvent(event: UserKafkaEvent, key: String) {
        try {
            kafkaTemplate.send(userEventsTopic, key, event)
                .whenComplete { result, ex ->
                    if (ex == null) {
                        logger.info("✅ 이벤트 발행 성공: ${event.eventType} for user $key")
                    } else {
                        logger.error("❌ 이벤트 발행 실패: ${event.eventType} for user $key", ex)
                    }
                }
        } catch (e: Exception) {
            logger.error("❌ 이벤트 발행 중 예외 발생: ${event.eventType}", e)
        }
    }
}

// UserKafkaEvent.kt
data class UserKafkaEvent(
    val eventId: String,
    val eventType: String,
    val timestamp: LocalDateTime,
    val version: String,
    val data: Map<String, Any>
)