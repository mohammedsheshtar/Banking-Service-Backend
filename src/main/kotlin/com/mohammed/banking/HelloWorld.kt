package com.mohammed.banking

import org.springframework.web.bind.annotation.*

@RestController
class HelloWorld {

    @GetMapping("/hello")
    fun HelloWorld() = "yoyoo";
}