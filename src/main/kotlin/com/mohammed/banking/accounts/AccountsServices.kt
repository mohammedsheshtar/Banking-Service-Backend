package com.mohammed.banking.accounts

import com.mohammed.banking.transactions.TransactionRepository
import com.mohammed.banking.users.UserRepository
import com.mohammed.banking.accounts.AccountRepository
import org.springframework.stereotype.Service
import com.mohammed.banking.accounts.ListAccounts
import com.mohammed.banking.accounts.TransferRequestDTO
import com.mohammed.banking.accounts.TransferResponseDTO
import com.mohammed.banking.accounts.AccountResponseDTO
import com.mohammed.banking.accounts.CreateAccountRequest
import com.mohammed.banking.transactions.TransactionEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.math.BigDecimal
import java.security.SecureRandom


@Service
class AccountsServices(
    private val accountRepository: AccountRepository, // injecting account's database into this file (see AccountRepository.kt for more information).
    private val userRepository: UserRepository, // injecting user's database into this file (see UserRepository.kt for more information).
    private val transactionRepository: TransactionRepository // injecting transaction's database into this file (see TransactionRepository.kt for more information).
) {
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

    fun addAccount(request: CreateAccountRequest): ResponseEntity<Any> {
        val user = userRepository.findById(request.userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User with ID ${request.userId} was not found"))

        if (request.initialBalance < BigDecimal(10.000) || request.initialBalance > BigDecimal(1000000.000)) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Initial balance must be between 10 and 1,000,000 KD"))
        }

        val userAccounts = accountRepository.findAll().filter { it.user.id == user.id && it.isActive }
        if (userAccounts.size >= 5) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "user has reached the maximum limit of 5 active accounts"))
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
        return ResponseEntity.ok( AccountResponseDTO(
            userId = account.user.id!!,
            accountNumber = account.accountNumber,
            name = account.name,
            balance = account.balance
        ))
    }

    fun closeAccount(accountNumber: String): ResponseEntity<Any> {
        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "account number $accountNumber does not exist"))

        val updateAccount = account.copy(isActive = false)
        accountRepository.save(updateAccount)

        return ResponseEntity.noContent().build()
    }

    fun transferFunds(request: TransferRequestDTO): ResponseEntity<Any> {
        val sourceAccount = accountRepository.findByAccountNumber(request.sourceAccountNumber)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "source account number ${request.sourceAccountNumber} was not found"))

        val destinationAccount = accountRepository.findByAccountNumber(request.destinationAccountNumber)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "destination account number ${request.sourceAccountNumber} was not found"))

        if (!sourceAccount.isActive) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "source account is closed"))
        }

        if (!destinationAccount.isActive) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "destination account is closed"))
        }

        if (sourceAccount.balance < request.amount){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "insufficient balance, source account has less than required transfer amount"))
        }

        if (request.amount <= BigDecimal.ZERO) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "amount cannot be zero"))
        }

        if (request.sourceAccountNumber == request.destinationAccountNumber) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "you can't transfer to the same account..."))
        }

        val updatedSource = sourceAccount.copy(balance = sourceAccount.balance - request.amount)
        val updatedDestination = destinationAccount.copy(balance = destinationAccount.balance + request.amount)

        accountRepository.save(updatedSource)
        accountRepository.save(updatedDestination)

        transactionRepository.save(
            TransactionEntity(
            sourceAccount = updatedSource,
            destinationAccount = updatedDestination,
            amount = request.amount)
        )

        return ResponseEntity.ok(TransferResponseDTO(newBalance = updatedSource.balance))
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
}
