package org.example.camticketkotlin.dto.request

data class UserProfileUpdateRequest(
    val nickName: String?,
    val introduction: String?,
    val bankAccount: String?  // ← 추가
)