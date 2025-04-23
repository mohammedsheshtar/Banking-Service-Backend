package com.mohammed.banking.scripts

import com.mohammed.banking.BankingServiceApplication
import com.mohammed.banking.users.Roles
import com.mohammed.banking.users.UserEntity
import com.mohammed.banking.users.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder

@SpringBootApplication
class InitUserRunner {
    @Bean
    fun initUsers(userRepository: UserRepository, passwordEncoder: PasswordEncoder) = CommandLineRunner {
        val user = UserEntity(
            username = "testuser",
            password = passwordEncoder.encode("password123"),
            role = Roles.USER
        )
        if (userRepository.findByUsername(user.username) == null) {
            println("Creating user ${user.username}")
            userRepository.save(user)
        } else  {
            println("User ${user.username} already exists")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<BankingServiceApplication>(*args).close()
}