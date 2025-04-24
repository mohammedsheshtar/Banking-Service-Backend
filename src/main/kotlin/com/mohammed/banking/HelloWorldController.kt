package com.mohammed.banking

import org.springframework.web.bind.annotation.*

@RestController
class HelloWorldController {

    @GetMapping("/hello")
    fun helloWorld() = "Hello World";
}