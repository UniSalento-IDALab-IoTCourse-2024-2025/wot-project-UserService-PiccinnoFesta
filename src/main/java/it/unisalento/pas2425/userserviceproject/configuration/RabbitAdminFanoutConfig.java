package it.unisalento.pas2425.userserviceproject.configuration;


import org.springframework.amqp.core.FanoutExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitAdminFanoutConfig {

    public static final String ADMIN_UPDATE_EXCHANGE_NAME = "admin.update.exchange";

    @Bean
    public FanoutExchange adminUpdateExchange() {
        return new FanoutExchange(ADMIN_UPDATE_EXCHANGE_NAME,true,false);
    }

}