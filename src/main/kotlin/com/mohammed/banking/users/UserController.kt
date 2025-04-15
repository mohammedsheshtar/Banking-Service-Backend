package com.mohammed.banking.users


import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*

/*
 * @RestController tells Spring that this file will be a controller that listens to HTTP requests of clients
 * and provides the required services via the functions contained inside it and will be marked with the correct
 * annotations to represent whether the service will GET (read) data or POST (create) data using
 * @PostMapping and @GetMapping.
 */
@RestController
class UserController(
   private val userRepository: UserRepository // injecting user's database into this file (see UserRepository.kt for more information).
) {
    /*
     * this is the controller that registers users,it receives a JSON request body, and the @RequestBody annotation tells
     * Spring to automatically deserialize it into a Kotlin object of type CreateUserDTO for us to use and store in our database.
     * The @Valid annotation is used to check whether the JSON body we receive will have the required data such as our username and password,
     * if not given, it will throw one of the comments that are annotated in the DTO (Data Transfer Object) class for
     * creating the user which is @field:NotBlank and enforces that these two pieces of data must be present else it will
     * throw an error called MethodArgumentNotValidException.
     */
    @PostMapping("/users/v1/register")
    fun createUser(@Valid @RequestBody request: CreateUserDTO) {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username '${request.username}' is already taken.") // if the username already exists, we want the user to try another one.
        }

        userRepository.save(UserEntity(username = request.username, password = request.password)) // saves the new data into our database.
    }
}

    /*
     * this is our DTO, it is useful because instead of directly using the UserEntity class to return everything to the client,
     * we have the flexibility to return only the necessary information back to the client, avoiding exposing sensitive fields like passwords.
     * To illustrate, when the user creates his/her username and password, we could return a message saying
     * "[*] username has been created: {username}" but we do not want to return the password they have just created.
     */

    data class CreateUserDTO(
        @field:NotBlank(message = "Username is required")
        val username: String,

        @field:NotBlank(message = "Password is required")
        val password: String
    )