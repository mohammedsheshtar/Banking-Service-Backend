package com.mohammed.banking
import io.cucumber.spring.CucumberContextConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@CucumberContextConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["src/test/resources/application-test.properties"])

@ActiveProfiles("test")
class BankingServiceCucumberApplicationTests {

}