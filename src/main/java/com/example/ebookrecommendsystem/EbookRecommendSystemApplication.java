package com.example.ebookrecommendsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EbookRecommendSystemApplication {

    public static void main(String[] args) {
        var context=SpringApplication.run(EbookRecommendSystemApplication.class, args);
        if(context.getEnvironment().getProperty("app.security.migrate-legacy-passwords",Boolean.class,false)) {
            context.close();
        }
    }

}
