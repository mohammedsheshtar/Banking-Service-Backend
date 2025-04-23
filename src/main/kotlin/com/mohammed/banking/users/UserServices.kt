package com.mohammed.banking.users

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class UserServices(
    private val userRepository: UserRepository
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

        userRepository.save(UserEntity(username = request.username, password = request.password))
        return ResponseEntity.ok().build()


    }
}
