package it.unisalento.pas2425.userserviceproject.components;


import it.unisalento.pas2425.userserviceproject.configuration.RabbitFanoutConfig;
import it.unisalento.pas2425.userserviceproject.configuration.RabbitTopicConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

//@Component
public class MessageSubscriber {


    @RabbitListener(queues = RabbitFanoutConfig.QUEUE_ERROR_LOGS)
    public void receiveErrorLogs(String message) {
        System.out.println("Messaggio ricevuto da queue-error-logs: " + message);
    }

    @RabbitListener(queues = RabbitTopicConfig.QUEUE_ALL_LOGS)
    public void receiveAllLogs(String message) {
        System.out.println("Messaggio ricevuto da queue-all-logs: " + message);
    }

}