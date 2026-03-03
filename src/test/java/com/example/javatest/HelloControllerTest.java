package com.example.javatest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloControllerTest {

    @Test
    void testHello() {
        HelloController controller = new HelloController();
        String response = controller.hello();
        assertEquals("Hello DevOps World test!", response);
    }

}