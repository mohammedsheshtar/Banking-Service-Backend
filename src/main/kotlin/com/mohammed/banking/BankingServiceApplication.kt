/*
 * Author: Muhammed Sheshtar
 * Desription: This project is a complete backend API service for a bank with postgresSQL as its database,
 * and Kotlin as the programming language to create the service logic, RESTful controllers, and repositories to manage
 * each required entity such as the KYCs, users, transactions, and accounts. Regarding my design choice
 * for the structure of the source code in this project, it may not coincide with the typical file/folder ordering convention
 * of Spring Boot projects, because of adding the business logic that the RESTful controllers are going to execute upon request
 * directly inside the controller files. I understand in the future, when dealing with projects on a greater scale,
 * it would be wise to separate the logic from the controller for greater scalability and maintainability, however,
 * for this project, it felt more intuitive to me due to the project being simple enough to do so without the controller files being too cluttered.
 * Moreover, I did not create a controller file for the transactions entity because we were required to create one service related to it, and it was
 * tied to accounts. Therefore, I added the transfer of funds service in the account's controller instead of making a whole
 * controller file just for it in transactions.
 */

package com.mohammed.banking

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BankingServiceApplication

fun main(args: Array<String>) {
	runApplication<BankingServiceApplication>(*args)
}
