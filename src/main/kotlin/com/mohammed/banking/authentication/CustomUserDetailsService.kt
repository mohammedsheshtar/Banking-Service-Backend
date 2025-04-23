package com.mohammed.banking.authentication

import org.springframework.security.core.userdetails.*
import org.springframework.stereotype.Service
import com.mohammed.banking.users.UserRepository

@Service
class CustomUserDetailsService(
    private val usersRepository: UserRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = usersRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found")

        return User.builder()
            .username(user.username)
            .password(user.password)
            .roles(user.role.toString())
            .build()
    }
}