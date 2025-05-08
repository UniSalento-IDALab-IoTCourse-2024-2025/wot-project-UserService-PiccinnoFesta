package it.unisalento.pas2425.userserviceproject.configuration;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String QUEUE_NAME = "simple-queue";

    @Bean
    public Queue simpleQueue() {
        return new Queue(QUEUE_NAME, false);
    }
}