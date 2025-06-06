package com.whatsappbot.whatsappservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.whatsappbot.whatsappservice")
public class WhatsappServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhatsappServiceApplication.class, args);
    }
}
