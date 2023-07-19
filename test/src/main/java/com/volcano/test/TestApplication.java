package com.volcano.test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.volcano.classloader.config",
        "com.volcano.test",
        "com.volcano.apis",
        "com.volcano.interfaces"})
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
