package it.unisalento.pas2425.userserviceproject.configuration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


public class RabbitTopicConfig {

    public static final String TOPIC_EXCHANGE_NAME = "topic-exchange";
    public static final String QUEUE_ERROR_LOGS = "queue-error-logs";
    public static final String QUEUE_ALL_LOGS = "queue-all-logs";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
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
    public Binding bindingErrorLogs(Queue queueErrorLogs, TopicExchange topicExchange) {
        return BindingBuilder.bind(queueErrorLogs).to(topicExchange).with("log.error");
    }

    @Bean
    public Binding bindingAllLogs(Queue queueAllLogs, TopicExchange topicExchange) {
        return BindingBuilder.bind(queueAllLogs).to(topicExchange).with("log.*");
    }
}
