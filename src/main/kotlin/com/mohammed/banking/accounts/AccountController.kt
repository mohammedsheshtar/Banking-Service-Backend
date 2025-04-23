package com.mohammed.banking.accounts

import com.mohammed.banking.transactions.TransactionEntity
import com.mohammed.banking.transactions.TransactionRepository
import com.mohammed.banking.users.UserRepository
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.security.SecureRandom
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/*
 * @RestController tells Spring that this file will be a controller that listens to HTTP requests of clients
 * and provides the required services via the functions contained inside it and will be marked with the correct
 * annotations to represent whether the service will GET (read) data or POST (create) data using
 * @PostMapping and @GetMapping.
 */
@RestController
class AccountController(
    private val accountsServices: AccountsServices
) {

    /*
     * this is our GET endpoint for our accounts. This service is responsible for retrieving all the existing ACTIVE accounts
     * in our database and returns them to the client.
     */
    @GetMapping("/accounts/v1/accounts")
    fun listAccounts(): ListAccounts {
        return accountsServices.listAccounts()
    }

    /*
     * this is one of our POST endpoints. This one is responsible for creating a new account for the user.
     * First, it checks whether the user exists, if no, tell the client the user was not found, if yes, then create
     * a new account for this user. We do not care about whether the user already has an account, each user can have
     * multiple accounts in this business, and we established that in our AccountRepository.kt file using
     * the @ManyToOne annotation. Once the account is created, return to the user the results of the operation including
     * the newly generated account number.
     */
    @PostMapping("/accounts/v1/accounts")
    fun addAccount(@RequestBody request: CreateAccountRequest): ResponseEntity<Any> {
        return accountsServices.addAccount(request)
    }

    /*
     * this is one of our POST endpoints, this one is responsible for closing an existing account by simply finding
     * the desired account to close via the given argument to us, which in this case is the unique account number. If the
     * account was not found, tell the client that the account was not found, if found, close the account by setting the
     * isActive Boolean variable connected to that account to false. This will keep the account's record in the database
     * but will not be able to be shown to anybody because our GET endpoint only retrieves accounts that are active (isActive = true).
     */
    @PostMapping("/accounts/v1/accounts/{accountNumber}/close")
    fun closeAccount(@PathVariable accountNumber: String): ResponseEntity<Any> {
        return accountsServices.closeAccount(accountNumber)
    }

    /*
     * this is one of our POST endpoints, this service is responsible for transferring money between two different accounts.
     * The service first checks if both accounts exist via the two account numbers given to us in the request. if both
     * accounts exist and different from each other, then we continue to the next check. If the source account has less
     * money than the amount desired to transfer, we stop and tell the client there is insufficient balance, if there is
     * enough balance, we continue to the last check, which is if the amount is not zero. If all these checks are
     * satisfied. Then we make the transfer by subtracting the amount from the source account's balance and adding it into the
     * destination account's balance and updating the new values in the database. Once that is done, we save the transaction
     * in the transaction database to keep a record of all account's transactions. Finally, we return the new balance of the
     * source account.
     */
    @PostMapping("/accounts/v1/accounts/transfer")
    fun transferFunds(@RequestBody request: TransferRequestDTO): ResponseEntity<Any>{
        return accountsServices.transferFunds(request)
    }
}

// this is our DTOs for each endpoint
data class AccountResponseDTO(

    val userId: Long?,
    val balance: BigDecimal,
    val accountNumber: String,
    val name: String
)

data class ListAccounts(
    val accounts: List<AccountResponseDTO>
)

data class CreateAccountRequest(

    val userId: Long,
    val initialBalance: BigDecimal,
    val name: String
)

data class TransferRequestDTO(
    val sourceAccountNumber: String,
    val destinationAccountNumber: String,
    val amount: BigDecimal
)

data class TransferResponseDTO(
    val newBalance: BigDecimal
)