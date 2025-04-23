package com.mohammed.banking.KYCs

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import com.mohammed.banking.users.UserRepository
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

/*
 * @RestController tells Spring that this file will be a controller that listens to HTTP requests of clients
 * and provides the required services via the functions contained inside it and will be marked with the correct
 * annotations to represent whether the service will GET (read) data or POST (create) data using
 * @PostMapping and @GetMapping.
 */
@RestController
class KYCController(
    private val kycServices: KYCServices
) {

    /*
     * this is our GET endpoint, this controller gets all the data available in the KYC database thats related to the user, we know which user
     * to search for because the user's id is given to us as an argument in the URL and we retrieve and convert it into
     * a Kotlin variable by using @PathVariable before declaring a parameter to assign that argument to. Once that is done,
     * we check whether the user exists, if not, we stop here and tell the client the user is not found, if yes, we then
     * fetch all the KYC data available to that user in our database and send it back to the client.
     */
    @GetMapping("/users/v1/kyc/{userId}")
    fun listKYC(@PathVariable userId: Long): ResponseEntity<Any> {
        return kycServices.getKYC(userId)
    }


    /* this is our POST endpoint, this controller is responsible for creating/updating the KYC data related to the user.
     * We first check if the user exists from the data given to us, if not, tell the client the user is not found,
     * if yes, we check whether the user exists, if not, we stop here and tell the client the user is not found,
     * if yes, we then check if the user has a KYC profile, if not, then make a new one with the data given to us, if yes,
     * then update his/her KYC data with the newly received data. Finally, we return the results of the operation to the client.
     */
    @PostMapping("/users/v1/kyc")
    fun addOrUpdateKYC(@RequestBody request: KYCRequestDTO): ResponseEntity<Any> {
        return kycServices.addOrUpdateKYC(request)
    }
}

    /*
 * our DTOs for creating/updating and fetching the KYC data. Although they currently contain the same data fields,
 * in the future, the required variables for either the POST or GET might change, so I made one for each for flexibility.
 */
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

