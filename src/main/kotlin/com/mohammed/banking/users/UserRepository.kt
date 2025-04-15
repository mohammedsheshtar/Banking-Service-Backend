package com.mohammed.banking.users

import jakarta.inject.Named
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository

/*
 * the annotation "Named" is used so that we can inject our repository in other modules
 * that require interaction with the database. As you can see, the interface we made is of type JpaRepository such
 * that Spring Data JPA will be able to do all the SQL operations behind the scenes including the function we made for this interface.
 */
@Named
interface UserRepository : JpaRepository <UserEntity, Long> {
    fun existsByUsername(username: String): Boolean
    // equivalent SQL operation: SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END FROM users WHERE username = ?;
}

/*
 * the annotation @Entity tells spring that this is the data class that will be mapped to the table in our database
 * and @Table gives us the ability to explicitly tell spring the name of the table.
 */
@Entity
@Table(name = "users")
data class UserEntity(
    @Id // tells Spring that this is our primary key id in the table
    @GeneratedValue(strategy = GenerationType.IDENTITY) // we want to tell spring that the id will be incrementally generated.
    val id: Long? = null,
    val username: String,
    val password: String

){
    // this constructor is crucial for not letting JPA crash out when there is no default value when instantiating an entity via reflection.
    constructor() : this(null, "", "")
}
