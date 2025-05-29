package it.unisalento.pas2425.userserviceproject.components;

//@Component
public class MessageListener {

    //@RabbitListener(queues = RabbitWalletConfig.QUEUE_NAME)
    public void receiveMessage(String message) {
        System.out.println("Messaggio ricevuto: " + message );
    }
}
