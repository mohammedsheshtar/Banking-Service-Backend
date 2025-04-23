package com.mohammed.banking.users

import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


@Service
class UserServices(
    private val userRepository: UserRepository, //injecting user's database into this file (see UserRepository.kt for more information).
    private val passwordEncoder: PasswordEncoder
) {

    fun createUser(request: CreateUserDTO): ResponseEntity<Any> {
        if (userRepository.existsByUsername(request.username)) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Username '${request.username}' is already taken."))
        }

        if (request.username.length >= 12) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Username '${request.username}' is too long."))
        }

        if (request.username.length <= 4) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Username '${request.username}' is too short."))
        }

        val hashedPassword = passwordEncoder.encode(request.password)
        userRepository.save(UserEntity(username = request.username, password = hashedPassword))
        return ResponseEntity.ok().build()


    }
}
