package com.camticket.user.service

import com.camticket.user.domain.User
import com.camticket.user.domain.enums.Role
import com.camticket.user.dto.UserDto
import com.camticket.user.dto.request.UserProfileUpdateRequest
import com.camticket.user.dto.response.ArtistUserOverviewResponse
import com.camticket.user.exception.NotFoundException
import com.camticket.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val s3Uploader: S3Uploader
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UserService::class.java)
    }

    @Transactional
    fun updateUserProfile(user: User, request: UserProfileUpdateRequest) {
        val foundUser = userRepository.findById(user.id!!)
            .orElseThrow { NotFoundException("해당 유저가 존재하지 않습니다.") }

        // 닉네임 중복 검사
        request.nickName?.let { newNickName ->
            if (newNickName.length < 2) {
                throw IllegalArgumentException("닉네임은 최소 2글자 이상이어야 합니다.")
            }

            val isDuplicate = userRepository.existsByNickName(newNickName) &&
                    foundUser.nickName != newNickName

            if (isDuplicate) {
                throw IllegalArgumentException("이미 사용 중인 닉네임입니다.")
            }
        }

        // 도메인 객체를 통해 업데이트 (이벤트 자동 발행)
        foundUser.updateProfile(
            nickName = request.nickName,
            introduction = request.introduction,
            bankAccount = request.bankAccount
        )

        userRepository.save(foundUser) // 이벤트가 여기서 발행됨
    }

    @Transactional
    fun updateProfileImage(user: User, newImage: MultipartFile): String {
        val foundUser = userRepository.findById(user.id!!)
            .orElseThrow { NotFoundException("유저가 존재하지 않습니다.") }

        logger.info("🔍 기존 프로필 이미지 URL: ${foundUser.profileImageUrl}")

        // 기존 이미지 삭제
        foundUser.profileImageUrl.let {
            logger.info("🗑️ S3에서 기존 이미지 삭제 시도: $it")
            s3Uploader.delete(it)
        }

        // 새 이미지 업로드
        val uploadedUrl = s3Uploader.upload(newImage, "camticket/user")
        logger.info("✅ 새 프로필 이미지 업로드 완료: $uploadedUrl")

        // 도메인 객체를 통해 업데이트 (이벤트 자동 발행)
        foundUser.updateProfileImage(uploadedUrl)
        userRepository.save(foundUser)

        return uploadedUrl
    }

    @Transactional(readOnly = true)
    fun getUserDtoById(userId: Long): UserDto {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("해당 유저가 존재하지 않습니다.") }

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

    // 다른 서비스에서 사용자 정보 조회용 (내부 API)
    @Transactional(readOnly = true)
    fun getUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { NotFoundException("해당 유저가 존재하지 않습니다.") }
    }

    // 다른 서비스에서 사용자 목록 조회용 (내부 API)
    @Transactional(readOnly = true)
    fun getUsersByIds(userIds: List<Long>): List<UserDto> {
        return userRepository.findAllById(userIds).map { UserDto.toDto(it) }
    }

    // 사용자 존재 여부 확인 (내부 API)
    @Transactional(readOnly = true)
    fun existsById(userId: Long): Boolean {
        return userRepository.existsById(userId)
    }
}