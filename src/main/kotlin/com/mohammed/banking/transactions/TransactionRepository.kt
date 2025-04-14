package com.mohammed.banking.transactions

import jakarta.inject.Named
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import com.mohammed.banking.accounts.AccountEntity
import java.math.BigDecimal

/*
 * the annotation "Named" is used so that we can inject our repository in other modules
 * that require interaction with the database. As you can see, the interface we made is of type JpaRepository such
 * that Spring Data JPA will be able to do all the SQL operations behind the scenes
 */

@Named
interface TransactionRepository : JpaRepository<TransactionEntity, Long>

/*
 * the annotation @Entity tells spring that this is the data class that will be mapped to the table in our database
 * and @Table gives us the ability to explicitly tell spring the name of the table
 */
@Entity
@Table(name = "transactions")
data class TransactionEntity(
    @Id // tells Spring that this is our primary key id in the table
    @GeneratedValue(strategy = GenerationType.IDENTITY) // we want to tell spring that the id will be incrementally generated
    val id: Long? = null,

    /*
     * the line below is one of the most important lines in this code to connect and establish a relationship
     * between specific entities when desired, such as letting an entity interact with another entity's data. In this case,
     * we are telling Spring that there can be many instances of the same account id for many transactions. The annotation
     * @ManyTOne ensures that Spring would understand the previously mentioned concept and would connect the account ids
     * with the transactions to ensure the required account (source) is sending money to the other required account (destinations)
     */
    @ManyToOne
    @JoinColumn(name = "source_account", referencedColumnName = "id")
    val sourceAccount: AccountEntity,

    @ManyToOne
    @JoinColumn(name = "destination_account", referencedColumnName = "id")
    val destinationAccount: AccountEntity,

    @Column(precision = 9, scale = 3) // ensures that there can be nine total numbers with three being after the decimal point
    val amount: BigDecimal
){
    // this constructor is crucial for not letting JPA crash out when there is no default value when instantiating an entity via reflection
    constructor() : this(null, AccountEntity(), AccountEntity(), BigDecimal.ZERO)
}