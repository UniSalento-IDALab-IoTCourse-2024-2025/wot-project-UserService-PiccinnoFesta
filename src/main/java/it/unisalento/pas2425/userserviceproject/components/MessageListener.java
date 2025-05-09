package it.unisalento.pas2425.userserviceproject.components;

import it.unisalento.pas2425.userserviceproject.configuration.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

//@Component
public class MessageListener {

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void receiveMessage(String message) {
        System.out.println("Messaggio ricevuto: " + message );
    }
}
