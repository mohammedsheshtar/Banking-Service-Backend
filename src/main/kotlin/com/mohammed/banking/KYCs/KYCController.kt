package com.mohammed.banking.KYCs


import com.mohammed.banking.users.UserEntity
import com.mohammed.banking.users.UserRepository
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate


@RestController
class KYCController(
    private val kycRepository: KYCRepository,
    private val userRepository: UserRepository
) {
    @GetMapping("/users/v1/kyc/{userId}")
    fun listKYC(@PathVariable userId: Long): KYCResponseDTO {
        val kyc = kycRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("user not found homie")

        return KYCResponseDTO(
            userId = userId,
            firstName = kyc.firstName,
            lastName = kyc.lastName,
            dateOfBirth = kyc.dateOfBirth,
            salary = kyc.salary
        )
    }

    @PostMapping("/users/v1/kyc")
    fun addOrUpdateKYC(@RequestBody request: KYCRequestDTO): KYCResponseDTO {
        val user = userRepository.findById(request.userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        val existing = kycRepository.findByUserId(request.userId)

        val kyc = if (existing != null) {
            existing.copy(
                user = user,
                firstName = request.firstName,
                lastName = request.lastName,
                dateOfBirth = request.dateOfBirth,
                salary = request.salary
            )
        } else {
            KYCEntity(
                user = user,
                firstName = request.firstName,
                lastName = request.lastName,
                dateOfBirth = request.dateOfBirth,
                salary = request.salary
            )
        }

        val saved = kycRepository.save(kyc)

        return KYCResponseDTO(
            userId = saved.user.id!!,
            firstName = saved.firstName,
            lastName = saved.lastName,
            dateOfBirth = saved.dateOfBirth,
            salary = saved.salary
        )
    }
}

data class KYCRequestDTO(
    val userId: Long,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val salary: BigDecimal
)

data class KYCResponseDTO(
    val userId: Long,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val salary: BigDecimal
)
