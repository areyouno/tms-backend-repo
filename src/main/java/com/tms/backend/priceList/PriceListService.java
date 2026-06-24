package com.tms.backend.priceList;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tms.backend.currency.Currency;
import com.tms.backend.currency.CurrencyRepository;
import com.tms.backend.dto.PriceListCreateDTO;
import com.tms.backend.dto.PriceListLanguagePairDTO;
import com.tms.backend.dto.PriceListLanguagePairResponseDTO;
import com.tms.backend.dto.PriceListResponseDTO;
import com.tms.backend.dto.PriceListUpdateDTO;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class PriceListService {

    private final PriceListRepository priceListRepository;
    private final UserRepository userRepository;
    private final CurrencyRepository currencyRepository;

    public PriceListService(
        PriceListRepository priceListRepository,
        UserRepository userRepository,
        CurrencyRepository currencyRepository
    ) {
        this.priceListRepository = priceListRepository;
        this.userRepository = userRepository;
        this.currencyRepository = currencyRepository;
    }

    @Transactional
    public PriceListResponseDTO createPriceList(PriceListCreateDTO dto, Long userId) {
        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Currency currency = currencyRepository.findById(dto.currencyId())
            .orElseThrow(() -> new RuntimeException("Currency not found"));

        PriceList priceList = new PriceList();
        priceList.setName(dto.name());
        priceList.setCurrency(currency);
        priceList.setBillingUnit(dto.billingUnit());
        priceList.setCreatedBy(creator);

        if (dto.languagePairs() != null) {
            for (PriceListLanguagePairDTO lpDto : dto.languagePairs()) {
                PriceListLanguagePair lp = buildLanguagePair(lpDto, priceList);
                priceList.getLanguagePairs().add(lp);
            }
        }

        PriceList saved = priceListRepository.save(priceList);
        return toDTO(saved);
    }

    @Transactional
    public List<PriceListResponseDTO> getAllPriceLists() {
        return priceListRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public PriceListResponseDTO getPriceListById(Long id) {
        PriceList priceList = priceListRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("PriceList not found"));
        return toDTO(priceList);
    }

    @Transactional
    public PriceListLanguagePairResponseDTO getLanguagePair(Long priceListId, String source, String target) {

        PriceList priceList = priceListRepository.findById(priceListId)
            .orElseThrow(() -> new RuntimeException("PriceList not found"));

        PriceListLanguagePair lp = priceList.getLanguagePairs().stream()
            .filter(pair -> source.equalsIgnoreCase(pair.getSourceLanguage())
                         && target.equalsIgnoreCase(pair.getTargetLanguage()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "No language pair found for: " + source + " → " + target));

        return new PriceListLanguagePairResponseDTO(
            lp.getId(),
            lp.getSourceLanguage(),
            lp.getTargetLanguage(),
            lp.getPrice()
        );
    }

    @Transactional
    public PriceListResponseDTO updatePriceList(Long id, PriceListUpdateDTO dto) {
        PriceList priceList = priceListRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("PriceList not found"));

        if (dto.name() != null) {
            priceList.setName(dto.name());
        }
        if (dto.currencyId() != null) {
            Currency currency = currencyRepository.findById(dto.currencyId())
                .orElseThrow(() -> new RuntimeException("Currency not found"));
            priceList.setCurrency(currency);
        }
        if (dto.billingUnit() != null) {
            priceList.setBillingUnit(dto.billingUnit());
        }

        PriceList saved = priceListRepository.save(priceList);
        return toDTO(saved);
    }

    @Transactional
    public void deletePriceLists(List<Long> ids) {
        List<PriceList> priceLists = priceListRepository.findAllById(ids);

        if (priceLists.isEmpty()) {
            throw new RuntimeException("No price lists found for the given IDs");
        }

        priceListRepository.deleteAll(priceLists);
    }

    @Transactional
    public PriceListResponseDTO addLanguagePair(Long priceListId, PriceListLanguagePairDTO dto) {
        PriceList priceList = priceListRepository.findById(priceListId)
            .orElseThrow(() -> new RuntimeException("PriceList not found"));

        PriceListLanguagePair lp = buildLanguagePair(dto, priceList);
        priceList.getLanguagePairs().add(lp);

        PriceList saved = priceListRepository.save(priceList);
        return toDTO(saved);
    }

    @Transactional
    public PriceListResponseDTO updateLanguagePair(Long priceListId, Long languagePairId, PriceListLanguagePairDTO dto) {
        PriceList priceList = priceListRepository.findById(priceListId)
            .orElseThrow(() -> new RuntimeException("PriceList not found"));

        PriceListLanguagePair lp = priceList.getLanguagePairs().stream()
            .filter(pair -> pair.getId().equals(languagePairId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Language pair not found"));

        lp.setSourceLanguage(dto.sourceLanguage());
        lp.setTargetLanguage(dto.targetLanguage());
        lp.setPrice(dto.price() != null ? dto.price() : 0.0);

        PriceList saved = priceListRepository.save(priceList);
        return toDTO(saved);
    }

    @Transactional
    public PriceListResponseDTO deleteLanguagePair(Long priceListId, Long languagePairId) {
        PriceList priceList = priceListRepository.findById(priceListId)
            .orElseThrow(() -> new RuntimeException("PriceList not found"));

        boolean removed = priceList.getLanguagePairs().removeIf(lp -> lp.getId().equals(languagePairId));

        if (!removed) {
            throw new RuntimeException("Language pair not found");
        }

        PriceList saved = priceListRepository.save(priceList);
        return toDTO(saved);
    }

    private PriceListLanguagePair buildLanguagePair(PriceListLanguagePairDTO dto, PriceList priceList) {
        PriceListLanguagePair lp = new PriceListLanguagePair();
        lp.setPriceList(priceList);
        lp.setSourceLanguage(dto.sourceLanguage());
        lp.setTargetLanguage(dto.targetLanguage());
        lp.setPrice(dto.price() != null ? dto.price() : 0.0);
        return lp;
    }

    private PriceListResponseDTO toDTO(PriceList priceList) {
        List<PriceListLanguagePairResponseDTO> lpDtos = priceList.getLanguagePairs().stream()
            .map(lp -> new PriceListLanguagePairResponseDTO(
                lp.getId(),
                lp.getSourceLanguage(),
                lp.getTargetLanguage(),
                lp.getPrice()
            ))
            .toList();

        return new PriceListResponseDTO(
            priceList.getId(),
            priceList.getName(),
            priceList.getCurrency() != null ? priceList.getCurrency().getId() : null,
            priceList.getCurrency() != null ? priceList.getCurrency().getCode() : null,
            priceList.getCurrency() != null ? priceList.getCurrency().getName() : null,
            priceList.getBillingUnit(),
            priceList.getCreateDate(),
            priceList.getCreatedBy() != null ? priceList.getCreatedBy().getId() : null,
            priceList.getCreatedBy() != null ? priceList.getCreatedBy().getFirstName() + " " + priceList.getCreatedBy().getLastName() : null,
            lpDtos
        );
    }
}
