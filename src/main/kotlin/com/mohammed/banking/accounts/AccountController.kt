package com.mohammed.banking.accounts

import com.mohammed.banking.transactions.TransactionEntity
import com.mohammed.banking.transactions.TransactionRepository
import com.mohammed.banking.users.UserRepository
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.security.SecureRandom

@RestController
class AccountController(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository
) {
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

    @PostMapping("/accounts/v1/accounts/{accountNumber}/close")
    fun closeAccount(@PathVariable accountNumber: String) {
        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: throw IllegalArgumentException("Account with number $accountNumber not found")

        val updateAccount = account.copy(isActive = false)
        accountRepository.save(updateAccount)
    }

    @PostMapping("/accounts/v1/accounts/transfer")
    fun transferFunds(@RequestBody request: TransferRequestDTO): TransferResponseDTO{
        val sourceAccount = accountRepository.findByAccountNumber(request.sourceAccountNumber)
            ?: throw IllegalArgumentException("source account not found")

        val destinationAccount = accountRepository.findByAccountNumber(request.destinationAccountNumber)
            ?: throw IllegalArgumentException("destination account not found")

        if (sourceAccount.balance < request.amount){
            throw IllegalArgumentException("insufficient balance, need more money homie")
        }

        if (!sourceAccount.isActive || !destinationAccount.isActive) {
            throw IllegalArgumentException("one of the accounts is inactive homie check both accounts")
        }

        if (request.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("amount must be positive")
        }

        if (request.sourceAccountNumber == request.destinationAccountNumber) {
            throw IllegalArgumentException("you can't transfer to the same account")
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


