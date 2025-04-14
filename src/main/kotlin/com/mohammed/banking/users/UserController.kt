package com.mohammed.banking.users


import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*

@RestController
class UserController(
   private val userRepository: UserRepository
) {
    @PostMapping("/users/v1/register")
    fun createUser(@Valid @RequestBody request: CreateUserDTO) {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username '${request.username}' is already taken.")
        }

        userRepository.save(UserEntity(username = request.username, password = request.password))
    }
}


    data class CreateUserDTO(
        @field:NotBlank(message = "Username is required")
        val username: String,

        @field:NotBlank(message = "Password is required")
        val password: String
    )