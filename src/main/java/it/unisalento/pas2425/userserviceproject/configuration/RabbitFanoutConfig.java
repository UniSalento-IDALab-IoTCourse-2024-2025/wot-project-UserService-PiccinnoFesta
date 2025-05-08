package it.unisalento.pas2425.userserviceproject.configuration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitFanoutConfig {

    public static final String FANOUT_EXCHANGE_NAME = "fanout-exchange";
    public static final String QUEUE_ERROR_LOGS = "queue-error-logs";
    public static final String QUEUE_ALL_LOGS = "queue-all-logs";

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE_NAME);
    }

    @Bean
    public Queue queueErrorLogs() {
        return new Queue(QUEUE_ERROR_LOGS, false);
    }

    @Bean
    public Queue queueAllLogs() {
        return new Queue(QUEUE_ALL_LOGS, false);
    }

    @Bean
    public Binding bindingErrorLogs(Queue queueErrorLogs, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(queueErrorLogs).to(fanoutExchange);
    }

    @Bean
    public Binding bindingAllLogs(Queue queueAllLogs, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(queueAllLogs).to(fanoutExchange);
    }
}
