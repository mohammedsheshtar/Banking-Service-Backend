package com.mohammed.banking.accounts

import com.mohammed.banking.transactions.TransactionEntity
import com.mohammed.banking.transactions.TransactionRepository
import com.mohammed.banking.users.UserRepository
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.security.SecureRandom

/*
 * @RestController tells Spring that this file will be a controller that listens to HTTP requests of clients
 * and provides the required services via the functions contained inside it and will be marked with the correct
 * annotations to represent whether the service will GET (read) data or POST (create) data using
 * @PostMapping and @GetMapping.
 */
@RestController
class AccountController(
    private val accountRepository: AccountRepository, // injecting account's database into this file (see AccountRepository.kt for more information).
    private val userRepository: UserRepository, // injecting user's database into this file (see UserRepository.kt for more information).
    private val transactionRepository: TransactionRepository // injecting transaction's database into this file (see TransactionRepository.kt for more information).
) {

    /*
     * this is our GET endpoint for our accounts. This service is responsible for retrieving all the existing ACTIVE accounts
     * in our database and returns them to the client.
     */
    @GetMapping("/accounts/v1/accounts")
    fun listAccounts(): ListAccounts {
        val accounts = accountRepository.findAll().filter { it.isActive }.map {
            AccountResponseDTO(
                userId = it.user.id,
                balance = it.balance,
                accountNumber = it.accountNumber,
                name = it.name
            )
        }

        return ListAccounts(accounts)
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
    fun addAccount(@RequestBody request: CreateAccountRequest): AccountResponseDTO {
        val user = userRepository.findById(request.userId).orElseThrow {
            IllegalArgumentException("User with ID ${request.userId} not found")
        }

        val account = accountRepository.save(
            AccountEntity(
                user = user,
                balance = request.initialBalance,
                isActive = true,
                name = request.name,
                accountNumber = generateUniqueAccountNumber()
            )
        )
        return AccountResponseDTO(
            userId = account.user.id!!,
            accountNumber = account.accountNumber,
            name = account.name,
            balance = account.balance

        )
    }

    /*
     * this is one of our POST endpoints, this one is responsible for closing an existing account by simply finding
     * the desired account to close via the given argument to us, which in this case is the unique account number. If the
     * account was not found, tell the client that the account was not found, if found, close the account by setting the
     * isActive Boolean variable connected to that account to false. This will keep the account's record in the database
     * but will not be able to be shown to anybody because our GET endpoint only retrieves accounts that are active (isActive = true).
     */
    @PostMapping("/accounts/v1/accounts/{accountNumber}/close")
    fun closeAccount(@PathVariable accountNumber: String) {
        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: throw IllegalArgumentException("Account with number $accountNumber not found")

        val updateAccount = account.copy(isActive = false)
        accountRepository.save(updateAccount)
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
    fun transferFunds(@RequestBody request: TransferRequestDTO): TransferResponseDTO{
        val sourceAccount = accountRepository.findByAccountNumber(request.sourceAccountNumber)
            ?: throw IllegalArgumentException("source account not found")

        val destinationAccount = accountRepository.findByAccountNumber(request.destinationAccountNumber)
            ?: throw IllegalArgumentException("destination account not found")

        if (sourceAccount.balance < request.amount){
            throw IllegalArgumentException("insufficient balance")
        }

        if (!sourceAccount.isActive || !destinationAccount.isActive) {
            throw IllegalArgumentException("one of the accounts is inactive")
        }

        if (request.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("amount must be positive")
        }

        if (request.sourceAccountNumber == request.destinationAccountNumber) {
            throw IllegalArgumentException("you can't transfer to the same account...")
        }

        val updatedSource = sourceAccount.copy(balance = sourceAccount.balance - request.amount)
        val updatedDestination = destinationAccount.copy(balance = destinationAccount.balance + request.amount)

        accountRepository.save(updatedSource)
        accountRepository.save(updatedDestination)

        transactionRepository.save(TransactionEntity(
            sourceAccount = updatedSource,
            destinationAccount = updatedDestination,
            amount = request.amount)
        )

        return TransferResponseDTO(newBalance = updatedSource.balance)

    }

    /*
     * the two below functions work together to create the account number for each account. I tried
     * to keep the randomness strong enough to be hard to predict. in this case, I used the SecureRandom library
     * that has better seeding than the normal Random library due to it using non-deterministic variables in its
     * equations to generate a random sequence of numbers. I added a prefix of two number sevens as a design choice
     * because my favorite number is seven :)
     *
     * Once the number generator was created, I wanted to design the account number such that it would always be unique.
     * So, I decided to use a do while loop that uses the number generator and make a new account number and then checks
     * whether that account number exists in the database. If it exists in the database, then generate another, it will
     * keep doing so in the while loop until it gets a unique number that would break the loop and return the newly
     * generated unique number.
     */
    fun generateSecureAccountNumber(): String {
        val secureRandom = SecureRandom()
        val prefix = "77"
        val randomDigits = (1..12)
            .map { secureRandom.nextInt(10) }
            .joinToString("")
        return "$prefix$randomDigits"
    }
    fun generateUniqueAccountNumber(): String {
        var accountNumber: String
        do {
            accountNumber = generateSecureAccountNumber()
        } while (accountRepository.existsByAccountNumber(accountNumber))
        return accountNumber
    }


// this is our DTOs for each endpoint
}

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


