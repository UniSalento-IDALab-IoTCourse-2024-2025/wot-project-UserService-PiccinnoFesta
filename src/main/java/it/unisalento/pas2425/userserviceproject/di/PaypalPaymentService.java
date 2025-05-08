package it.unisalento.pas2425.userserviceproject.di;

import org.springframework.stereotype.Component;

@Component
public class PaypalPaymentService implements IPaymentService{
    @Override
    public void initialize() {
        System.out.println("Sono nel metodo initialize di Paypal");
    }
}
