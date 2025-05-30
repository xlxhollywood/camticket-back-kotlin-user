package org.example.camticketkotlin.service

import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.dto.UserDto
import org.example.camticketkotlin.domain.UserDomainEvent
import org.example.camticketkotlin.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService(
        private val userRepository: UserRepository,
        private val randomNicknameService: RandomNicknameService,
        private val applicationEventPublisher: ApplicationEventPublisher
) {

    // AuthService에 로그 추가
    fun kakaoLogin(dto: UserDto): UserDto {
        val kakaoId = dto.kakaoId ?: throw IllegalArgumentException("카카오 ID는 null일 수 없습니다.")

        val user = userRepository.findByKakaoId(kakaoId)
            .orElseGet {
//                println("🆕 신규 사용자 생성: kakaoId=$kakaoId")
                val newUser = User.from(dto)
                newUser.nickName = randomNicknameService.generateUniqueNickname()
                val savedUser = userRepository.save(newUser)
//                println("✅ 사용자 저장 완료: userId=${savedUser.id}")

                // ✅ 이벤트 발행 (올바른 방식)
                applicationEventPublisher.publishEvent(
                    UserDomainEvent.UserRegistered(
                        userId = savedUser.id!!,
                        kakaoId = savedUser.kakaoId,
                        name = savedUser.name,
                        email = savedUser.email,
                        role = savedUser.role
                    )
                )
//                println("📤 ApplicationEventPublisher로 이벤트 발행: userId=${savedUser.id}")
                savedUser
            }

//        println("🔄 기존 사용자 로그인: userId=${user.id}")

        // 기존 사용자 정보 업데이트
        dto.email?.let { user.email = it }
        dto.profileImageUrl?.let { user.profileImageUrl = it }
        dto.name?.let { user.name = it }

        return UserDto.toDto(user)
    }

    // 사용자 ID로 로그인한 사용자 정보 조회
    fun getLoginUser(userId: Long): User {
        return userRepository.findById(userId)
                .orElseThrow { IllegalArgumentException("해당 유저가 없습니다.") }
    }
}
