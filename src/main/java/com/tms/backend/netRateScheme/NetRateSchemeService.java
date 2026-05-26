package com.tms.backend.netRateScheme;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tms.backend.dto.MatchTypeRateDTO;
import com.tms.backend.dto.MatchTypeRateResponseDTO;
import com.tms.backend.dto.NetRateSchemeCreateDTO;
import com.tms.backend.dto.NetRateSchemeResponseDTO;
import com.tms.backend.dto.NetRateSchemeUpdateDTO;
import com.tms.backend.client.Client;
import com.tms.backend.client.ClientRepository;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class NetRateSchemeService {
    private final NetRateSchemeRepository netRateSchemeRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public NetRateSchemeService(
        NetRateSchemeRepository netRateSchemeRepository,
        UserRepository userRepository,
        ClientRepository clientRepository
    ) {
        this.netRateSchemeRepository = netRateSchemeRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public NetRateScheme createScheme(NetRateSchemeCreateDTO dto, Long userId) {
        NetRateScheme scheme = new NetRateScheme();
        scheme.setName(dto.name());

        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        scheme.setCreatedBy(creator);

        if (dto.isDefault()) {
            netRateSchemeRepository.clearCurrentDefault();
            scheme.setDefault(true);
        } else if (!netRateSchemeRepository.existsByIsDefaultTrue()) {
            scheme.setDefault(true);
        }

        if (dto.matchTypeRates() != null) {
            for (MatchTypeRateDTO rateDto : dto.matchTypeRates()) {
                MatchTypeRate rate = toMatchTypeRate(rateDto);
                rate.setNetRateScheme(scheme);
                scheme.getMatchTypeRates().add(rate);
            }
        }

        NetRateScheme savedScheme = netRateSchemeRepository.save(scheme);

        if (dto.clientId() != null) {
            Client client = clientRepository.findById(dto.clientId())
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            client.setNetRateScheme(savedScheme);
            clientRepository.save(client);
        }

        return savedScheme;
    }

    public List<NetRateSchemeResponseDTO> getAllSchemes() {
        return netRateSchemeRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public NetRateSchemeResponseDTO getSchemeById(Long id) {
        NetRateScheme scheme = netRateSchemeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("NetRateScheme not found"));
        return toDTO(scheme);
    }

    public NetRateSchemeResponseDTO toDTO(NetRateScheme scheme) {
        List<MatchTypeRateResponseDTO> rates = scheme.getMatchTypeRates().stream()
                .map(m -> new MatchTypeRateResponseDTO(
                        m.getMatchType(),
                        m.getTransMemoryPercent(),
                        m.getMachineTransPercent(),
                        m.getNonTranslatablePercent(),
                        m.getInternalFuzziesPercent()
                ))
                .toList();

        return new NetRateSchemeResponseDTO(
                scheme.getId(),
                scheme.getName(),
                scheme.isDefault(),
                rates
        );
    }

    public NetRateSchemeResponseDTO getDefaultScheme() {
        NetRateScheme scheme = netRateSchemeRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new RuntimeException("No default NetRateScheme set"));
        return toDTO(scheme);
    }

    public NetRateSchemeResponseDTO getSchemeByClientId(long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        NetRateScheme scheme = client.getNetRateScheme();
        if (scheme == null) {
            scheme = netRateSchemeRepository.findByIsDefaultTrue()
                    .orElseThrow(() -> new RuntimeException("No default NetRateScheme set"));
        }
        return toDTO(scheme);
    }

    @Transactional
    public NetRateScheme updateScheme(Long schemeId, NetRateSchemeUpdateDTO dto) {
        NetRateScheme scheme = netRateSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new RuntimeException("NetRateScheme not found"));

        if (dto.name() != null) {
            scheme.setName(dto.name());
        }

        scheme.getMatchTypeRates().clear();

        if (dto.matchTypeRates() != null) {
            for (MatchTypeRateDTO rateDto : dto.matchTypeRates()) {
                MatchTypeRate rate = toMatchTypeRate(rateDto);
                rate.setNetRateScheme(scheme);
                scheme.getMatchTypeRates().add(rate);
            }
        }

        return netRateSchemeRepository.save(scheme);
    }

    @Transactional
    public void setDefault(Long schemeId) {
        NetRateScheme scheme = netRateSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new RuntimeException("Scheme not found"));

        netRateSchemeRepository.clearCurrentDefault();
        scheme.setDefault(true);
        netRateSchemeRepository.save(scheme);
    }

    public void deleteSchemes(List<Long> ids) {
        List<NetRateScheme> schemes = netRateSchemeRepository.findAllById(ids);

        if (schemes.isEmpty()) {
            throw new RuntimeException("No schemes found for the given IDs");
        }

        netRateSchemeRepository.deleteAll(schemes);
    }

    @Transactional
    public NetRateScheme duplicateScheme(Long sourceSchemeId, Long userId) {
        NetRateScheme original = netRateSchemeRepository.findById(sourceSchemeId)
                .orElseThrow(() -> new RuntimeException("NetRateScheme not found"));

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        NetRateScheme copy = new NetRateScheme();
        copy.setName(original.getName() + " (Copy)");
        copy.setCreatedBy(creator);

        for (MatchTypeRate original_rate : original.getMatchTypeRates()) {
            MatchTypeRate rateCopy = new MatchTypeRate(
                original_rate.getMatchType(),
                original_rate.getTransMemoryPercent(),
                original_rate.getMachineTransPercent(),
                original_rate.getNonTranslatablePercent(),
                original_rate.getInternalFuzziesPercent()
            );
            rateCopy.setNetRateScheme(copy);
            copy.getMatchTypeRates().add(rateCopy);
        }

        return netRateSchemeRepository.save(copy);
    }

    @Transactional
    public void insertDefaultNetRateSchemeIfMissing() {
        if (netRateSchemeRepository.existsByIsDefaultTrue()) return;

        NetRateScheme scheme = new NetRateScheme();
        scheme.setName("Default");
        scheme.setDefault(true);

        for (MatchTypeRate rate : createDefaultMatchTypeRates()) {
            rate.setNetRateScheme(scheme);
            scheme.getMatchTypeRates().add(rate);
        }

        netRateSchemeRepository.save(scheme);
    }

    private List<MatchTypeRate> createDefaultMatchTypeRates() {
        return List.of(
            new MatchTypeRate(MatchType.REPETITIONS, 10L, 0L, 0L, 0L),
            new MatchTypeRate(MatchType.PERCENT_101, 10L, 0L, 0L, 0L),
            new MatchTypeRate(MatchType.PERCENT_100, 10L, 30L, 10L, 10L),
            new MatchTypeRate(MatchType.PERCENT_95, 33L, 40L, 33L, 33L),
            new MatchTypeRate(MatchType.PERCENT_85, 66L, 70L, 66L, 66L),
            new MatchTypeRate(MatchType.PERCENT_75, 100L, 100L, 100L, 100L),
            new MatchTypeRate(MatchType.PERCENT_50, 100L, 100L, 100L, 100L),
            new MatchTypeRate(MatchType.PERCENT_0, 100L, 100L, 100L, 100L)
        );
    }

    private MatchTypeRate toMatchTypeRate(MatchTypeRateDTO dto) {
        MatchTypeRate rate = new MatchTypeRate();
        rate.setMatchType(dto.matchType());
        rate.setTransMemoryPercent(dto.transMemoryPercent());
        rate.setMachineTransPercent(dto.machineTransPercent());
        rate.setNonTranslatablePercent(dto.nonTranslatablePercent());
        rate.setInternalFuzziesPercent(dto.internalFuzziesPercent());
        return rate;
    }
}
