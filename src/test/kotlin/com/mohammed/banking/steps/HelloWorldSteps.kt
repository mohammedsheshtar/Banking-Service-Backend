package com.mohammed.banking.steps

import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.ResponseEntity

class HelloWorldSteps {

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    private var response: ResponseEntity<String>? = null

    @When("I make a GET request to {string}")
    fun iMakeAGETRequestTo(endpoint: String) {
        response = testRestTemplate.getForEntity(endpoint, String::class.java)
    }

    @Then("the response status code should be {int}")
    fun theResponseStatusCodeShouldBe(expectedStatusCode: Int) {
        assertEquals(expectedStatusCode, response?.statusCode?.value())
    }

    @Then("the response body should be {string}")
    fun theResponseBodyShouldBe(expectedBody: String) {
        assertEquals(expectedBody, response?.body)
    }
}