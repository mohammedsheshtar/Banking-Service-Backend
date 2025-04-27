package com.mohammed.banking

import com.mohammed.banking.KYCs.KYCRequestDTO
import com.mohammed.banking.KYCs.KYCResponseDTO
import com.mohammed.banking.accounts.*
import com.mohammed.banking.authentication.jwt.JwtService
import com.mohammed.banking.transactions.TransactionRepository
import com.mohammed.banking.users.CreateUserDTO
import com.mohammed.banking.users.UserEntity
import com.mohammed.banking.users.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.MultiValueMap
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance


@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = ["src/test/resources/application-test.properties"]
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BankingServiceApplicationTests {

	companion object {
		lateinit var savedUser: UserEntity
		@JvmStatic
		@BeforeAll
		fun setUp(
			@Autowired userRepository: UserRepository,
			@Autowired passwordEncoder: PasswordEncoder,
			@Autowired accountRepository: AccountRepository
		) {
			userRepository.deleteAll()
			val user = UserEntity(
				username = "momo1234",
				password = passwordEncoder.encode("123dB45")
			)
			savedUser = userRepository.save(user)
		}
	}


	@Autowired lateinit var restTemplate: TestRestTemplate
	@Autowired lateinit var accountRepository: AccountRepository
	@Autowired lateinit var transactionRepository: TransactionRepository

	@AfterEach
	fun cleanUp() {
		transactionRepository.deleteAll()
		accountRepository.deleteAll()
	}

	@Test
	fun `adding new account should work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders()
		headers.set("Authorization", "Bearer $token")

		val requestBody = savedUser.id?.let {
			CreateAccountRequest(
				userId = it,
				initialBalance = BigDecimal("777.777"),
				name = "Checking Account"
			)
		}

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			entity,
			AccountResponseDTO::class.java
		)

		assertEquals(HttpStatus.OK, response.statusCode)

		val body = response.body!!
		assertEquals("Checking Account", body.name)
		assertEquals(BigDecimal("777.777"), body.balance)
		assert(body.accountNumber.isNotBlank()) { "Account number should not be blank!" }
	}
	@Test
	fun `multiple account creations should work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders().apply {
			set("Authorization", "Bearer $token")
		}

		val accountsToCreate = listOf(
			CreateAccountRequest(savedUser.id!!, BigDecimal("100.00"), "Checking Account"),
			CreateAccountRequest(savedUser.id!!, BigDecimal("250.50"), "Savings Account"),
			CreateAccountRequest(savedUser.id!!, BigDecimal("2250.50"), "Savings Account"),
			CreateAccountRequest(savedUser.id!!, BigDecimal("2503.50"), "Savings Account"),
			CreateAccountRequest(savedUser.id!!, BigDecimal("2650.50"), "Savings Account"),

		)

		accountsToCreate.forEach { request ->
			val response = restTemplate.exchange(
				"/accounts/v1/accounts",
				HttpMethod.POST,
				HttpEntity(request, headers),
				AccountResponseDTO::class.java
			)

			val body = response.body!!
			assertEquals(HttpStatus.OK, response.statusCode)
			assertEquals(request.name, body.name)
			assertEquals(request.initialBalance, body.balance)
			assert(body.accountNumber.isNotBlank())
		}
	}

	@Test
	fun `creating and closing an account should work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders(
			MultiValueMap.fromSingleValue(mapOf("Authorization" to "Bearer $token"))
		)

		// creating the account
		val requestBody = CreateAccountRequest(
			userId = savedUser.id!!,
			initialBalance = BigDecimal("7777.777"),
			name = "Checking Account"
		)
		val entity = HttpEntity(requestBody, headers)

		val creationResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			entity,
			AccountResponseDTO::class.java
		)

		assertEquals(HttpStatus.OK, creationResponse.statusCode)

		// closing the account
		val closedAccount = creationResponse.body!!.accountNumber
		assert(closedAccount.isNotBlank())

		val closeEntity = HttpEntity(null, headers)
		val closeResponse = restTemplate.exchange(
			"/accounts/v1/accounts/$closedAccount/close",
			HttpMethod.POST,
			closeEntity,
			String::class.java
		)

		assertEquals(HttpStatus.NO_CONTENT, closeResponse.statusCode)

	}
	@Test
	fun `fetching a list of accounts should work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders(
			MultiValueMap.fromSingleValue(mapOf("Authorization" to "Bearer $token"))
		)
		val request = HttpEntity<String>(headers)

		val result = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.GET,
			request,
			String::class.java
		)

		assertEquals(HttpStatus.OK, result.statusCode)
	}

	@Test
	fun `adding new KYC profile should work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders()
		headers.set("Authorization", "Bearer $token")

		val requestBody = savedUser.id?.let {
			KYCRequestDTO(
				userId = it,
				firstName = "Mohammed",
				lastName = "Sheshtar",
				dateOfBirth = LocalDate.parse("2001-04-04"),
				salary = BigDecimal("1200.124")
			)
		}

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/users/v1/kyc",
			HttpMethod.POST,
			entity,
			KYCResponseDTO::class.java
		)

		assertEquals(HttpStatus.OK, response.statusCode)

		val body = response.body!!
		assertEquals("Mohammed", body.firstName)
		assertEquals("Sheshtar", body.lastName)
		assertEquals(LocalDate.parse("2001-04-04"), body.dateOfBirth)
		assertEquals(BigDecimal("1200.124"), body.salary)

	}

	@Test
	fun `updating a KYC profile should work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders()
		headers.set("Authorization", "Bearer $token")

		val requestBody = savedUser.id?.let {
			KYCRequestDTO(
				userId = it,
				firstName = "Mohammed",
				lastName = "Sheshtar",
				dateOfBirth = LocalDate.parse("2001-04-04"),
				salary = BigDecimal("1200.124")
			)
		}

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/users/v1/kyc",
			HttpMethod.POST,
			entity,
			KYCResponseDTO::class.java
		)

		assertEquals(HttpStatus.OK, response.statusCode)

		val requestUpdateBody = savedUser.id?.let {
			KYCRequestDTO(
				userId = it,
				firstName = "Ahmed",
				lastName = "Sheshtar",
				dateOfBirth = LocalDate.parse("1999-03-03"),
				salary = BigDecimal("1660.124")
			)
		}

		val updateEntity = HttpEntity(requestUpdateBody, headers)

		val updateResponse = restTemplate.exchange(
			"/users/v1/kyc",
			HttpMethod.POST,
			updateEntity,
			KYCResponseDTO::class.java
		)

		val body = updateResponse.body!!
		assertEquals("Ahmed", body.firstName)
		assertEquals("Sheshtar", body.lastName)
		assertEquals(LocalDate.parse("1999-03-03"), body.dateOfBirth)
		assertEquals(BigDecimal("1660.124"), body.salary)

	}

	@Test
	fun `fetching KYC profile should work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders(
			MultiValueMap.fromSingleValue(mapOf("Authorization" to "Bearer $token"))
		)
		val request = HttpEntity<String>(headers)

		val result = restTemplate.exchange(
			"/users/v1/kyc/${savedUser.id}",
			HttpMethod.GET,
			request,
			String::class.java
		)

		assertEquals(HttpStatus.OK, result.statusCode)
	}
	@Test
	fun `transferring money between two accounts should work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders().apply {
			set("Authorization", "Bearer $token")
		}

		val sourceAccount = CreateAccountRequest(savedUser.id!!, BigDecimal("100.000"), "Savings account")
		val destinationAccount = CreateAccountRequest(savedUser.id!!, BigDecimal("990.000"), "Checking Account")

		val SourceEntity = HttpEntity(sourceAccount, headers)
		val DestinationEntity = HttpEntity(destinationAccount, headers)

		val sourceResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			SourceEntity,
			AccountResponseDTO::class.java
		)

		val destinationResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			DestinationEntity,
			AccountResponseDTO::class.java
		)

		assertEquals(HttpStatus.OK, sourceResponse.statusCode)
		assertEquals(HttpStatus.OK, destinationResponse.statusCode)

		val sourceBody = sourceResponse.body!!
		val destinationBody = destinationResponse.body!!


		assertEquals("Savings account", sourceBody.name)
		assertEquals(BigDecimal("100.000"), sourceBody.balance)
		assert(sourceBody.accountNumber.isNotBlank()) { "Account number should not be blank!" }

		assertEquals("Checking Account", destinationBody.name)
		assertEquals(BigDecimal("990.000"), destinationBody.balance)
		assert(sourceBody.accountNumber.isNotBlank()) { "Account number should not be blank!" }

		val requestBody = TransferRequestDTO(
			sourceAccountNumber = sourceBody.accountNumber,
			destinationAccountNumber = destinationBody.accountNumber,
			amount = BigDecimal("50.000")
		)

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/accounts/v1/accounts/transfer",
			HttpMethod.POST,
			entity,
			TransferResponseDTO::class.java
		)
		val body = response.body!!
		assertEquals(HttpStatus.OK, response.statusCode)
		assertEquals(BigDecimal("50.000"), body.newBalance)
	}

	@Test
	fun `Adding user with incorrect username paramter should NOT work`() {
		val request = CreateUserDTO(username = "mmmohaM655555555555554", password = "12Ln34567")
		val result = restTemplate.postForEntity("/users/v1/register", request, String::class.java)
		assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
	}

	@Test
	fun `adding new account with zero as initial balance should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders().apply {
			set("Authorization", "Bearer $token")
		}

		val requestBody = savedUser.id?.let {
			CreateAccountRequest(
				userId = it,
				initialBalance = BigDecimal("0.0"),
				name = "Checking Account"
			)
		}

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			entity,
			String::class.java
		)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertEquals(
			"""{"error":"Initial balance must be between 10 and 1,000,000 KD"}""",
			response.body
		)

	}

	@Test
	fun `adding new account with one billion as initial balance should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders().apply {
			set("Authorization", "Bearer $token")
		}

		val requestBody = savedUser.id?.let {
			CreateAccountRequest(
				userId = it,
				initialBalance = BigDecimal("1000000000.000"),
				name = "Checking Account"
			)
		}

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			entity,
			String::class.java
		)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertEquals(
			"""{"error":"Initial balance must be between 10 and 1,000,000 KD"}""",
			response.body
		)

	}

	@Test
	fun `closing an account with non-existent account number should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders(
			MultiValueMap.fromSingleValue(mapOf("Authorization" to "Bearer $token"))
		)

		val closeEntity = HttpEntity(null, headers)
		val closeResponse = restTemplate.exchange(
			"/accounts/v1/accounts/55777777777777/close",
			HttpMethod.POST,
			closeEntity,
			String::class.java
		)

		assertEquals(HttpStatus.BAD_REQUEST, closeResponse.statusCode)
		assertEquals(
			"""{"error":"account number 55777777777777 does not exist"}""",
			closeResponse.body
		)
	}

	@Test
	fun `adding new KYC profile with incorrect date of birth should should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders()
		headers.set("Authorization", "Bearer $token")

		val requestBody = savedUser.id?.let {
			KYCRequestDTO(
				userId = it,
				firstName = "Mohammed",
				lastName = "Sheshtar",
				dateOfBirth = LocalDate.parse("2021-04-04"),
				salary = BigDecimal("1200.124")
			)
		}

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/users/v1/kyc",
			HttpMethod.POST,
			entity,
			String::class.java
		)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertEquals(
			"""{"error":"you must be 18 or older to register"}""",
			response.body
		)
	}

	@Test
	fun `adding new KYC profile profile with zero as salary should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders()
		headers.set("Authorization", "Bearer $token")

		val requestBody = savedUser.id?.let {
			KYCRequestDTO(
				userId = it,
				firstName = "Mohammed",
				lastName = "Sheshtar",
				dateOfBirth = LocalDate.parse("2001-04-04"),
				salary = BigDecimal("0.0")
			)
		}

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/users/v1/kyc",
			HttpMethod.POST,
			entity,
			String::class.java
		)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertEquals(
			"""{"error":"salary must be between 100 and 1,000,000 KD"}""",
			response.body
		)

	}

	@Test
	fun `adding new KYC profile profile with one billion as salary should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders()
		headers.set("Authorization", "Bearer $token")

		val requestBody = savedUser.id?.let {
			KYCRequestDTO(
				userId = it,
				firstName = "Mohammed",
				lastName = "Sheshtar",
				dateOfBirth = LocalDate.parse("2001-04-04"),
				salary = BigDecimal("1000000000.0")
			)
		}

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/users/v1/kyc",
			HttpMethod.POST,
			entity,
			String::class.java
		)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertEquals(
			"""{"error":"salary must be between 100 and 1,000,000 KD"}""",
			response.body
		)

	}

	@Test
	fun `transferring money between two accounts with insufficient balance should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders().apply {
			set("Authorization", "Bearer $token")
		}

		val sourceAccount = CreateAccountRequest(savedUser.id!!, BigDecimal("100.000"), "Savings account")
		val destinationAccount = CreateAccountRequest(savedUser.id!!, BigDecimal("990.000"), "Checking Account")

		val SourceEntity = HttpEntity(sourceAccount, headers)
		val DestinationEntity = HttpEntity(destinationAccount, headers)

		val sourceResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			SourceEntity,
			AccountResponseDTO::class.java
		)

		val destinationResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			DestinationEntity,
			AccountResponseDTO::class.java
		)

		assertEquals(HttpStatus.OK, sourceResponse.statusCode)
		assertEquals(HttpStatus.OK, destinationResponse.statusCode)

		val sourceBody = sourceResponse.body!!
		val destinationBody = destinationResponse.body!!


		assertEquals("Savings account", sourceBody.name)
		assertEquals(BigDecimal("100.000"), sourceBody.balance)
		assert(sourceBody.accountNumber.isNotBlank()) { "Account number should not be blank!" }

		assertEquals("Checking Account", destinationBody.name)
		assertEquals(BigDecimal("990.000"), destinationBody.balance)
		assert(sourceBody.accountNumber.isNotBlank()) { "Account number should not be blank!" }

		val requestBody = TransferRequestDTO(
			sourceAccountNumber = sourceBody.accountNumber,
			destinationAccountNumber = destinationBody.accountNumber,
			amount = BigDecimal("1000.000")
		)

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/accounts/v1/accounts/transfer",
			HttpMethod.POST,
			entity,
			String::class.java
		)

		val body = response.body!!
		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertEquals(
			"""{"error":"insufficient balance, source account has less than required transfer amount"}""",
			response.body
		)

	}

	@Test
	fun `transferring money between two accounts with amount as zero should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders().apply {
			set("Authorization", "Bearer $token")
		}

		val sourceAccount = CreateAccountRequest(savedUser.id!!, BigDecimal("100.000"), "Savings account")
		val destinationAccount = CreateAccountRequest(savedUser.id!!, BigDecimal("990.000"), "Checking Account")

		val SourceEntity = HttpEntity(sourceAccount, headers)
		val DestinationEntity = HttpEntity(destinationAccount, headers)

		val sourceResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			SourceEntity,
			AccountResponseDTO::class.java
		)

		val destinationResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			DestinationEntity,
			AccountResponseDTO::class.java
		)

		assertEquals(HttpStatus.OK, sourceResponse.statusCode)
		assertEquals(HttpStatus.OK, destinationResponse.statusCode)

		val sourceBody = sourceResponse.body!!
		val destinationBody = destinationResponse.body!!


		assertEquals("Savings account", sourceBody.name)
		assertEquals(BigDecimal("100.000"), sourceBody.balance)
		assert(sourceBody.accountNumber.isNotBlank()) { "Account number should not be blank!" }

		assertEquals("Checking Account", destinationBody.name)
		assertEquals(BigDecimal("990.000"), destinationBody.balance)
		assert(sourceBody.accountNumber.isNotBlank()) { "Account number should not be blank!" }

		val requestBody = TransferRequestDTO(
			sourceAccountNumber = sourceBody.accountNumber,
			destinationAccountNumber = destinationBody.accountNumber,
			amount = BigDecimal("0.000")
		)

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/accounts/v1/accounts/transfer",
			HttpMethod.POST,
			entity,
			String::class.java
		)

		val body = response.body!!
		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertEquals(
			"""{"error":"amount cannot be zero"}""",
			response.body
		)
	}

	@Test
	fun `transferring money between two accounts with source account closed should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders().apply {
			set("Authorization", "Bearer $token")
		}

		val sourceAccount = CreateAccountRequest(savedUser.id!!, BigDecimal("100.000"), "Savings account")
		val destinationAccount = CreateAccountRequest(savedUser.id!!, BigDecimal("990.000"), "Checking Account")

		val SourceEntity = HttpEntity(sourceAccount, headers)
		val DestinationEntity = HttpEntity(destinationAccount, headers)

		val sourceResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			SourceEntity,
			AccountResponseDTO::class.java
		)

		val destinationResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			DestinationEntity,
			AccountResponseDTO::class.java
		)

		assertEquals(HttpStatus.OK, sourceResponse.statusCode)
		assertEquals(HttpStatus.OK, destinationResponse.statusCode)

		val sourceBody = sourceResponse.body!!
		val destinationBody = destinationResponse.body!!




		assertEquals("Savings account", sourceBody.name)
		assertEquals(BigDecimal("100.000"), sourceBody.balance)
		assert(sourceBody.accountNumber.isNotBlank()) { "Account number should not be blank!" }

		assertEquals("Checking Account", destinationBody.name)
		assertEquals(BigDecimal("990.000"), destinationBody.balance)
		assert(sourceBody.accountNumber.isNotBlank()) { "Account number should not be blank!" }

		val closedAccount = sourceBody.accountNumber!!
		assert(closedAccount.isNotBlank())

		val closeEntity = HttpEntity(null, headers)
		val closeResponse = restTemplate.exchange(
			"/accounts/v1/accounts/$closedAccount/close",
			HttpMethod.POST,
			closeEntity,
			String::class.java
		)

		assertEquals(HttpStatus.NO_CONTENT, closeResponse.statusCode)

		val requestBody = TransferRequestDTO(
			sourceAccountNumber = sourceBody.accountNumber,
			destinationAccountNumber = destinationBody.accountNumber,
			amount = BigDecimal("1000.000")
		)

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/accounts/v1/accounts/transfer",
			HttpMethod.POST,
			entity,
			String::class.java
		)

		val body = response.body!!
		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertEquals(
			"""{"error":"source account is closed"}""",
			response.body
		)
	}

	@Test
	fun `transferring money between two accounts with destination account closed should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders().apply {
			set("Authorization", "Bearer $token")
		}

		val sourceAccount = CreateAccountRequest(savedUser.id!!, BigDecimal("100.000"), "Savings account")
		val destinationAccount = CreateAccountRequest(savedUser.id!!, BigDecimal("990.000"), "Checking Account")

		val SourceEntity = HttpEntity(sourceAccount, headers)
		val DestinationEntity = HttpEntity(destinationAccount, headers)

		val sourceResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			SourceEntity,
			AccountResponseDTO::class.java
		)

		val destinationResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			DestinationEntity,
			AccountResponseDTO::class.java
		)

		assertEquals(HttpStatus.OK, sourceResponse.statusCode)
		assertEquals(HttpStatus.OK, destinationResponse.statusCode)

		val sourceBody = sourceResponse.body!!
		val destinationBody = destinationResponse.body!!




		assertEquals("Savings account", sourceBody.name)
		assertEquals(BigDecimal("100.000"), sourceBody.balance)
		assert(sourceBody.accountNumber.isNotBlank()) { "Account number should not be blank!" }

		assertEquals("Checking Account", destinationBody.name)
		assertEquals(BigDecimal("990.000"), destinationBody.balance)
		assert(sourceBody.accountNumber.isNotBlank()) { "Account number should not be blank!" }

		val closedAccount = destinationBody.accountNumber!!
		assert(closedAccount.isNotBlank())

		val closeEntity = HttpEntity(null, headers)
		val closeResponse = restTemplate.exchange(
			"/accounts/v1/accounts/$closedAccount/close",
			HttpMethod.POST,
			closeEntity,
			String::class.java
		)

		assertEquals(HttpStatus.NO_CONTENT, closeResponse.statusCode)

		val requestBody = TransferRequestDTO(
			sourceAccountNumber = sourceBody.accountNumber,
			destinationAccountNumber = destinationBody.accountNumber,
			amount = BigDecimal("1000.000")
		)

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/accounts/v1/accounts/transfer",
			HttpMethod.POST,
			entity,
			String::class.java
		)

		val body = response.body!!
		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertEquals(
			"""{"error":"destination account is closed"}""",
			response.body
		)
	}

	@Test
	fun `transferring money between the same account should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders().apply {
			set("Authorization", "Bearer $token")
		}

		val sourceAccount = CreateAccountRequest(savedUser.id!!, BigDecimal("100.000"), "Savings account")

		val SourceEntity = HttpEntity(sourceAccount, headers)

		val sourceResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			SourceEntity,
			AccountResponseDTO::class.java
		)

		assertEquals(HttpStatus.OK, sourceResponse.statusCode)

		val sourceBody = sourceResponse.body!!

		assertEquals("Savings account", sourceBody.name)
		assertEquals(BigDecimal("100.000"), sourceBody.balance)
		assert(sourceBody.accountNumber.isNotBlank()) { "Account number should not be blank!" }

		val requestBody = TransferRequestDTO(
			sourceAccountNumber = sourceBody.accountNumber,
			destinationAccountNumber = sourceBody.accountNumber,
			amount = BigDecimal("50.000")
		)

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/accounts/v1/accounts/transfer",
			HttpMethod.POST,
			entity,
			String::class.java
		)
		val body = response.body!!
		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertEquals(
			"""{"error":"you can't transfer to the same account..."}""",
			response.body
		)
	}

	@Test
	fun `multiple account creations should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders().apply {
			set("Authorization", "Bearer $token")
		}

		val requests = List(6) {
			CreateAccountRequest(savedUser.id!!, BigDecimal("250.50"), "Checking Account")
		}

		// first 5 should pass
		requests.take(5).forEach {
			val res = restTemplate.exchange(
				"/accounts/v1/accounts",
				HttpMethod.POST,
				HttpEntity(it, headers),
				String::class.java
			)
			assertEquals(HttpStatus.OK, res.statusCode)
		}

		// last one should fail
		val lastResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			HttpEntity(requests.last(), headers),
			String::class.java
		)
		assertEquals(HttpStatus.BAD_REQUEST, lastResponse.statusCode)
	}

	@Test
	fun `fetching a list of accounts should with incorrect endpoint should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders(
			MultiValueMap.fromSingleValue(mapOf("Authorization" to "Bearer $token"))
		)
		val request = HttpEntity<String>(headers)

		val result = restTemplate.exchange(
			"/accounts",
			HttpMethod.GET,
			request,
			String::class.java
		)

		assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
	}

	@Test
	fun `adding user with incorrect endpoint should not work`() {
		val request = CreateUserDTO(username = "mmmohaM64", password = "12Ln34567")
		val result = restTemplate.postForEntity("/users", request, String::class.java)
		assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
	}

	@Test
	fun `adding new account with incorrect endpoint should work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders()
		headers.set("Authorization", "Bearer $token")

		val requestBody = savedUser.id?.let {
			CreateAccountRequest(
				userId = it,
				initialBalance = BigDecimal("777.777"),
				name = "Checking Account"
			)
		}

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/account",
			HttpMethod.POST,
			entity,
			AccountResponseDTO::class.java
		)

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
	}

	@Test
	fun `creating and closing an account with incorrect endpoint should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders(
			MultiValueMap.fromSingleValue(mapOf("Authorization" to "Bearer $token"))
		)

		// creating the account
		val requestBody = CreateAccountRequest(
			userId = savedUser.id!!,
			initialBalance = BigDecimal("7777.777"),
			name = "Checking Account"
		)
		val entity = HttpEntity(requestBody, headers)

		val creationResponse = restTemplate.exchange(
			"/accounts/v1/accounts",
			HttpMethod.POST,
			entity,
			AccountResponseDTO::class.java
		)

		assertEquals(HttpStatus.OK, creationResponse.statusCode)

		// closing the account
		val closedAccount = creationResponse.body!!.accountNumber
		assert(closedAccount.isNotBlank())

		val closeEntity = HttpEntity(null, headers)
		val closeResponse = restTemplate.exchange(
			"/accounts/$closedAccount",
			HttpMethod.POST,
			closeEntity,
			String::class.java
		)

		assertEquals(HttpStatus.FORBIDDEN, closeResponse.statusCode)

	}
	@Test
	fun `adding new KYC profile with incorrect endpoint should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders()
		headers.set("Authorization", "Bearer $token")

		val requestBody = savedUser.id?.let {
			KYCRequestDTO(
				userId = it,
				firstName = "Mohammed",
				lastName = "Sheshtar",
				dateOfBirth = LocalDate.parse("2001-04-04"),
				salary = BigDecimal("1200.124")
			)
		}

		val entity = HttpEntity(requestBody, headers)

		val response = restTemplate.exchange(
			"/user",
			HttpMethod.POST,
			entity,
			KYCResponseDTO::class.java
		)

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
	}

	@Test
	fun `fetching KYC profile with incorrect endpoint should NOT work`(@Autowired jwtService: JwtService) {
		val token = jwtService.generateToken("momo1234")
		val headers = HttpHeaders(
			MultiValueMap.fromSingleValue(mapOf("Authorization" to "Bearer $token"))
		)
		val request = HttpEntity<String>(headers)

		val result = restTemplate.exchange(
			"/user",
			HttpMethod.GET,
			request,
			String::class.java
		)

		assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
	}
}