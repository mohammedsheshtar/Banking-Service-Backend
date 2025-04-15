package com.mohammed.banking.KYCs

import com.mohammed.banking.users.UserEntity
import jakarta.inject.Named
import java.math.BigDecimal
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

/*
 * the annotation "Named" is used so that we can inject our repository in other modules
 * that require interaction with the database. As you can see, the interface we made is of type JpaRepository such
 * that Spring Data JPA will be able to do all the SQL operations behind the scenes including the function we made for this interface.
 */
@Named
interface KYCRepository : JpaRepository<KYCEntity, Long> {
    fun findByUserId(userId: Long): KYCEntity?
    //equivalent SQL operation: SELECT * FROM KYCs WHERE user_id = ? LIMIT 1;
}

/*
 * the annotation @Entity tells spring that this is the data class that will be mapped to the table in our database
 * and @Table gives us the ability to explicitly tell spring the name of the table.
 */
@Entity
@Table(name = "KYCs")
data class KYCEntity(
    @Id // tells Spring that this is our primary key id in the table.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // we want to tell spring that the id will be incrementally generated.
    val id: Long? = null,

    /*
     * the line below is used to establish a one-to-one relationship between the KYCEntity and the UserEntity.
     * In real-world banking systems, a user's Know Your Customer (KYC) profile is unique and directly tied to a single user.
     * So, each user entity will ONLY be connected to ONE KYC entity and vice versa. Unlike the case with account and transactions,
     * ONE account can have MANY transactions. This relationship allows us to fetch the full user details from a given KYC record,
     * and enforces the business rule that a user cannot have more than one KYC record.
     */
    @OneToOne
    @JoinColumn(name = "user_id")
    val user: UserEntity,

    // @Column explicitly tells spring which column this variable represents because it does not have the exact same name.
    @Column(name = "date_of_birth")
    val dateOfBirth: LocalDate,

    @Column(name = "first_name")
    val firstName: String,

    @Column(name = "last_name")
    val lastName: String,

    @Column(precision = 9, scale = 3)
    val salary: BigDecimal
){
    // this constructor is crucial for not letting JPA crash out when there is no default value when instantiating an entity via reflection.
    constructor() : this(null, UserEntity(), LocalDate.MIN, "", "", BigDecimal.ZERO)
}