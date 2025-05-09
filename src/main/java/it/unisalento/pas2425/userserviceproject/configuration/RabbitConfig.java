package it.unisalento.pas2425.userserviceproject.configuration;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


//rimuovo @Configuration da qui e dalle altre classi legate a rabbit perchè nel CI/CD non funziona, perchè c'è un rabbit
//che non trova
//@Configuration
public class RabbitConfig {
    public static final String QUEUE_NAME = "simple-queue";

    @Bean
    public Queue simpleQueue() {
        return new Queue(QUEUE_NAME, false);
    }
}