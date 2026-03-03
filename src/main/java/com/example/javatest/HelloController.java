package com.example.javatest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        String podName = System.getenv("POD_NAME");
        LocalDateTime now = LocalDateTime.now();

        System.out.println("[" + now + "] Request handled by pod: " + podName);
        return "Hello DevOps World!";
    }
 
}