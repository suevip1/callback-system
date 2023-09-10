package com.dhx.callback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class CallbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(CallbackApplication.class, args);
    }

    /**
     * 注入 RestTemplate
     *
     * @return {@link RestTemplate}
     */
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
