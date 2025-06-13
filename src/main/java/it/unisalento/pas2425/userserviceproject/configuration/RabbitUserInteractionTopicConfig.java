package it.unisalento.pas2425.userserviceproject.configuration;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitUserInteractionTopicConfig {

    // Deve corrispondere all’exchange che il consumer sta usando
    public static final String INTERACTION_EXCHANGE = "user.interaction.exchange";

    // I due routing‐key che vuoi utilizzare
    public static final String ROUTING_ADMIN_UPDATE  = "adminUpdate";
    public static final String ROUTING_CREATE_WALLET = "createWallet";

    @Bean
    public TopicExchange interactionExchange() {
        return new TopicExchange(INTERACTION_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jacksonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory cf,
            MessageConverter jacksonConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(jacksonConverter);
        // Imposti l’exchange di default sul template
        template.setExchange(INTERACTION_EXCHANGE);
        return template;
    }
}