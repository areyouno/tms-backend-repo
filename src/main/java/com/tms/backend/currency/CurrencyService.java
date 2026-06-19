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
            new Currency("KRW", "South Korean Won")
        );

        for (Currency currency : defaults) {
            if (!currencyRepository.existsByCode(currency.getCode())) {
                currencyRepository.save(currency);
            }
        }
    }
}
