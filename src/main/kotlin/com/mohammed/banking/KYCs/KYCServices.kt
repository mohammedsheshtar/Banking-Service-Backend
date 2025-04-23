package com.mohammed.banking.KYCs

import com.mohammed.banking.users.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import com.mohammed.banking.KYCs.KYCResponseDTO
import com.mohammed.banking.KYCs.KYCRequestDTO



@Service
class KYCServices(
    private val kycRepository: KYCRepository, // injecting KYC's database into this file (see KYCRepository.kt for more information).
    private val userRepository: UserRepository // injecting user's database into this file (see UserRepository.kt for more information).
) {
    fun getKYC(userId: Long): ResponseEntity<Any> {
        val kyc = kycRepository.findByUserId(userId)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "User with ID $userId was not found"))

        return ResponseEntity.ok(
            KYCResponseDTO(
                userId = userId,
                firstName = kyc.firstName,
                lastName = kyc.lastName,
                dateOfBirth = kyc.dateOfBirth,
                salary = kyc.salary
            )
        )
    }

    fun addOrUpdateKYC(request: KYCRequestDTO): ResponseEntity<Any> {
        val user = userRepository.findById(request.userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "User with ID ${request.userId} was not found"))

        val existing = kycRepository.findByUserId(request.userId) // retrieving whatever data available in the KYC database for this user

        val age = java.time.Period.between(request.dateOfBirth, LocalDate.now()).years
        if (age < 18) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "you must be 18 or older to register"))
        }

        if (request.salary < BigDecimal(100) || request.salary > BigDecimal(1000000)) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "salary must be between 100 and 1,000,000 KD"))
        }

        val kyc = if (existing != null) { // updating data
            existing.copy(
                user = user,
                firstName = request.firstName,
                lastName = request.lastName,
                dateOfBirth = request.dateOfBirth,
                salary = request.salary
            )
        } else {
            KYCEntity( // making a new KYC profile for this user
                user = user,
                firstName = request.firstName,
                lastName = request.lastName,
                dateOfBirth = request.dateOfBirth,
                salary = request.salary
            )
        }

        val saved = kycRepository.save(kyc) // saving the new/updated data

        return ResponseEntity.ok( KYCResponseDTO( // returning the results of the operation to the client
            userId = saved.user.id!!,
            firstName = saved.firstName,
            lastName = saved.lastName,
            dateOfBirth = saved.dateOfBirth,
            salary = saved.salary
        ))
    }

}