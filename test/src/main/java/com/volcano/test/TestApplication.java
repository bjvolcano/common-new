package com.volcano.test;
import com.volcano.classloader.config.Encrypt;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.volcano.classloader.config",
        "com.volcano.apis",
        "com.volcano.interfaces"})
public class TestApplication {
    public static void main(String[] args) {
        Encrypt.load();
        SpringApplication.run(TestApplication.class, args);
    }
}
