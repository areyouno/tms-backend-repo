package com.tms.backend.currency;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    public CurrencyService(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    public List<Currency> getAllCurrencies() {
        return currencyRepository.findAll();
    }

    public void seedDefaultCurrencies() {
        List<Currency> defaults = List.of(
            new Currency("EUR", "Euro"),
            new Currency("USD", "US Dollar"),
            new Currency("GBP", "British Pound"),
            new Currency("JPY", "Japanese Yen"),
            new Currency("CHF", "Swiss Franc"),
            new Currency("CAD", "Canadian Dollar"),
            new Currency("AUD", "Australian Dollar"),
            new Currency("CNY", "Chinese Yuan"),
            new Currency("KRW", "South Korean Won"),
            new Currency("SEK", "Swedish Krona"),
            new Currency("NOK", "Norwegian Krone"),
            new Currency("DKK", "Danish Krone"),
            new Currency("PLN", "Polish Zloty"),
            new Currency("CZK", "Czech Koruna"),
            new Currency("BRL", "Brazilian Real"),
            new Currency("INR", "Indian Rupee"),
            new Currency("MXN", "Mexican Peso"),
            new Currency("SGD", "Singapore Dollar"),
            new Currency("HKD", "Hong Kong Dollar"),
            new Currency("TRY", "Turkish Lira")
        );

        for (Currency currency : defaults) {
            if (!currencyRepository.existsByCode(currency.getCode())) {
                currencyRepository.save(currency);
            }
        }
    }
}
