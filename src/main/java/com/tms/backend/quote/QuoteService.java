package com.tms.backend.quote;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.currency.Currency;
import com.tms.backend.currency.CurrencyRepository;
import com.tms.backend.dto.QuoteCreateDTO;
import com.tms.backend.dto.QuoteResponseDTO;
import com.tms.backend.jobAnalysis.JobAnalysis;
import com.tms.backend.jobAnalysis.JobAnalysisRepository;
import com.tms.backend.netRateScheme.NetRateSchemeRepository;
import com.tms.backend.priceList.PriceList;
import com.tms.backend.priceList.PriceListLanguagePair;
import com.tms.backend.priceList.PriceListRepository;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;
import com.tms.backend.workflowSteps.WorkflowStep;
import com.tms.backend.workflowSteps.WorkflowStepRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final PriceListRepository priceListRepository;
    private final CurrencyRepository currencyRepository;
    private final NetRateSchemeRepository netRateSchemeRepository;
    private final JobAnalysisRepository jobAnalysisRepository;
    private final WorkflowStepRepository workflowStepRepository;

    @Transactional(readOnly = true)
    public List<QuoteResponseDTO> getAllQuotes() {
        return quoteRepository.findAll().stream()
                .map(QuoteResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuoteResponseDTO getQuote(Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quote not found with id: " + id));
        return QuoteResponseDTO.fromEntity(quote);
    }

    @Transactional
    public QuoteResponseDTO createQuote(QuoteCreateDTO request) {
        Quote quote = new Quote();
        quote.setName(request.name());
        quote.setType(request.type() != null ? request.type() : QuoteType.PROVIDER);

        if (request.providerId() != null) {
            User provider = userRepository.findById(request.providerId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + request.providerId()));
            quote.setProvider(provider);
        }

        if (request.priceListId() != null) {
            PriceList priceList = priceListRepository.findById(request.priceListId())
                    .orElseThrow(() -> new RuntimeException("PriceList not found with id: " + request.priceListId()));
            quote.setPriceList(priceList);
        }

        if (request.currencyId() != null) {
            Currency currency = currencyRepository.findById(request.currencyId())
                    .orElseThrow(() -> new RuntimeException("Currency not found with id: " + request.currencyId()));
            quote.setCurrency(currency);
        }

        if (request.billingUnit() != null) {
            quote.setBillingUnit(request.billingUnit());
        }

        if (request.netRateSchemeId() != null) {
            netRateSchemeRepository.findById(request.netRateSchemeId())
                    .ifPresent(quote::setNetRateScheme);
        } else {
            netRateSchemeRepository.findByIsDefaultTrue().ifPresent(quote::setNetRateScheme);
        }

        if (request.jobAnalysisId() != null) {
            JobAnalysis jobAnalysis = jobAnalysisRepository.findById(request.jobAnalysisId())
                    .orElseThrow(() -> new RuntimeException("JobAnalysis not found with id: " + request.jobAnalysisId()));
            quote.setJobAnalysis(jobAnalysis);
            quote.setSourceLanguage(jobAnalysis.getSourceLang());
            if (jobAnalysis.getTargetLanguages() != null) {
                quote.setTargetLanguage(String.join(", ", jobAnalysis.getTargetLanguages()));
            }
        }

        if (request.workflowSteps() != null) {
            final JobAnalysis resolvedAnalysis = quote.getJobAnalysis();
            final PriceList resolvedPriceList = quote.getPriceList();

            if (resolvedAnalysis == null) {
                throw new RuntimeException("A job analysis is required");
            }
            if (resolvedPriceList == null) {
                throw new RuntimeException("A price list is required to compute step prices");
            }

            String sourceLang = resolvedAnalysis.getSourceLang();
            String targetLang = quote.getTargetLanguage();

            log.info("[Quote] Creating quote '{}' | Language pair: {} -> {} | Analysis: {} | Price list: {}",
                    quote.getName(), sourceLang, targetLang,
                    resolvedAnalysis.getId(), resolvedPriceList.getId());

            PriceListLanguagePair languagePair = resolvedPriceList.getLanguagePairs().stream()
                    .filter(lp -> sourceLang != null && sourceLang.equalsIgnoreCase(lp.getSourceLanguage())
                               && targetLang != null && targetLang.equalsIgnoreCase(lp.getTargetLanguage()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                        "No language pair found in price list for: " + sourceLang + " -> " + targetLang));

            log.info("[Quote] Matched language pair id={} | minPrice={}", languagePair.getId(), languagePair.getMinPrice());

            List<QuoteWorkflowStep> steps = request.workflowSteps().stream().map(entry -> {
                WorkflowStep workflowStep = workflowStepRepository.findById(entry.workflowStepId())
                        .orElseThrow(() -> new RuntimeException("WorkflowStep not found with id: " + entry.workflowStepId()));

                long netWords = entry.netWords() != null ? entry.netWords() : 0L;
                double unitPrice = entry.price() != null ? entry.price().doubleValue() : 0.0;
                double minPrice = languagePair.getMinPrice() != null ? languagePair.getMinPrice() : 0.0;
                double effectiveUnitPrice = Math.max(unitPrice, minPrice);
                BigDecimal finalPrice = BigDecimal.valueOf(netWords * effectiveUnitPrice);

                log.info("[Quote] Step '{}' (id={}) | netWords={} | unitPrice={} | effectiveUnitPrice={} | minPrice={} | finalPrice={}",
                        workflowStep.getName(), workflowStep.getId(),
                        netWords, unitPrice, effectiveUnitPrice, minPrice, finalPrice);

                QuoteWorkflowStep step = new QuoteWorkflowStep();
                step.setQuote(quote);
                step.setWorkflowStep(workflowStep);
                step.setNetWords(netWords);
                step.setPrice(finalPrice);
                return step;
            }).collect(Collectors.toList());
            quote.setWorkflowSteps(steps);
        }

        Quote saved = quoteRepository.save(quote);
        return QuoteResponseDTO.fromEntity(saved);
    }

    @Transactional
    public void deleteQuote(Long id) {
        if (!quoteRepository.existsById(id)) {
            throw new RuntimeException("Quote not found with id: " + id);
        }
        quoteRepository.deleteById(id);
    }
}
