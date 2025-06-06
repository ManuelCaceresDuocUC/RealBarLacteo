package com.whatsappbot.whatsappservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.whatsappbot.whatsappservice")
public class WhatsappServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhatsappServiceApplication.class, args);
    }
}
