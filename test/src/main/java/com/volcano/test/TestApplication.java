package com.volcano.test;

import com.volcano.classloader.config.Encrypt;
import com.volcano.classloader.loader.EncryptClassLoader;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.volcano.test",
        "com.volcano.classloader.config",
})
public class TestApplication {

    @SneakyThrows
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
