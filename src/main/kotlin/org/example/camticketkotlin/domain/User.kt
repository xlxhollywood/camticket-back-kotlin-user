package org.example.camticketkotlin.domain

import jakarta.persistence.*
import org.example.camticketkotlin.domain.enums.Role
import org.example.camticketkotlin.dto.UserDto
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.AbstractAggregateRoot
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "users")
class User private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val kakaoId: Long,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = true, length = 30, unique = true)
    var nickName: String? = null,

    @Column(nullable = false, length = 100)
    var email: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var profileImageUrl: String,

    @Column(nullable = true, length = 500)
    var introduction: String? = null,

    @Column(nullable = true, length = 100)
    var bankAccount: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role,

    @CreatedDate
    @Column(updatable = false)
    var regDate: LocalDateTime? = null,

    @LastModifiedDate
    var modDate: LocalDateTime? = null

) : AbstractAggregateRoot<User>() { // 도메인 이벤트 발행을 위해 상속

    fun updateProfile(nickName: String?, introduction: String?, bankAccount: String?) {
        val oldNickName = this.nickName
        val oldIntroduction = this.introduction
        val oldBankAccount = this.bankAccount

        nickName?.let { this.nickName = it }
        introduction?.let { this.introduction = it }
        bankAccount?.let { this.bankAccount = it }

        // 프로필 변경 이벤트 발행
        registerEvent(
            UserDomainEvent.ProfileUpdated(
                userId = this.id!!,
                oldNickName = oldNickName,
                newNickName = this.nickName,
                oldIntroduction = oldIntroduction,
                newIntroduction = this.introduction,
                oldBankAccount = oldBankAccount,
                newBankAccount = this.bankAccount
            )
        )
    }

    fun updateProfileImage(newImageUrl: String) {
        val oldImageUrl = this.profileImageUrl
        this.profileImageUrl = newImageUrl

        // 프로필 이미지 변경 이벤트 발행
        registerEvent(
            UserDomainEvent.ProfileImageUpdated(
                userId = this.id!!,
                oldImageUrl = oldImageUrl,
                newImageUrl = newImageUrl
            )
        )
    }

    fun updateFromKakao(name: String, email: String, profileImageUrl: String) {
        this.name = name
        this.email = email
        this.profileImageUrl = profileImageUrl

        // 카카오 정보 동기화 이벤트 발행
        registerEvent(
            UserDomainEvent.KakaoInfoSynced(
                userId = this.id!!,
                name = name,
                email = email,
                profileImageUrl = profileImageUrl
            )
        )
    }

    companion object {
        fun create(dto: UserDto): User {
            val user = User(
                kakaoId = requireNotNull(dto.kakaoId) { "카카오 ID는 필수입니다" },
                name = requireNotNull(dto.name) { "이름은 필수입니다" },
                nickName = dto.nickName,
                email = requireNotNull(dto.email) { "이메일은 필수입니다" },
                profileImageUrl = requireNotNull(dto.profileImageUrl) { "프로필 이미지는 필수입니다" },
                introduction = dto.introduction,
                bankAccount = dto.bankAccount,
                role = dto.role ?: Role.ROLE_USER
            )

            // 사용자 생성 이벤트 발행
            user.registerEvent(
                UserDomainEvent.UserRegistered(
                    userId = user.id ?: 0L, // 실제로는 저장 후 ID가 생성됨
                    kakaoId = user.kakaoId,
                    name = user.name,
                    email = user.email,
                    role = user.role
                )
            )

            return user
        }
    }
}

// 도메인 이벤트 정의
sealed class UserDomainEvent {
    data class UserRegistered(
        val userId: Long,
        val kakaoId: Long,
        val name: String,
        val email: String,
        val role: Role
    ) : UserDomainEvent()

    data class ProfileUpdated(
        val userId: Long,
        val oldNickName: String?,
        val newNickName: String?,
        val oldIntroduction: String?,
        val newIntroduction: String?,
        val oldBankAccount: String?,
        val newBankAccount: String?
    ) : UserDomainEvent()

    data class ProfileImageUpdated(
        val userId: Long,
        val oldImageUrl: String,
        val newImageUrl: String
    ) : UserDomainEvent()

    data class KakaoInfoSynced(
        val userId: Long,
        val name: String,
        val email: String,
        val profileImageUrl: String
    ) : UserDomainEvent()
}
