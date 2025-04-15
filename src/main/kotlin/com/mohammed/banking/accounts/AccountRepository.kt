package com.mohammed.banking.accounts

import com.mohammed.banking.users.UserEntity
import jakarta.inject.Named
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal

/*
 * the annotation "Named" is used so that we can inject our repository in other modules
 * that require interaction with the database. As you can see, the interface we made is of type JpaRepository such
 * that Spring Data JPA will be able to do all the SQL operations behind the scenes, including the function we made for this interface.
 */
@Named
interface AccountRepository : JpaRepository<AccountEntity, Long>{
    fun existsByAccountNumber(accountNumber: String): Boolean //equivalent SQL operation: SELECT COUNT(*) > 0 FROM accounts WHERE account_number = ?;
    fun findByAccountNumber(accountNumber: String): AccountEntity? //equivalent SQL operation: SELECT * FROM accounts WHERE account_number = ? LIMIT 1;

}

/*
 * the annotation @Entity tells spring that this is the data class that will be mapped to the table in our database
 * and @Table gives us the ability to explicitly tell spring the name of the table.
 */
@Entity
@Table(name = "accounts")
data class AccountEntity(
    @Id // tells Spring that this is our primary key id in the table
    @GeneratedValue(strategy = GenerationType.IDENTITY) // we want to tell spring that the id will be incrementally generated.
    val id: Long? = null,

    /*
     * this field establishes a many-to-one relationship between accounts and users. In the real world,
     * a single user can own multiple bank accounts (e.g., savings, checking, etc.), but each individual
     * account must be linked to exactly one user. In this case, we are telling Spring that there can be many instances
     * of the same user id for many accounts. The annotation @ManyTOne ensures that Spring would understand the
     * previously mentioned concept and would connect the user ids with each account that user has ever created.
    */
    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: UserEntity,

    // @Column explicitly tells spring which column this variable represents because it does not have the exact same name.
    @Column(precision = 9, scale = 3)
    val balance: BigDecimal,

    val name: String,

    @Column(name = "is_active")
    val isActive: Boolean,

    @Column(name = "account_number")
    val accountNumber: String
){
    // this constructor is crucial for not letting JPA crash out when there is no default value when instantiating an entity via reflection.
    constructor() : this(null, UserEntity(), BigDecimal.ZERO, "", false, "")
}