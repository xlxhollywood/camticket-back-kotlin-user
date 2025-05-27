package org.example.camticketkotlin.service

import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.dto.UserDto
import org.example.camticketkotlin.dto.request.UserProfileUpdateRequest
import org.example.camticketkotlin.dto.response.ArtistUserOverviewResponse
import org.example.camticketkotlin.exception.NotFoundException
import org.example.camticketkotlin.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.example.camticketkotlin.domain.enums.Role
import org.example.camticketkotlin.event.UserProfileImageUpdatedEvent
import org.example.camticketkotlin.event.UserUpdatedEvent
import org.example.camticketkotlin.kafka.GenericKafkaProducer


@Service
class UserService (
    private val userRepository: UserRepository,
    private val s3Uploader: S3Uploader,
    private val kafkaProducer: GenericKafkaProducer
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UserService::class.java)
    }
    /**
     * 사용자 닉네임과 소개글을 수정합니다.
     * 프로필 이미지 수정은 포함되지 않습니다.
     */
    @Transactional
    fun updateUserProfile(user: User, request: UserProfileUpdateRequest) {
        val foundUser = getUserById(user.id!!)

        var updated = false

        request.nickName?.let { newNickName ->
            if (newNickName.length < 2) {
                throw IllegalArgumentException("닉네임은 최소 2글자 이상이어야 합니다.")
            }

            val isDuplicate = userRepository.existsByNickName(newNickName) &&
                    foundUser.nickName != newNickName

            if (isDuplicate) {
                throw IllegalArgumentException("이미 사용 중인 닉네임입니다.")
            }

            if (foundUser.nickName != newNickName) {
                foundUser.nickName = newNickName
                updated = true
            }
        }

        request.introduction?.let {
            if (foundUser.introduction != it) {
                foundUser.introduction = it
                updated = true
            }
        }

        // ✅ Kafka 발행: 변경된 경우에만
        if (updated) {
            val event = UserUpdatedEvent(
                userId = foundUser.id!!,
                nickname = foundUser.nickName,
                introduction = foundUser.introduction
            )
            kafkaProducer.send("user.updated", foundUser.id.toString(), event)
        }
    }


    @Transactional
    fun updateProfileImage(user: User, newImage: MultipartFile): String {
        val foundUser = getUserById(user.id!!)

        logger.info("🔍 기존 프로필 이미지 URL: ${foundUser.profileImageUrl}")

        foundUser.profileImageUrl?.let {
            logger.info("🗑️ S3에서 기존 이미지 삭제 시도: $it")
            s3Uploader.delete(it)
        }

        val uploadedUrl = s3Uploader.upload(newImage, "camticket/user")
        logger.info("✅ 새 프로필 이미지 업로드 완료: $uploadedUrl")

        foundUser.profileImageUrl = uploadedUrl

        // ✅ Kafka 이벤트 발행 (이미지 전용)
        val event = UserProfileImageUpdatedEvent(
            userId = foundUser.id!!,
            profileImageUrl = uploadedUrl
        )
        kafkaProducer.send("user.profile-image-updated", foundUser.id.toString(), event)
        logger.info("📤 Kafka 전송 완료: user.profile-image-updated → ${foundUser.id}")

        return uploadedUrl
    }

    @Transactional(readOnly = true)
    fun getUserDtoById(userId: Long): UserDto {
        val user = userRepository.findById(userId)
            .orElseThrow { throw NotFoundException("해당 유저(id=${userId})가 존재하지 않습니다.")
            }
        return UserDto.toDto(user)
    }

    @Transactional(readOnly = true)
    fun getAllManagerUsers(): List<ArtistUserOverviewResponse> {
        return userRepository.findAllByRole(Role.ROLE_MANAGER).map {
            ArtistUserOverviewResponse(
                userId = it.id!!,
                nickName = it.nickName!!,
                profileImageUrl = it.profileImageUrl
            )
        }

    }

    @Transactional(readOnly = true)
    fun getUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { throw NotFoundException("해당 유저(id=${userId})가 존재하지 않습니다.")
            }
    }

    @Transactional(readOnly = true)
    fun searchUserByNickname(nickname: String): UserDto {
        val user = userRepository.findByNickName(nickname)
            ?: throw NotFoundException("닉네임 '$nickname' 에 해당하는 유저가 존재하지 않습니다.")
        return UserDto.toDto(user)
    }

}
