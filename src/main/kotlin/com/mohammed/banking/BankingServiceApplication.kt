/*
Author: Muhammed Sheshtar
Desription: This project is a complete backend API service for a bank with postgresSQL as its database,
and Kotlin as the programming language to create the service logic, RESTful controllers, and repositories to manage
each required entity such as the KYCs, users, transactions, and accounts.
* */

package com.mohammed.banking

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BankingServiceApplication

fun main(args: Array<String>) {
	runApplication<BankingServiceApplication>(*args)
}
