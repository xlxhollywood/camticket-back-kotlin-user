package org.example.camticketkotlin.event

data class UserCreatedEvent(
    val id: Long,
    val kakaoId: Long,
    val email: String,
    val name: String,
    val nickname: String,
    val profileImageUrl: String
)
